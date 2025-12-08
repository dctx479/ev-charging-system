package com.ev.charging.service;

import com.ev.charging.dto.CreditRecordVO;
import com.ev.charging.dto.CreditStatisticsVO;
import com.ev.charging.entity.CarbonCreditRecord;
import com.ev.charging.entity.User;
import com.ev.charging.repository.CarbonCreditRecordRepository;
import com.ev.charging.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
     * @param orderId      订单ID
     * @param userId       用户ID
     * @param chargeAmount 充电量(kWh)
     * @return 获得的积分
     */
    @Transactional
    public Integer addCreditsForCharging(Long orderId, Long userId, BigDecimal chargeAmount) {
        log.info("为订单 {} 添加充电积分，用户ID: {}, 充电量: {} kWh", orderId, userId, chargeAmount);

        // 计算积分
        int credits = (int) (chargeAmount.doubleValue() * CARBON_EMISSION_FACTOR * CREDIT_MULTIPLIER);

        // 更新用户积分余额
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setCarbonCredits(user.getCarbonCredits() + credits);
        userRepository.save(user);

        // 创建积分记录
        CarbonCreditRecord record = CarbonCreditRecord.builder()
                .userId(userId)
                .orderId(orderId)
                .creditChange(credits)
                .creditType((byte) 1) // 1-充电获得
                .description(String.format("充电%.2f kWh，获得碳积分", chargeAmount))
                .balanceAfter(user.getCarbonCredits())
                .build();
        creditRecordRepository.save(record);

        log.info("充电积分添加成功，获得积分: {}, 当前余额: {}", credits, user.getCarbonCredits());
        return credits;
    }

    /**
     * 每日签到获得积分
     *
     * @param userId 用户ID
     * @return 获得的积分
     */
    @Transactional
    public Integer dailyCheckIn(Long userId) {
        log.info("用户 {} 尝试签到", userId);

        // 检查今天是否已签到
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        boolean hasCheckedIn = creditRecordRepository.existsByUserIdAndCreditTypeAndCreateTimeBetween(
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

        // 更新用户积分余额
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setCarbonCredits(user.getCarbonCredits() + credits);
        userRepository.save(user);

        // 创建积分记录
        String description = (credits > DAILY_CHECK_IN_CREDITS)
                ? String.format("每日签到（连续%d天，额外奖励%d积分）", continuousDays + 1, CONTINUOUS_7_DAYS_BONUS)
                : "每日签到";

        CarbonCreditRecord record = CarbonCreditRecord.builder()
                .userId(userId)
                .creditChange(credits)
                .creditType((byte) 2) // 2-签到
                .description(description)
                .balanceAfter(user.getCarbonCredits())
                .build();
        creditRecordRepository.save(record);

        log.info("签到成功，获得积分: {}, 当前余额: {}", credits, user.getCarbonCredits());
        return credits;
    }

    /**
     * 兑换消耗积分
     *
     * @param userId      用户ID
     * @param amount      消耗积分数量
     * @param description 兑换描述
     */
    @Transactional
    public void redeemCredits(Long userId, Integer amount, String description) {
        log.info("用户 {} 兑换积分: {}, 描述: {}", userId, amount, description);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查积分余额
        if (user.getCarbonCredits() < amount) {
            log.warn("用户 {} 积分不足，当前余额: {}, 需要: {}", userId, user.getCarbonCredits(), amount);
            throw new RuntimeException("积分不足");
        }

        // 扣除积分
        user.setCarbonCredits(user.getCarbonCredits() - amount);
        userRepository.save(user);

        // 创建积分记录（负数）
        CarbonCreditRecord record = CarbonCreditRecord.builder()
                .userId(userId)
                .creditChange(-amount)
                .creditType((byte) 3) // 3-兑换消耗
                .description(description)
                .balanceAfter(user.getCarbonCredits())
                .build();
        creditRecordRepository.save(record);

        log.info("兑换成功，消耗积分: {}, 当前余额: {}", amount, user.getCarbonCredits());
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
            records = creditRecordRepository.findByUserIdAndCreditTypeOrderByCreateTimeDesc(userId, creditType);
        } else {
            records = creditRecordRepository.findByUserIdOrderByCreateTimeDesc(userId);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return records.stream()
                .map(record -> CreditRecordVO.builder()
                        .id(record.getId())
                        .creditChange(record.getCreditChange())
                        .creditType(record.getCreditType())
                        .creditTypeText(CreditRecordVO.getCreditTypeText(record.getCreditType()))
                        .description(record.getDescription())
                        .balanceAfter(record.getBalanceAfter())
                        .createTime(record.getCreateTime())
                        .createTimeFormatted(record.getCreateTime().format(formatter))
                        .build())
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
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = LocalDate.now().atTime(LocalTime.MAX);

        Integer monthEarned = creditRecordRepository.sumEarnedCreditsByPeriod(userId, monthStart, monthEnd);
        Integer monthSpent = creditRecordRepository.sumSpentCreditsByPeriod(userId, monthStart, monthEnd);

        // 连续签到天数
        int continuousDays = calculateContinuousCheckInDays(userId);

        // 今天是否已签到
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        boolean hasCheckedInToday = creditRecordRepository.existsByUserIdAndCreditTypeAndCreateTimeBetween(
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
                LocalDate today = LocalDate.now();
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
