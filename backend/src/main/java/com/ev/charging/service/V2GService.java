package com.ev.charging.service;

import com.ev.charging.dto.StartDischargeDTO;
import com.ev.charging.dto.StopDischargeDTO;
import com.ev.charging.entity.ChargingPile;
import com.ev.charging.entity.User;
import com.ev.charging.entity.V2GRecord;
import com.ev.charging.exception.BusinessException;
import com.ev.charging.repository.ChargingPileRepository;
import com.ev.charging.repository.UserRepository;
import com.ev.charging.repository.V2GRecordRepository;
import com.ev.charging.vo.V2GRecordVO;
import com.ev.charging.vo.V2GStatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * V2G双向充电服务
 * 处理V2G放电启动、停止、收益计算等业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class V2GService {

    private final V2GRecordRepository v2gRecordRepository;
    private final ChargingPileRepository pileRepository;
    private final com.ev.charging.repository.ChargingStationRepository stationRepository;
    private final UserRepository userRepository;
    private final ElectricityPriceService electricityPriceService;
    private final CacheService cacheService;

    /**
     * 车辆最低 SOC 阈值（%）
     * 根据行业标准，电池 SOC 低于 20% 会损伤电池寿命，不允许放电
     */
    private static final int MIN_SOC_THRESHOLD = 20;

    /**
     * 开始V2G放电
     *
     * SOC 保护机制：
     * - 检查当前 SOC 是否低于最低阈值 (20%)
     * - 如果低于阈值，拒绝放电请求
     * - 防止电池过度放电而损伤
     */
    @Transactional(rollbackFor = Exception.class)
    public V2GRecordVO startDischarge(Long userId, StartDischargeDTO dto) {
        // 1. 验证充电桩是否支持V2G
        ChargingPile pile = pileRepository.findById(dto.getPileId())
            .orElseThrow(() -> new BusinessException("充电桩不存在"));

        if (pile.getSupportV2g() == null || pile.getSupportV2g() != 1) {
            throw new BusinessException("该充电桩不支持V2G功能");
        }

        if (pile.getStatus() == null || pile.getStatus() != 1) {
            throw new BusinessException("充电桩不可用");
        }

        // 2. 获取用户车辆信息
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("用户不存在"));

        if (user.getBatteryCapacity() == null) {
            throw new BusinessException("请先完善车辆信息");
        }

        // 3. 检查当前 SOC 是否满足放电条件（新增保护）
        Integer currentSoc = dto.getCurrentSoc();
        if (currentSoc == null || currentSoc < MIN_SOC_THRESHOLD) {
            log.warn("用户 {} 尝试放电，但 SOC 过低 (当前: {}%, 最低要求: {}%)",
                userId, currentSoc, MIN_SOC_THRESHOLD);
            throw new BusinessException(
                String.format("电池电量过低 (当前: %d%%)，最少需要 %d%% 才能放电，请先充电",
                    currentSoc, MIN_SOC_THRESHOLD)
            );
        }

        // 4. 获取实时电价
        BigDecimal currentPrice = electricityPriceService.getRealTimePrice();

        // 5. 检查电价是否满足用户设置的最低价格
        if (dto.getMinPrice() != null && currentPrice.compareTo(dto.getMinPrice()) < 0) {
            throw new BusinessException("当前电价未达到设定值");
        }

        // 6. 创建V2G放电记录
        V2GRecord record = V2GRecord.builder()
            .userId(userId)
            .pileId(dto.getPileId())
            .stationId(pile.getStationId())
            .recordType((byte) 2)  // 放电
            .startTime(LocalDateTime.now())
            .electricityPrice(currentPrice)
            .batteryHealthBefore(dto.getBatteryHealthBefore())
            .build();

        v2gRecordRepository.save(record);

        // 7. 原子性更新充电桩状态为"充电中"(实际是放电,但占用充电桩)
        // CAS操作：只有当前状态为空闲(1)时才能更新为充电中(2)
        int updated = pileRepository.updatePileStatusAtomically(dto.getPileId(), (byte) 2, (byte) 1);
        if (updated == 0) {
            log.warn("充电桩 {} 状态更新失败，可能已被占用", dto.getPileId());
            throw new BusinessException("充电桩状态更新失败，请重试");
        }
        cacheService.evictPileCache(dto.getPileId());

        log.info("用户 {} 开始V2G放电，记录ID: {}, 充电桩ID: {}, 当前SOC: {}%",
            userId, record.getId(), dto.getPileId(), currentSoc);

        return convertToVO(record);
    }

    /**
     * 停止V2G放电并结算收益
     *
     * 竞态条件防护：使用原子操作确保同一记录只能停止一次
     */
    @Transactional(rollbackFor = Exception.class)
    public V2GRecordVO stopDischarge(Long userId, StopDischargeDTO dto) {
        // 1. 查询放电记录
        V2GRecord record = v2gRecordRepository.findById(dto.getRecordId())
            .orElseThrow(() -> new BusinessException("记录不存在"));

        // 2. 验证记录所有权
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此放电记录");
        }

        if (record.getRecordType() == null || record.getRecordType() != 2) {
            throw new BusinessException("不是放电记录");
        }

        // 3. 使用原子操作停止放电，防止 double-stop 竞态条件
        LocalDateTime endTime = LocalDateTime.now();
        int updated = v2gRecordRepository.atomicStopDischarge(
            dto.getRecordId(),
            endTime
        );

        if (updated == 0) {
            throw new BusinessException("放电已停止");
        }

        // 4. 重新查询已更新的记录
        record = v2gRecordRepository.findById(dto.getRecordId())
            .orElseThrow(() -> new BusinessException("记录不存在"));

        // 5. 检查最终 SOC 是否达到下限（防止继续放电）
        Integer finalSoc = dto.getFinalSoc();
        if (finalSoc != null && finalSoc <= MIN_SOC_THRESHOLD) {
            log.warn("用户 {} 放电停止，最终SOC: {}%，已达到保护阈值", userId, finalSoc);
        }

        // 6. 服务端计算放电量（不信任客户端提供的 energyAmount）
        // 放电量 = 充电桩放电功率 × 放电时长（小时）
        ChargingPile pile = pileRepository.findById(record.getPileId())
            .orElseThrow(() -> new BusinessException("充电桩不存在"));
        BigDecimal pilePower = pile.getPower() != null ? pile.getPower() : BigDecimal.valueOf(7);
        long durationMinutes = java.time.Duration.between(record.getStartTime(), endTime).toMinutes();
        BigDecimal serverEnergyAmount = pilePower
            .multiply(BigDecimal.valueOf(durationMinutes))
            .divide(BigDecimal.valueOf(60), 3, RoundingMode.HALF_UP);
        // 最小 0.1 kWh
        if (serverEnergyAmount.compareTo(BigDecimal.valueOf(0.1)) < 0) {
            serverEnergyAmount = BigDecimal.valueOf(0.1);
        }

        record.setEnergyAmount(serverEnergyAmount);
        record.setBatteryHealthAfter(dto.getBatteryHealthAfter());

        // 7. 计算收益
        // 收益 = 放电量 × 电价 × 0.9(平台收取10%手续费)
        BigDecimal price = record.getElectricityPrice() != null ? record.getElectricityPrice() : BigDecimal.ZERO;
        BigDecimal profit = serverEnergyAmount
            .multiply(price)
            .multiply(BigDecimal.valueOf(0.9))
            .setScale(2, RoundingMode.HALF_UP);

        record.setProfit(profit);
        record.setAmount(profit);
        v2gRecordRepository.save(record);

        // 8. 使用原子操作增加用户余额
        int balanceUpdated = userRepository.addBalanceAtomically(record.getUserId(), profit);
        if (balanceUpdated == 0) {
            throw new BusinessException("余额更新失败");
        }

        // 9. 原子性更新充电桩状态为"空闲"
        // CAS操作：只有当前状态为充电中(2)时才能更新为空闲(1)
        int pileUpdated = pileRepository.updatePileStatusAtomically(record.getPileId(), (byte) 1, (byte) 2);
        if (pileUpdated == 0) {
            log.error("充电桩 {} 状态更新失败，可能已被其他操作修改", record.getPileId());
            throw new BusinessException("充电桩状态更新失败，请联系管理员");
        }
        cacheService.evictPileCache(record.getPileId());

        log.info("用户 {} 停止V2G放电，记录ID: {}, 收益: {}, 最终SOC: {}%",
            userId, record.getId(), profit, finalSoc);

        return convertToVO(record);
    }

    /**
     * 获取用户V2G记录
     */
    public List<V2GRecordVO> getRecords(Long userId) {
        List<V2GRecord> records = v2gRecordRepository.findByUserIdOrderByCreateTimeDesc(userId);
        return records.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }

    /**
     * 获取全平台V2G记录（管理端）
     */
    public List<V2GRecordVO> getAllRecords() {
        List<V2GRecord> records = v2gRecordRepository.findAllByOrderByCreateTimeDesc();
        return records.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }

    /**
     * 获取用户V2G统计数据
     */
    public V2GStatisticsVO getStatistics(Long userId) {
        BigDecimal totalProfit = v2gRecordRepository.sumProfitByUser(userId);
        BigDecimal totalDischargeAmount = v2gRecordRepository.sumDischargeAmountByUser(userId);

        return V2GStatisticsVO.builder()
            .totalProfit(totalProfit != null ? totalProfit : BigDecimal.ZERO)
            .totalDischargeAmount(totalDischargeAmount != null ? totalDischargeAmount : BigDecimal.ZERO)
            .averagePrice(totalDischargeAmount != null && totalDischargeAmount.compareTo(BigDecimal.ZERO) > 0 ?
                (totalProfit != null ? totalProfit : BigDecimal.ZERO).divide(totalDischargeAmount, 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO)
            .build();
    }

    /**
     * 获取管理端全平台V2G统计数据（不需要 userId）
     */
    public V2GStatisticsVO getAdminStatistics() {
        LocalDateTime todayStart = LocalDate.now(ZoneId.of("Asia/Shanghai")).atStartOfDay();

        BigDecimal todayDischarge = v2gRecordRepository.sumDischargeAmountSince(todayStart);
        BigDecimal todayIncome = v2gRecordRepository.sumProfitSince(todayStart);
        Long dischargeCount = v2gRecordRepository.countCompletedDischarge();
        Long userCount = v2gRecordRepository.countDistinctUsers();

        return V2GStatisticsVO.builder()
            .todayDischarge(todayDischarge != null ? todayDischarge : BigDecimal.ZERO)
            .todayIncome(todayIncome != null ? todayIncome : BigDecimal.ZERO)
            .dischargeCount(dischargeCount != null ? dischargeCount : 0L)
            .userCount(userCount != null ? userCount : 0L)
            .build();
    }

    private V2GRecordVO convertToVO(V2GRecord record) {
        V2GRecordVO vo = new V2GRecordVO();
        BeanUtils.copyProperties(record, vo);
        // 填充关联名称
        if (record.getUserId() != null) {
            userRepository.findById(record.getUserId())
                .ifPresent(u -> vo.setUserName(u.getNickname() != null ? u.getNickname() : u.getUsername()));
        }
        if (record.getPileId() != null) {
            pileRepository.findById(record.getPileId())
                .ifPresent(p -> vo.setPileNo(p.getPileNo()));
        }
        if (record.getStationId() != null) {
            stationRepository.findById(record.getStationId())
                .ifPresent(s -> vo.setStationName(s.getName()));
        }
        return vo;
    }
}
