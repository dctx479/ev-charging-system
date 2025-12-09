package com.ev.charging.service;

import com.ev.charging.entity.ChargingPile;
import com.ev.charging.repository.ChargingPileRepository;
import com.ev.charging.repository.ChargingStationRepository;
import com.ev.charging.repository.FaultRecordRepository;
import com.ev.charging.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计服务
 */
@Slf4j
@Service
public class StatisticsService {

    @Autowired
    private ChargingPileRepository chargingPileRepository;

    @Autowired
    private ChargingStationRepository chargingStationRepository;

    @Autowired
    private FaultRecordRepository faultRecordRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 获取仪表板数据
     */
    public DashboardVO getDashboardData() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().plusDays(1).atStartOfDay();

        // 今日充电量和收入（从订单表查询）
        String orderSql = "SELECT " +
                "COALESCE(SUM(charge_amount), 0) as todayChargeAmount, " +
                "COALESCE(SUM(total_fee), 0) as todayIncome, " +
                "COUNT(*) as todayOrderCount " +
                "FROM charge_order " +
                "WHERE create_time >= ? AND create_time < ? AND order_status != 4";

        Map<String, Object> orderStats = jdbcTemplate.queryForMap(orderSql, todayStart, todayEnd);

        // 充电桩状态统计
        List<ChargingPile> allPiles = chargingPileRepository.findAll();
        int onlineCount = 0;
        int faultCount = 0;
        int chargingCount = 0;

        for (ChargingPile pile : allPiles) {
            switch (pile.getStatus()) {
                case 1: // 空闲
                case 3: // 预约中
                    onlineCount++;
                    break;
                case 2: // 充电中
                    chargingCount++;
                    onlineCount++;
                    break;
                case 4: // 故障
                    faultCount++;
                    break;
            }
        }

        // 待维修故障数
        Long pendingFaultCount = faultRecordRepository.countByStationIdAndRepairStatus(null, (byte) 0);

        // 紧急故障数
        List<?> urgentFaults = faultRecordRepository.findUrgentPendingFaults();

        return DashboardVO.builder()
                .todayChargeAmount(new BigDecimal(orderStats.get("todayChargeAmount").toString()))
                .todayIncome(new BigDecimal(orderStats.get("todayIncome").toString()))
                .todayOrderCount(((Number) orderStats.get("todayOrderCount")).intValue())
                .onlinePileCount(onlineCount)
                .faultPileCount(faultCount)
                .chargingPileCount(chargingCount)
                .pendingFaultCount(pendingFaultCount != null ? pendingFaultCount.intValue() : 0)
                .urgentFaultCount(urgentFaults.size())
                .totalPileCount(allPiles.size())
                .totalStationCount((int) chargingStationRepository.count())
                .build();
    }

    /**
     * 获取充电趋势（最近7天）
     */
    public ChargeTrendVO getChargeTrend(Integer days) {
        if (days == null || days <= 0) {
            days = 7;
        }

        List<String> dates = new ArrayList<>();
        List<BigDecimal> chargeAmounts = new ArrayList<>();
        List<BigDecimal> incomes = new ArrayList<>();
        List<Integer> orderCounts = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            dates.add(date.format(formatter));

            // 查询当天统计数据
            String sql = "SELECT " +
                    "COALESCE(SUM(charge_amount), 0) as chargeAmount, " +
                    "COALESCE(SUM(total_fee), 0) as income, " +
                    "COUNT(*) as orderCount " +
                    "FROM charge_order " +
                    "WHERE create_time >= ? AND create_time < ? AND order_status != 4";

            try {
                Map<String, Object> stats = jdbcTemplate.queryForMap(sql, dayStart, dayEnd);
                chargeAmounts.add(new BigDecimal(stats.get("chargeAmount").toString()));
                incomes.add(new BigDecimal(stats.get("income").toString()));
                orderCounts.add(((Number) stats.get("orderCount")).intValue());
            } catch (Exception e) {
                log.warn("查询日期{}的数据失败: {}", date, e.getMessage());
                chargeAmounts.add(BigDecimal.ZERO);
                incomes.add(BigDecimal.ZERO);
                orderCounts.add(0);
            }
        }

        return ChargeTrendVO.builder()
                .dates(dates)
                .chargeAmounts(chargeAmounts)
                .incomes(incomes)
                .orderCounts(orderCounts)
                .build();
    }

    /**
     * 获取充电桩状态分布
     */
    public PileStatusDistributionVO getPileStatusDistribution() {
        List<ChargingPile> allPiles = chargingPileRepository.findAll();

        int availableCount = 0;
        int chargingCount = 0;
        int reservedCount = 0;
        int faultCount = 0;
        int offlineCount = 0;

        for (ChargingPile pile : allPiles) {
            switch (pile.getStatus()) {
                case 1:
                    availableCount++;
                    break;
                case 2:
                    chargingCount++;
                    break;
                case 3:
                    reservedCount++;
                    break;
                case 4:
                    faultCount++;
                    break;
                case 5:
                    offlineCount++;
                    break;
            }
        }

        return PileStatusDistributionVO.builder()
                .availableCount(availableCount)
                .chargingCount(chargingCount)
                .reservedCount(reservedCount)
                .faultCount(faultCount)
                .offlineCount(offlineCount)
                .build();
    }

    /**
     * 获取站点收入排行（Top 10）
     */
    public List<StationRankingVO> getStationRanking(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        String sql = "SELECT " +
                "s.id as stationId, " +
                "s.name as stationName, " +
                "COALESCE(SUM(o.total_fee), 0) as totalIncome, " +
                "COALESCE(SUM(o.charge_amount), 0) as totalChargeAmount, " +
                "COUNT(o.id) as totalOrderCount " +
                "FROM charging_station s " +
                "LEFT JOIN charge_order o ON s.id = o.station_id AND o.order_status != 4 " +
                "GROUP BY s.id, s.name " +
                "ORDER BY totalIncome DESC " +
                "LIMIT ?";

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, limit);

            return results.stream().map(row -> StationRankingVO.builder()
                    .stationId(((Number) row.get("stationId")).longValue())
                    .stationName((String) row.get("stationName"))
                    .totalIncome(new BigDecimal(row.get("totalIncome").toString()))
                    .totalChargeAmount(new BigDecimal(row.get("totalChargeAmount").toString()))
                    .totalOrderCount(((Number) row.get("totalOrderCount")).intValue())
                    .build()).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("查询站点排行失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取订单统计（按日期范围）
     */
    public Map<String, Object> getOrderStatistics(String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();

        try {
            String sql = "SELECT " +
                    "COUNT(*) as totalCount, " +
                    "SUM(CASE WHEN order_status = 3 THEN 1 ELSE 0 END) as completedCount, " +
                    "SUM(CASE WHEN order_status IN (1, 2) THEN 1 ELSE 0 END) as inProgressCount, " +
                    "SUM(CASE WHEN order_status = 4 THEN 1 ELSE 0 END) as cancelledCount, " +
                    "COALESCE(SUM(charge_amount), 0) as totalChargeAmount, " +
                    "COALESCE(SUM(total_fee), 0) as totalRevenue " +
                    "FROM charge_order " +
                    "WHERE DATE(create_time) >= ? AND DATE(create_time) <= ?";

            Map<String, Object> stats = jdbcTemplate.queryForMap(sql, startDate, endDate);

            result.put("totalCount", ((Number) stats.get("totalCount")).intValue());
            result.put("completedCount", ((Number) stats.get("completedCount")).intValue());
            result.put("inProgressCount", ((Number) stats.get("inProgressCount")).intValue());
            result.put("cancelledCount", ((Number) stats.get("cancelledCount")).intValue());
            result.put("totalChargeAmount", new BigDecimal(stats.get("totalChargeAmount").toString()));
            result.put("totalRevenue", new BigDecimal(stats.get("totalRevenue").toString()));
        } catch (Exception e) {
            log.error("查询订单统计失败", e);
            result.put("totalCount", 0);
            result.put("completedCount", 0);
            result.put("inProgressCount", 0);
            result.put("cancelledCount", 0);
            result.put("totalChargeAmount", BigDecimal.ZERO);
            result.put("totalRevenue", BigDecimal.ZERO);
        }

        return result;
    }

    /**
     * 获取收入统计（按周期）
     */
    public Map<String, Object> getRevenueStatistics(String period) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();

        int days;
        String dateFormat;
        String groupBy;

        switch (period) {
            case "week":
                days = 7;
                dateFormat = "%Y-%m-%d";
                groupBy = "DATE(create_time)";
                break;
            case "month":
                days = 30;
                dateFormat = "%Y-%m-%d";
                groupBy = "DATE(create_time)";
                break;
            default: // day
                days = 7;
                dateFormat = "%Y-%m-%d";
                groupBy = "DATE(create_time)";
                break;
        }

        try {
            String sql = "SELECT DATE_FORMAT(create_time, ?) as dateKey, " +
                    "COALESCE(SUM(total_fee), 0) as revenue " +
                    "FROM charge_order " +
                    "WHERE create_time >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                    "AND order_status != 4 " +
                    "GROUP BY dateKey " +
                    "ORDER BY dateKey";

            List<Map<String, Object>> queryResults = jdbcTemplate.queryForList(sql, dateFormat, days);

            // 构建日期到收入的映射
            Map<String, BigDecimal> revenueMap = new HashMap<>();
            for (Map<String, Object> row : queryResults) {
                String dateKey = (String) row.get("dateKey");
                BigDecimal revenue = new BigDecimal(row.get("revenue").toString());
                revenueMap.put(dateKey, revenue);
            }

            // 生成完整日期列表
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                String dateKey = date.format(formatter);
                dates.add(dateKey);
                revenues.add(revenueMap.getOrDefault(dateKey, BigDecimal.ZERO));
            }

        } catch (Exception e) {
            log.error("查询收入统计失败", e);
            // 返回空数据
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                dates.add(date.format(formatter));
                revenues.add(BigDecimal.ZERO);
            }
        }

        result.put("dates", dates);
        result.put("revenues", revenues);
        return result;
    }
}
