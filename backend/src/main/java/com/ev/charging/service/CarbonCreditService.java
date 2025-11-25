package com.ev.charging.service;

import com.ev.charging.dto.CreditRecordVO;
import com.ev.charging.dto.CreditStatisticsVO;
import com.ev.charging.entity.CarbonCreditRecord;
import com.ev.charging.entity.User;
import com.ev.charging.repository.CarbonCreditRecordRepository;
import com.ev.charging.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 碳积分服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CarbonCreditService {

    private final CarbonCreditRecordRepository creditRecordRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * 碳排放因子（kg CO2/kWh）
     */
    private static final double CARBON_EMISSION_FACTOR = 0.785;

    /**
     * 积分转换系数
     */
    private static final int CREDIT_MULTIPLIER = 10;

    /**
     * 每日签到积分
     */
    private static final int DAILY_CHECK_IN_CREDITS = 5;

    /**
     * 连续签到7天奖励积分
     */
    private static final int CONTINUOUS_7_DAYS_BONUS = 20;

    /**
     * 充电获得积分
     * 公式：积分 = 充电量(kWh) × 碳排放因子(0.785) × 10
     *
     * 并发防护：使用数据库级别的唯一约束 + 异常捕获
     * - 第一层：existsByOrderId 快速检查（只能防大部分重复，并发不可靠）
     * - 第二层：DataIntegrityViolationException 捕获（防止并发重复）
     *
     * @param orderId      订单ID
     * @param userId       用户ID
     * @param chargeAmount 充电量(kWh)
     * @return 获得的积分，重复发放返回0
     */
    @Transactional(rollbackFor = Exception.class)
    public Integer addCreditsForCharging(Long orderId, Long userId, BigDecimal chargeAmount) {
        log.info("为订单 {} 添加充电积分，用户ID: {}, 充电量: {} kWh", orderId, userId, chargeAmount);

        // 幂等防重：同一订单不重复发放积分（快速检查）
        if (creditRecordRepository.existsByOrderId(orderId)) {
            log.warn("订单 {} 已发放积分，跳过重复发放", orderId);
            return 0;
        }

        // 计算积分
        int credits = (int) (chargeAmount.doubleValue() * CARBON_EMISSION_FACTOR * CREDIT_MULTIPLIER);

        try {
            // 原子性增加用户积分（无竞态条件）
            userRepository.addCarbonCreditsAtomically(userId, credits);

            // 重新查询用户获取最新余额（原子更新后JPA一级缓存陈旧）
            userRepository.flush();
            int actualBalance = userRepository.findById(userId).map(User::getCarbonCredits).orElse(0);

            // 创建积分记录
            CarbonCreditRecord record = CarbonCreditRecord.builder()
                    .userId(userId)
                    .orderId(orderId)
                    .changeAmount(credits)
                    .changeType((byte) 1) // 1-充电获得
                    .description(String.format("充电%.2f kWh，获得碳积分", chargeAmount))
                    .balanceAfter(actualBalance)
                    .build();
            creditRecordRepository.save(record);

            log.info("充电积分添加成功，获得积分: {}, 当前余额: {}", credits, actualBalance);
            return credits;
        } catch (DataIntegrityViolationException e) {
            // 捕获数据库唯一约束冲突（并发场景下的重复发放）
            log.warn("订单 {} 积分已由其他线程发放，拒绝重复发放: {}", orderId, e.getMessage());
            return 0;
        }
    }

    /**
     * 每日签到获得积分
     *
     * 并发防护：使用 Redis 分布式锁防止 TOCTOU（Time-Of-Check-Time-Of-Use）竞态条件
     * - 在签到前获取分布式锁，确保同一用户同一天只能有一个线程执行完整的签到流程
     * - 若获取锁失败，拒绝请求并告知用户签到处理中
     * - 锁的 TTL 为 10 秒，自动失效
     *
     * @param userId 用户ID
     * @return 获得的积分
     */
    @Transactional(rollbackFor = Exception.class)
    public Integer dailyCheckIn(Long userId) {
        log.info("用户 {} 尝试签到", userId);

        // 分布式锁：使用 Redis 防止并发签到
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        String lockKey = "checkin:lock:" + userId + ":" + today;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(10));

        if (Boolean.FALSE.equals(acquired)) {
            log.warn("用户 {} 签到处理中，拒绝重复请求", userId);
            throw new RuntimeException("签到处理中，请勿重复操作");
        }

        try {
            // 检查今天是否已签到（使用固定时区，避免JVM时区不一致）
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

            boolean hasCheckedIn = creditRecordRepository.existsByUserIdAndChangeTypeAndCreateTimeBetween(
                    userId, (byte) 2, startOfDay, endOfDay);

            if (hasCheckedIn) {
                log.warn("用户 {} 今天已签到", userId);
                throw new RuntimeException("今天已签到，请明天再来");
            }

            // 基础签到积分
            int credits = DAILY_CHECK_IN_CREDITS;

            // 计算连续签到天数
            int continuousDays = calculateContinuousCheckInDays(userId);

            // 如果连续签到7天，额外奖励20积分
            if ((continuousDays + 1) % 7 == 0) {
                credits += CONTINUOUS_7_DAYS_BONUS;
                log.info("用户 {} 连续签到7天，额外奖励 {} 积分", userId, CONTINUOUS_7_DAYS_BONUS);
            }

            // 更新用户积分余额（原子性操作，防止并发竞态条件）
            userRepository.addCarbonCreditsAtomically(userId, credits);

            // 重新查询用户获取最新余额（原子更新后JPA一级缓存陈旧）
            userRepository.flush();
            int actualBalance = userRepository.findById(userId).map(User::getCarbonCredits).orElse(0);

            // 创建积分记录
            String description = (credits > DAILY_CHECK_IN_CREDITS)
                    ? String.format("每日签到（连续%d天，额外奖励%d积分）", continuousDays + 1, CONTINUOUS_7_DAYS_BONUS)
                    : "每日签到";

            CarbonCreditRecord record = CarbonCreditRecord.builder()
                    .userId(userId)
                    .changeAmount(credits)
                    .changeType((byte) 2) // 2-签到
                    .description(description)
                    .balanceAfter(actualBalance)
                    .build();
            creditRecordRepository.save(record);

            log.info("签到成功，获得积分: {}, 当前余额: {}", credits, actualBalance);
            return credits;
        } finally {
            // 确保释放分布式锁
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 兑换消耗积分
     *
     * @param userId      用户ID
     * @param amount      消耗积分数量
     * @param description 兑换描述
     */
    @Transactional(rollbackFor = Exception.class)
    public void redeemCredits(Long userId, Integer amount, String description) {
        log.info("用户 {} 兑换积分: {}, 描述: {}", userId, amount, description);

        // 预先获取用户余额（用于计算 balanceAfter，避免@Modifying后JPA缓存陈旧）
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 原子性扣减积分（WHERE carbon_credits >= amount，并发安全，避免超扣）
        int rows = userRepository.deductCarbonCreditsAtomically(userId, amount);
        if (rows == 0) {
            log.warn("用户 {} 积分不足，当前余额: {}, 需要: {}", userId, user.getCarbonCredits(), amount);
            throw new RuntimeException("积分不足");
        }

        // 重新查询用户获取最新余额（原子更新后JPA一级缓存陈旧）
        userRepository.flush();
        int actualBalance = userRepository.findById(userId).map(User::getCarbonCredits).orElse(0);

        // 创建积分记录（负数）
        CarbonCreditRecord record = CarbonCreditRecord.builder()
                .userId(userId)
                .changeAmount(-amount)
                .changeType((byte) 3) // 3-兑换消耗
                .description(description)
                .balanceAfter(actualBalance)
                .build();
        creditRecordRepository.save(record);

        log.info("兑换成功，消耗积分: {}, 当前余额: {}", amount, actualBalance);
    }

    /**
     * 获取用户积分余额
     *
     * @param userId 用户ID
     * @return 积分余额
     */
    public Integer getCreditBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return user.getCarbonCredits();
    }

    /**
     * 获取用户积分记录
     *
     * @param userId     用户ID
     * @param creditType 积分类型（可选，null表示全部）
     * @return 积分记录列表
     */
    public List<CreditRecordVO> getCreditHistory(Long userId, Byte creditType) {
        List<CarbonCreditRecord> records;

        if (creditType != null) {
            records = creditRecordRepository.findByUserIdAndChangeTypeOrderByCreateTimeDesc(userId, creditType);
        } else {
            records = creditRecordRepository.findByUserIdOrderByCreateTimeDesc(userId);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return records.stream()
                .map(record -> {
                    // 根据积分类型计算对应的碳减排量
                    // 碳减排量 = 积分变动 / 10 * 0.785（kg/kWh）
                    BigDecimal carbonReduction = null;
                    if (record.getChangeAmount() != null && record.getChangeAmount() > 0 &&
                        record.getChangeType() != null && record.getChangeType() == 1) {
                        // 只有充电获得的积分才计算碳减排量
                        int credits = record.getChangeAmount();
                        // 积分 = 充电量(kWh) * 碳排放因子(0.785kg/kWh) * 10
                        // 所以：充电量 = 积分 / (0.785 * 10)
                        BigDecimal chargeAmount = BigDecimal.valueOf(credits)
                                .divide(BigDecimal.valueOf(7.85), 2, java.math.RoundingMode.HALF_UP);
                        carbonReduction = chargeAmount
                                .multiply(BigDecimal.valueOf(0.785))
                                .setScale(2, java.math.RoundingMode.HALF_UP);
                    }

                    return CreditRecordVO.builder()
                            .id(record.getId())
                            .creditChange(record.getChangeAmount())
                            .creditType(record.getChangeType())
                            .creditTypeText(CreditRecordVO.getCreditTypeText(record.getChangeType()))
                            .description(record.getDescription())
                            .balanceAfter(record.getBalanceAfter())
                            .carbonReduction(carbonReduction)
                            .createTime(record.getCreateTime())
                            .createTimeFormatted(record.getCreateTime() != null ?
                                    record.getCreateTime().format(formatter) : "")
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取用户积分统计
     *
     * @param userId 用户ID
     * @return 积分统计
     */
    public CreditStatisticsVO getCreditStatistics(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 累计获得和消耗
        Integer totalEarned = creditRecordRepository.sumTotalEarnedCredits(userId);
        Integer totalSpent = creditRecordRepository.sumTotalSpentCredits(userId);

        // 本月获得和消耗
        LocalDate todayForMonth = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        LocalDateTime monthStart = todayForMonth.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = todayForMonth.atTime(LocalTime.MAX);

        Integer monthEarned = creditRecordRepository.sumEarnedCreditsByPeriod(userId, monthStart, monthEnd);
        Integer monthSpent = creditRecordRepository.sumSpentCreditsByPeriod(userId, monthStart, monthEnd);

        // 连续签到天数
        int continuousDays = calculateContinuousCheckInDays(userId);

        // 今天是否已签到
        LocalDate todayForCheckIn = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        LocalDateTime startOfDay = todayForCheckIn.atStartOfDay();
        LocalDateTime endOfDay = todayForCheckIn.atTime(LocalTime.MAX);
        boolean hasCheckedInToday = creditRecordRepository.existsByUserIdAndChangeTypeAndCreateTimeBetween(
                userId, (byte) 2, startOfDay, endOfDay);

        return CreditStatisticsVO.builder()
                .currentBalance(user.getCarbonCredits())
                .totalEarned(totalEarned != null ? totalEarned : 0)
                .totalSpent(totalSpent != null ? totalSpent : 0)
                .monthEarned(monthEarned != null ? monthEarned : 0)
                .monthSpent(monthSpent != null ? monthSpent : 0)
                .continuousCheckInDays(continuousDays)
                .hasCheckedInToday(hasCheckedInToday)
                .build();
    }

    /**
     * 计算连续签到天数
     *
     * @param userId 用户ID
     * @return 连续签到天数
     */
    private int calculateContinuousCheckInDays(Long userId) {
        List<CarbonCreditRecord> checkInRecords = creditRecordRepository.findCheckInRecords(userId, (byte) 2);

        if (checkInRecords.isEmpty()) {
            return 0;
        }

        int continuousDays = 0;
        LocalDate lastDate = null;

        for (CarbonCreditRecord record : checkInRecords) {
            LocalDate currentDate = record.getCreateTime().toLocalDate();

            if (lastDate == null) {
                // 第一条记录
                LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
                // 只有今天或昨天签到才算连续
                if (currentDate.equals(today) || currentDate.equals(today.minusDays(1))) {
                    continuousDays = 1;
                    lastDate = currentDate;
                } else {
                    break;
                }
            } else {
                // 检查是否连续
                long daysBetween = ChronoUnit.DAYS.between(currentDate, lastDate);
                if (daysBetween == 1) {
                    continuousDays++;
                    lastDate = currentDate;
                } else {
                    break;
                }
            }
        }

        return continuousDays;
    }
}
