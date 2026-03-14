package com.ev.charging.service;

import com.ev.charging.entity.ChargingPile;
import com.ev.charging.entity.ChargingStation;
import com.ev.charging.repository.ChargingPileRepository;
import com.ev.charging.repository.ChargingStationRepository;
import com.ev.charging.repository.FaultRecordRepository;
import com.ev.charging.vo.ChargeTrendVO;
import com.ev.charging.vo.DashboardVO;
import com.ev.charging.vo.PileStatusDistributionVO;
import com.ev.charging.vo.StationRankingVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 统计服务 - 单元测试
 * 测试仪表板、趋势、排行等统计功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("统计服务测试")
class StatisticsServiceTest {

    @Mock
    private ChargingPileRepository chargingPileRepository;

    @Mock
    private ChargingStationRepository chargingStationRepository;

    @Mock
    private FaultRecordRepository faultRecordRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private StatisticsService statisticsService;

    private List<ChargingPile> testPiles;
    private List<ChargingStation> testStations;

    @BeforeEach
    void setUp() {
        // 初始化测试充电桩
        testPiles = new ArrayList<>();

        // 5个空闲充电桩
        for (int i = 1; i <= 5; i++) {
            testPiles.add(ChargingPile.builder()
                    .id((long) i)
                    .pileNo("PL-00" + i)
                    .status((byte) 1)
                    .build());
        }

        // 3个充电中
        for (int i = 6; i <= 8; i++) {
            testPiles.add(ChargingPile.builder()
                    .id((long) i)
                    .pileNo("PL-00" + i)
                    .status((byte) 2)
                    .build());
        }

        // 2个预约中
        for (int i = 9; i <= 10; i++) {
            testPiles.add(ChargingPile.builder()
                    .id((long) i)
                    .pileNo("PL-0" + i)
                    .status((byte) 3)
                    .build());
        }

        // 2个故障
        for (int i = 11; i <= 12; i++) {
            testPiles.add(ChargingPile.builder()
                    .id((long) i)
                    .pileNo("PL-0" + i)
                    .status((byte) 4)
                    .build());
        }

        // 1个离线
        testPiles.add(ChargingPile.builder()
                .id(13L)
                .pileNo("PL-13")
                .status((byte) 5)
                .build());

        // 初始化测试站点
        testStations = Arrays.asList(
                ChargingStation.builder().id(1L).name("A站点").build(),
                ChargingStation.builder().id(2L).name("B站点").build(),
                ChargingStation.builder().id(3L).name("C站点").build()
        );
    }

    // ==================== getDashboardData 测试 ====================

    @Test
    @DisplayName("获取仪表板数据 - 成功")
    void getDashboardData_Success() {
        // Arrange
        Map<String, Object> orderStats = new HashMap<>();
        orderStats.put("todayChargeAmount", BigDecimal.valueOf(1500.25));
        orderStats.put("todayIncome", BigDecimal.valueOf(1800.50));
        orderStats.put("todayOrderCount", 45);

        when(chargingPileRepository.findAll()).thenReturn(testPiles);
        when(jdbcTemplate.queryForMap(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(orderStats);
        when(chargingStationRepository.count()).thenReturn(3L);
        when(faultRecordRepository.countGlobalByRepairStatus((byte) 0)).thenReturn(2L);
        when(faultRecordRepository.findUrgentPendingFaults()).thenReturn(new ArrayList<>());

        // Act
        DashboardVO dashboard = statisticsService.getDashboardData();

        // Assert
        assertNotNull(dashboard);
        assertEquals(new BigDecimal("1500.25"), dashboard.getTodayChargeAmount());
        assertEquals(new BigDecimal("1800.50"), dashboard.getTodayIncome());
        assertEquals(45, dashboard.getTodayOrderCount());
        assertEquals(10, dashboard.getOnlinePileCount()); // 5空闲+3充电+2预约
        assertEquals(3, dashboard.getChargingPileCount()); // 3充电中
        assertEquals(2, dashboard.getFaultPileCount()); // 2故障
        assertEquals(13, dashboard.getTotalPileCount()); // 总共13个
        assertEquals(3, dashboard.getTotalStationCount()); // 3个站点
    }

    @Test
    @DisplayName("获取仪表板数据 - 无充电桩")
    void getDashboardData_NoChargers() {
        // Arrange
        Map<String, Object> orderStats = new HashMap<>();
        orderStats.put("todayChargeAmount", BigDecimal.ZERO);
        orderStats.put("todayIncome", BigDecimal.ZERO);
        orderStats.put("todayOrderCount", 0);

        when(chargingPileRepository.findAll()).thenReturn(new ArrayList<>());
        when(jdbcTemplate.queryForMap(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(orderStats);
        when(chargingStationRepository.count()).thenReturn(0L);
        when(faultRecordRepository.countGlobalByRepairStatus((byte) 0)).thenReturn(0L);
        when(faultRecordRepository.findUrgentPendingFaults()).thenReturn(new ArrayList<>());

        // Act
        DashboardVO dashboard = statisticsService.getDashboardData();

        // Assert
        assertEquals(0, dashboard.getTotalPileCount());
        assertEquals(0, dashboard.getOnlinePileCount());
    }

    @Test
    @DisplayName("获取仪表板数据 - 充电桩状态为null")
    void getDashboardData_NullPileStatus() {
        // Arrange
        List<ChargingPile> pilesWithNull = new ArrayList<>();
        pilesWithNull.add(ChargingPile.builder().id(1L).status(null).build()); // 状态为null

        Map<String, Object> orderStats = new HashMap<>();
        orderStats.put("todayChargeAmount", BigDecimal.ZERO);
        orderStats.put("todayIncome", BigDecimal.ZERO);
        orderStats.put("todayOrderCount", 0);

        when(chargingPileRepository.findAll()).thenReturn(pilesWithNull);
        when(jdbcTemplate.queryForMap(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(orderStats);
        when(chargingStationRepository.count()).thenReturn(0L);
        when(faultRecordRepository.countGlobalByRepairStatus((byte) 0)).thenReturn(0L);
        when(faultRecordRepository.findUrgentPendingFaults()).thenReturn(new ArrayList<>());

        // Act
        DashboardVO dashboard = statisticsService.getDashboardData();

        // Assert
        assertEquals(0, dashboard.getOnlinePileCount()); // 跳过null的
    }

    @Test
    @DisplayName("获取仪表板数据 - 紧急故障统计")
    void getDashboardData_UrgentFaults() {
        // Arrange
        List<?> urgentFaults = Arrays.asList(1, 2, 3); // 3个紧急故障

        Map<String, Object> orderStats = new HashMap<>();
        orderStats.put("todayChargeAmount", BigDecimal.ZERO);
        orderStats.put("todayIncome", BigDecimal.ZERO);
        orderStats.put("todayOrderCount", 0);

        when(chargingPileRepository.findAll()).thenReturn(new ArrayList<>());
        when(jdbcTemplate.queryForMap(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(orderStats);
        when(chargingStationRepository.count()).thenReturn(0L);
        when(faultRecordRepository.countGlobalByRepairStatus((byte) 0)).thenReturn(5L);
        when(faultRecordRepository.findUrgentPendingFaults()).thenReturn(urgentFaults);

        // Act
        DashboardVO dashboard = statisticsService.getDashboardData();

        // Assert
        assertEquals(5, dashboard.getPendingFaultCount());
        assertEquals(3, dashboard.getUrgentFaultCount());
    }

    // ==================== getChargeTrend 测试 ====================

    @Test
    @DisplayName("获取充电趋势 - 默认7天")
    void getChargeTrend_DefaultDays() {
        // Arrange
        Map<String, Object> stats1 = new HashMap<>();
        stats1.put("chargeAmount", BigDecimal.valueOf(500.0));
        stats1.put("income", BigDecimal.valueOf(600.0));
        stats1.put("orderCount", 10);

        when(jdbcTemplate.queryForMap(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(stats1);

        // Act
        ChargeTrendVO trend = statisticsService.getChargeTrend(null);

        // Assert
        assertNotNull(trend);
        assertEquals(7, trend.getDates().size());
        assertEquals(7, trend.getChargeAmounts().size());
        assertEquals(7, trend.getIncomes().size());
        assertEquals(7, trend.getOrderCounts().size());
    }

    @Test
    @DisplayName("获取充电趋势 - 指定天数")
    void getChargeTrend_SpecificDays() {
        // Arrange
        Map<String, Object> stats = new HashMap<>();
        stats.put("chargeAmount", BigDecimal.valueOf(300.0));
        stats.put("income", BigDecimal.valueOf(360.0));
        stats.put("orderCount", 5);

        when(jdbcTemplate.queryForMap(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(stats);

        // Act
        ChargeTrendVO trend = statisticsService.getChargeTrend(14);

        // Assert
        assertEquals(14, trend.getDates().size());
        assertEquals(14, trend.getChargeAmounts().size());
    }

    @Test
    @DisplayName("获取充电趋势 - 天数无效")
    void getChargeTrend_InvalidDays() {
        // Arrange
        Map<String, Object> stats = new HashMap<>();
        stats.put("chargeAmount", BigDecimal.ZERO);
        stats.put("income", BigDecimal.ZERO);
        stats.put("orderCount", 0);

        when(jdbcTemplate.queryForMap(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(stats);

        // Act
        ChargeTrendVO trend = statisticsService.getChargeTrend(0); // 无效值应该使用默认7天

        // Assert
        assertEquals(7, trend.getDates().size());
    }

    @Test
    @DisplayName("获取充电趋势 - 查询异常处理")
    void getChargeTrend_QueryException() {
        // Arrange
        when(jdbcTemplate.queryForMap(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        ChargeTrendVO trend = statisticsService.getChargeTrend(7);

        // Assert
        assertNotNull(trend);
        assertEquals(7, trend.getDates().size());
        // 所有数据应该是零
        assertTrue(trend.getChargeAmounts().stream().allMatch(a -> a.equals(BigDecimal.ZERO)));
    }

    @Test
    @DisplayName("获取充电趋势 - 数据验证（日期排序）")
    void getChargeTrend_DateOrdering() {
        // Arrange
        Map<String, Object> stats = new HashMap<>();
        stats.put("chargeAmount", BigDecimal.valueOf(100.0));
        stats.put("income", BigDecimal.valueOf(120.0));
        stats.put("orderCount", 2);

        when(jdbcTemplate.queryForMap(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(stats);

        // Act
        ChargeTrendVO trend = statisticsService.getChargeTrend(3);

        // Assert
        assertEquals(3, trend.getDates().size());
        // 验证日期是否递增
        for (int i = 0; i < trend.getDates().size() - 1; i++) {
            assertTrue(trend.getDates().get(i).compareTo(trend.getDates().get(i + 1)) < 0);
        }
    }

    // ==================== getPileStatusDistribution 测试 ====================

    @Test
    @DisplayName("获取充电桩状态分布 - 成功")
    void getPileStatusDistribution_Success() {
        // Arrange
        when(chargingPileRepository.findAll()).thenReturn(testPiles);

        // Act
        PileStatusDistributionVO distribution = statisticsService.getPileStatusDistribution();

        // Assert
        assertNotNull(distribution);
        assertEquals(5, distribution.getAvailableCount()); // 状态1
        assertEquals(3, distribution.getChargingCount()); // 状态2
        assertEquals(2, distribution.getReservedCount()); // 状态3
        assertEquals(2, distribution.getFaultCount()); // 状态4
        assertEquals(1, distribution.getOfflineCount()); // 状态5
    }

    @Test
    @DisplayName("获取充电桩状态分布 - 无充电桩")
    void getPileStatusDistribution_NoPiles() {
        // Arrange
        when(chargingPileRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        PileStatusDistributionVO distribution = statisticsService.getPileStatusDistribution();

        // Assert
        assertEquals(0, distribution.getAvailableCount());
        assertEquals(0, distribution.getChargingCount());
        assertEquals(0, distribution.getReservedCount());
        assertEquals(0, distribution.getFaultCount());
        assertEquals(0, distribution.getOfflineCount());
    }

    @Test
    @DisplayName("获取充电桩状态分布 - 忽略null状态")
    void getPileStatusDistribution_IgnoreNullStatus() {
        // Arrange
        List<ChargingPile> pilesWithNull = new ArrayList<>();
        pilesWithNull.add(ChargingPile.builder().id(1L).status(null).build());
        pilesWithNull.add(ChargingPile.builder().id(2L).status((byte) 1).build());

        when(chargingPileRepository.findAll()).thenReturn(pilesWithNull);

        // Act
        PileStatusDistributionVO distribution = statisticsService.getPileStatusDistribution();

        // Assert
        assertEquals(1, distribution.getAvailableCount());
        assertEquals(0, distribution.getChargingCount());
    }

    // ==================== getStationRanking 测试 ====================

    @Test
    @DisplayName("获取站点排行 - 成功")
    void getStationRanking_Success() {
        // Arrange
        List<Map<String, Object>> rankingResults = new ArrayList<>();

        Map<String, Object> station1 = new HashMap<>();
        station1.put("stationId", 1L);
        station1.put("stationName", "A站点");
        station1.put("totalIncome", BigDecimal.valueOf(5000.00));
        station1.put("totalChargeAmount", BigDecimal.valueOf(4000.0));
        station1.put("totalOrderCount", 100);

        Map<String, Object> station2 = new HashMap<>();
        station2.put("stationId", 2L);
        station2.put("stationName", "B站点");
        station2.put("totalIncome", BigDecimal.valueOf(3000.00));
        station2.put("totalChargeAmount", BigDecimal.valueOf(2500.0));
        station2.put("totalOrderCount", 60);

        rankingResults.add(station1);
        rankingResults.add(station2);

        when(jdbcTemplate.queryForList(anyString(), eq(10)))
                .thenReturn(rankingResults);

        // Act
        List<StationRankingVO> ranking = statisticsService.getStationRanking(null);

        // Assert
        assertNotNull(ranking);
        assertEquals(2, ranking.size());
        assertEquals("A站点", ranking.get(0).getStationName());
        assertEquals(new BigDecimal("5000.00"), ranking.get(0).getTotalIncome());
        assertEquals(100, ranking.get(0).getTotalOrderCount());
    }

    @Test
    @DisplayName("获取站点排行 - 自定义limit")
    void getStationRanking_CustomLimit() {
        // Arrange
        when(jdbcTemplate.queryForList(anyString(), eq(5)))
                .thenReturn(new ArrayList<>());

        // Act
        statisticsService.getStationRanking(5);

        // Assert
        verify(jdbcTemplate, times(1)).queryForList(anyString(), eq(5));
    }

    @Test
    @DisplayName("获取站点排行 - 无效limit")
    void getStationRanking_InvalidLimit() {
        // Arrange
        when(jdbcTemplate.queryForList(anyString(), eq(10)))
                .thenReturn(new ArrayList<>());

        // Act
        statisticsService.getStationRanking(0); // 无效值应该使用默认10

        // Assert
        verify(jdbcTemplate, times(1)).queryForList(anyString(), eq(10));
    }

    @Test
    @DisplayName("获取站点排行 - 查询异常处理")
    void getStationRanking_QueryException() {
        // Arrange
        when(jdbcTemplate.queryForList(anyString(), anyInt()))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        List<StationRankingVO> ranking = statisticsService.getStationRanking(10);

        // Assert
        assertNotNull(ranking);
        assertTrue(ranking.isEmpty());
    }

    @Test
    @DisplayName("获取站点排行 - Top 10验证")
    void getStationRanking_TopN() {
        // Arrange
        List<Map<String, Object>> results = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> station = new HashMap<>();
            station.put("stationId", (long) i);
            station.put("stationName", "站点" + i);
            station.put("totalIncome", BigDecimal.valueOf(5000 - i * 100));
            station.put("totalChargeAmount", BigDecimal.valueOf(4000.0));
            station.put("totalOrderCount", 100 - i * 5);
            results.add(station);
        }

        when(jdbcTemplate.queryForList(anyString(), eq(10)))
                .thenReturn(results);

        // Act
        List<StationRankingVO> ranking = statisticsService.getStationRanking(10);

        // Assert
        assertEquals(10, ranking.size());
        // 验证是否按收入降序
        for (int i = 0; i < ranking.size() - 1; i++) {
            assertTrue(ranking.get(i).getTotalIncome().compareTo(ranking.get(i + 1).getTotalIncome()) >= 0);
        }
    }

    // ==================== getOrderStatistics 测试 ====================

    @Test
    @DisplayName("获取订单统计 - 成功")
    void getOrderStatistics_Success() {
        // Arrange
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", 150);
        stats.put("completedCount", 120);
        stats.put("inProgressCount", 20);
        stats.put("cancelledCount", 10);
        stats.put("totalChargeAmount", BigDecimal.valueOf(5000.0));
        stats.put("totalRevenue", BigDecimal.valueOf(6000.0));

        when(jdbcTemplate.queryForMap(anyString(), eq("2025-01-01"), eq("2025-01-31")))
                .thenReturn(stats);

        // Act
        Map<String, Object> result = statisticsService.getOrderStatistics("2025-01-01", "2025-01-31");

        // Assert
        assertNotNull(result);
        assertEquals(150, result.get("totalCount"));
        assertEquals(120, result.get("completedCount"));
        assertEquals(20, result.get("inProgressCount"));
        assertEquals(10, result.get("cancelledCount"));
    }

    @Test
    @DisplayName("获取订单统计 - 查询异常处理")
    void getOrderStatistics_QueryException() {
        // Arrange
        when(jdbcTemplate.queryForMap(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        Map<String, Object> result = statisticsService.getOrderStatistics("2025-01-01", "2025-01-31");

        // Assert
        assertNotNull(result);
        assertEquals(0, result.get("totalCount"));
        assertEquals(BigDecimal.ZERO, result.get("totalRevenue"));
    }

    // ==================== getRevenueStatistics 测试 ====================

    @Test
    @DisplayName("获取收入统计 - 周期统计")
    void getRevenueStatistics_WeekPeriod() {
        // Arrange
        List<Map<String, Object>> queryResults = new ArrayList<>();
        Map<String, Object> dayData = new HashMap<>();
        dayData.put("dateKey", "2025-01-10");
        dayData.put("revenue", BigDecimal.valueOf(1000.0));
        queryResults.add(dayData);

        when(jdbcTemplate.queryForList(anyString(), eq("%Y-%m-%d"), eq(7)))
                .thenReturn(queryResults);

        // Act
        Map<String, Object> result = statisticsService.getRevenueStatistics("week");

        // Assert
        assertNotNull(result);
        assertNotNull(result.get("dates"));
        assertNotNull(result.get("revenues"));
        List<?> dates = (List<?>) result.get("dates");
        List<?> revenues = (List<?>) result.get("revenues");
        assertEquals(7, dates.size());
        assertEquals(7, revenues.size());
    }

    @Test
    @DisplayName("获取收入统计 - 月度统计")
    void getRevenueStatistics_MonthPeriod() {
        // Arrange
        when(jdbcTemplate.queryForList(anyString(), eq("%Y-%m-%d"), eq(30)))
                .thenReturn(new ArrayList<>());

        // Act
        Map<String, Object> result = statisticsService.getRevenueStatistics("month");

        // Assert
        List<?> dates = (List<?>) result.get("dates");
        assertEquals(30, dates.size());
    }

    @Test
    @DisplayName("获取收入统计 - 无效周期（使用默认）")
    void getRevenueStatistics_InvalidPeriod() {
        // Arrange
        when(jdbcTemplate.queryForList(anyString(), eq("%Y-%m-%d"), eq(7)))
                .thenReturn(new ArrayList<>());

        // Act
        Map<String, Object> result = statisticsService.getRevenueStatistics("invalid");

        // Assert
        List<?> dates = (List<?>) result.get("dates");
        assertEquals(7, dates.size()); // 应该使用默认7天
    }

    @Test
    @DisplayName("获取收入统计 - 异常处理")
    void getRevenueStatistics_Exception() {
        // Arrange
        when(jdbcTemplate.queryForList(anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        Map<String, Object> result = statisticsService.getRevenueStatistics("week");

        // Assert
        assertNotNull(result);
        List<?> dates = (List<?>) result.get("dates");
        List<?> revenues = (List<?>) result.get("revenues");
        assertEquals(7, dates.size());
        assertEquals(7, revenues.size());
        // 所有收入应该是零
        assertTrue(revenues.stream().allMatch(r -> BigDecimal.ZERO.equals(r)));
    }
}
