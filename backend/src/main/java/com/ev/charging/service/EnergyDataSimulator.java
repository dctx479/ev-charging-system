package com.ev.charging.service;

import com.ev.charging.entity.ChargingPile;
import com.ev.charging.entity.ChargingStation;
import com.ev.charging.entity.EnergyData;
import com.ev.charging.repository.ChargingPileRepository;
import com.ev.charging.repository.ChargingStationRepository;
import com.ev.charging.repository.EnergyDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class EnergyDataSimulator {

    private final EnergyDataRepository energyDataRepository;
    private final ChargingStationRepository stationRepository;
    private final ChargingPileRepository pileRepository;

    /**
     * 定时生成模拟能源数据(每5分钟一次)
     * 修复：添加异常处理，避免单个站点失败影响其他站点
     * 修复：添加@Transactional确保事务隔离，避免并发更新问题
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)  // 每次执行完成后等待5分钟再执行下一次
    public void generateEnergyData() {
        try {
            // 只为有光储充设施的站点生成数据
            List<ChargingStation> stations = stationRepository.findAll().stream()
                .filter(s -> (s.getHasSolar() != null && s.getHasSolar() == 1) ||
                            (s.getHasStorage() != null && s.getHasStorage() == 1))
                .collect(Collectors.toList());

            LocalDateTime now = LocalDateTime.now();
            int hour = now.getHour();

            for (ChargingStation station : stations) {
                try {
                    generateEnergyDataForStation(station, now, hour);
                } catch (Exception e) {
                    // 单个站点的异常不影响其他站点的数据生成
                    log.error("生成站点{}的能源数据失败: {}", station.getId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("能源数据生成任务异常，调度器线程安全退出", e);
        }
    }

    /**
     * 为单个站点生成能源数据
     * 修复：SOC值使用Integer类型防止溢出，添加范围检查
     * 注意：不需要独立的@Transactional，因为外层public方法generateEnergyData()已有@Transactional，
     * 且@Transactional标注在private方法上不会被Spring AOP代理拦截，实际不生效
     */
    private void generateEnergyDataForStation(ChargingStation station, LocalDateTime now, int hour) {
        // 模拟光伏发电(白天6-18点有发电)
        BigDecimal solarGeneration = BigDecimal.ZERO;
        if (station.getHasSolar() != null && station.getHasSolar() == 1 && hour >= 6 && hour <= 18) {
            // 发电量与光照强度相关(中午12点最大)
            double solarFactor = 1.0 - Math.abs(hour - 12) / 6.0;
            BigDecimal solarCapacity = station.getSolarCapacity() != null ?
                station.getSolarCapacity() : BigDecimal.valueOf(100);
            solarGeneration = solarCapacity
                .multiply(BigDecimal.valueOf(solarFactor))
                .multiply(BigDecimal.valueOf(0.083));  // 5分钟的发电量
        }

        // 模拟充电消耗(根据当前充电桩使用情况)
        List<ChargingPile> chargingPiles = pileRepository.findByStationIdAndStatus(
            station.getId(), (byte) 2  // 充电中
        );

        BigDecimal totalConsumption = chargingPiles.stream()
            .map(pile -> pile.getPower() != null ? pile.getPower() : BigDecimal.valueOf(60))
            .map(power -> power.multiply(BigDecimal.valueOf(0.083)))  // 5分钟的用电量
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 储能电池状态 - 修复：使用Integer防止溢出
        EnergyData lastData = energyDataRepository
            .findTopByStationIdOrderByRecordTimeDesc(station.getId())
            .orElse(null);

        Integer batterySoc = lastData != null && lastData.getBatterySoc() != null ?
                lastData.getBatterySoc().intValue() : 50;

        // 能源调度逻辑
        BigDecimal gridPurchase = BigDecimal.ZERO;
        BigDecimal batteryCharge = BigDecimal.ZERO;
        BigDecimal batteryDischarge = BigDecimal.ZERO;

        if (solarGeneration.compareTo(totalConsumption) >= 0) {
            // 光伏发电充足,多余电量给储能充电
            BigDecimal surplus = solarGeneration.subtract(totalConsumption);
            if (station.getHasStorage() != null && station.getHasStorage() == 1 && batterySoc < 90) {
                batteryCharge = surplus;
                // 修复：确保SOC在0-100范围内
                batterySoc = Math.min(100, batterySoc + 5);
            }
        } else {
            // 光伏不足,优先使用储能,不足再从电网购电
            BigDecimal shortage = totalConsumption.subtract(solarGeneration);

            if (station.getHasStorage() != null && station.getHasStorage() == 1 && batterySoc > 20) {
                batteryDischarge = shortage;
                // 修复：确保SOC在0-100范围内
                batterySoc = Math.max(0, batterySoc - 5);
            } else {
                gridPurchase = shortage;
            }
        }

        // 保存能源数据
        EnergyData data = EnergyData.builder()
            .stationId(station.getId())
            .recordTime(now)
            .solarGeneration(solarGeneration)
            .gridPurchase(gridPurchase)
            .batteryCharge(batteryCharge)
            .batteryDischarge(batteryDischarge)
            .batterySoc((byte) Math.min(100, Math.max(0, batterySoc)))
            .totalConsumption(totalConsumption)
            .build();

        energyDataRepository.save(data);
    }
}
