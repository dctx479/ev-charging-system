package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.entity.ChargingStation;
import com.ev.charging.entity.EnergyData;
import com.ev.charging.exception.BusinessException;
import com.ev.charging.repository.ChargingStationRepository;
import com.ev.charging.repository.EnergyDataRepository;
import com.ev.charging.vo.EnergyDataVO;
import com.ev.charging.vo.EnergyRealtimeVO;
import com.ev.charging.vo.EnergyStatisticsVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "能源监控", description = "能源数据实时监控、历史查询、统计分析")
@RestController
@RequestMapping("/energy")
@RequiredArgsConstructor
public class EnergyController {

    private final EnergyDataRepository energyDataRepository;
    private final ChargingStationRepository stationRepository;

    @GetMapping("/realtime")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'OPERATOR')")
    public Result<Map<String, Object>> getRealtimeData(@RequestParam Long stationId) {
        EnergyData data = energyDataRepository
            .findTopByStationIdOrderByRecordTimeDesc(stationId)
            .orElseThrow(() -> new BusinessException("暂无能源数据"));

        ChargingStation station = stationRepository.findById(stationId)
            .orElseThrow(() -> new BusinessException("充电站不存在"));

        // Map to frontend expected field names
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("solarPower", data.getSolarGeneration());
        result.put("storagePower", data.getBatteryDischarge());
        result.put("storageSoc", data.getBatterySoc());
        result.put("chargingLoad", data.getTotalConsumption());
        result.put("gridPower", data.getGridPurchase());
        result.put("solarCapacity", station.getSolarCapacity());
        result.put("storageCapacity", station.getStorageCapacity());
        result.put("recordTime", data.getRecordTime());

        return Result.success(result);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'OPERATOR')")
    public Result<List<Map<String, Object>>> getHistoryData(
        @RequestParam(required = false) Long stationId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        // 若未传 startTime/endTime 则默认查最近7天；若未传 stationId 则查全部站点
        if (startTime == null) {
            startTime = LocalDateTime.now().minusDays(7);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        List<EnergyData> dataList;
        if (stationId != null) {
            dataList = energyDataRepository.findByStationIdAndTimeRange(stationId, startTime, endTime);
        } else {
            dataList = energyDataRepository.findByRecordTimeBetween(startTime, endTime);
        }

        return Result.success(dataList.stream()
            .map(data -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("time", data.getRecordTime() != null ? data.getRecordTime().toString() : "");
                map.put("solarPower", data.getSolarGeneration());
                map.put("storagePower", data.getBatteryDischarge());
                map.put("gridPower", data.getGridPurchase());
                map.put("chargingLoad", data.getTotalConsumption());
                return map;
            })
            .collect(Collectors.toList())
        );
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'OPERATOR')")
    public Result<Map<String, Object>> getStatistics(
        @RequestParam(required = false) Long stationId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        // 若未传 date 则默认查今天；若未传 stationId 则汇总全部站点
        if (date == null) {
            date = LocalDate.now();
        }
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.plusDays(1).atStartOfDay();

        List<EnergyData> dataList;
        if (stationId != null) {
            dataList = energyDataRepository.findByStationIdAndTimeRange(stationId, startTime, endTime);
        } else {
            dataList = energyDataRepository.findByRecordTimeBetween(startTime, endTime);
        }

        BigDecimal totalSolarGeneration = dataList.stream()
            .map(d -> d.getSolarGeneration() != null ? d.getSolarGeneration() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGridPurchase = dataList.stream()
            .map(d -> d.getGridPurchase() != null ? d.getGridPurchase() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalConsumption = dataList.stream()
            .map(d -> d.getTotalConsumption() != null ? d.getTotalConsumption() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBatteryCharge = dataList.stream()
            .map(d -> d.getBatteryCharge() != null ? d.getBatteryCharge() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBatteryDischarge = dataList.stream()
            .map(d -> d.getBatteryDischarge() != null ? d.getBatteryDischarge() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算可再生能源占比（光伏发电量 / 总消费量）
        // 注意：分母用totalConsumption（总消费），不是totalSolarGeneration+totalGridPurchase
        BigDecimal renewableRatio = totalConsumption.compareTo(BigDecimal.ZERO) > 0 ?
            totalSolarGeneration
                .divide(totalConsumption, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;

        // 计算节省的电费(光伏发电按平均电价0.8元/度计算)
        BigDecimal savedCost = totalSolarGeneration.multiply(BigDecimal.valueOf(0.8));

        // 计算碳减排 (每度电减少0.785kg碳排放)
        BigDecimal carbonReduction = totalSolarGeneration.multiply(BigDecimal.valueOf(0.785));

        // 计算节约用煤 (每度电相当于0.4kg标准煤)
        BigDecimal coalSaving = totalSolarGeneration.multiply(BigDecimal.valueOf(0.4));

        // 相当于植树 (1棵树每年吸收18.3kg CO2)
        BigDecimal treeEquivalent = carbonReduction.divide(BigDecimal.valueOf(18.3), 0, RoundingMode.HALF_UP);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("todaySolarGeneration", totalSolarGeneration);
        result.put("todayStorageCharge", totalBatteryCharge);
        result.put("todayStorageDischarge", totalBatteryDischarge);
        result.put("todayGridConsumption", totalGridPurchase);
        result.put("totalChargingLoad", totalConsumption);
        result.put("todayChargingTotal", totalConsumption);
        result.put("renewableRatio", renewableRatio);
        result.put("savedCost", savedCost);
        result.put("carbonReduction", carbonReduction);
        result.put("coalSaving", coalSaving);
        result.put("treeEquivalent", treeEquivalent);

        return Result.success(result);
    }

    @GetMapping("/stations")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'OPERATOR')")
    public Result<List<Map<String, Object>>> getStationsEnergyData() {
        // Get all stations with their latest energy data
        List<ChargingStation> stations = stationRepository.findAll();

        return Result.success(stations.stream()
            .map(station -> {
                var latestData = energyDataRepository
                    .findTopByStationIdOrderByRecordTimeDesc(station.getId())
                    .orElse(null);

                Map<String, Object> map = new java.util.HashMap<>();
                map.put("stationId", station.getId());
                map.put("stationName", station.getName());
                map.put("solarGeneration", latestData != null ? latestData.getSolarGeneration() : BigDecimal.ZERO);
                map.put("storageSoc", latestData != null ? latestData.getBatterySoc() : BigDecimal.ZERO);
                map.put("chargingLoad", latestData != null ? latestData.getTotalConsumption() : BigDecimal.ZERO);
                map.put("gridPower", latestData != null ? latestData.getGridPurchase() : BigDecimal.ZERO);

                // Calculate renewable ratio for this station
                if (latestData != null && latestData.getTotalConsumption() != null &&
                    latestData.getTotalConsumption().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal renewable = latestData.getSolarGeneration() != null ?
                        latestData.getSolarGeneration() : BigDecimal.ZERO;
                    if (latestData.getBatteryDischarge() != null) {
                        renewable = renewable.add(latestData.getBatteryDischarge());
                    }
                    BigDecimal ratio = renewable.divide(latestData.getTotalConsumption(), 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                    map.put("renewableRatio", ratio);
                } else {
                    map.put("renewableRatio", BigDecimal.ZERO);
                }

                return map;
            })
            .collect(Collectors.toList())
        );
    }

    private EnergyDataVO convertToVO(EnergyData data) {
        EnergyDataVO vo = new EnergyDataVO();
        BeanUtils.copyProperties(data, vo);
        return vo;
    }
}
