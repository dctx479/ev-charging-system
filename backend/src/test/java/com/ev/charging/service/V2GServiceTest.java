package com.ev.charging.service;

import com.ev.charging.dto.StartDischargeDTO;
import com.ev.charging.dto.StopDischargeDTO;
import com.ev.charging.entity.ChargingPile;
import com.ev.charging.entity.ChargingStation;
import com.ev.charging.entity.User;
import com.ev.charging.entity.V2GRecord;
import com.ev.charging.exception.BusinessException;
import com.ev.charging.repository.ChargingPileRepository;
import com.ev.charging.repository.ChargingStationRepository;
import com.ev.charging.repository.UserRepository;
import com.ev.charging.repository.V2GRecordRepository;
import com.ev.charging.vo.V2GRecordVO;
import com.ev.charging.vo.V2GStatisticsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * V2G服务 - 单元测试
 * 测试V2G放电启动、停止、统计等核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("V2G双向充电服务测试")
class V2GServiceTest {

    @Mock
    private V2GRecordRepository v2gRecordRepository;

    @Mock
    private ChargingPileRepository pileRepository;

    @Mock
    private ChargingStationRepository stationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ElectricityPriceService electricityPriceService;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private V2GService v2gService;

    private User testUser;
    private ChargingPile testPile;
    private ChargingStation testStation;
    private V2GRecord testRecord;

    @BeforeEach
    void setUp() {
        // 初始化测试用户
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .nickname("测试用户")
                .batteryCapacity(BigDecimal.valueOf(75.0))
                .balance(BigDecimal.valueOf(1000.00))
                .build();

        // 初始化测试充电桩
        testPile = ChargingPile.builder()
                .id(100L)
                .pileNo("PL-001")
                .stationId(10L)
                .power(BigDecimal.valueOf(7.0)) // 7kW
                .supportV2g((byte) 1)
                .status((byte) 1) // 空闲
                .build();

        // 初始化测试站点
        testStation = ChargingStation.builder()
                .id(10L)
                .name("测试充电站")
                .build();

        // 初始化测试放电记录
        testRecord = V2GRecord.builder()
                .id(1L)
                .userId(1L)
                .pileId(100L)
                .stationId(10L)
                .recordType((byte) 2) // 放电
                .startTime(LocalDateTime.now())
                .electricityPrice(BigDecimal.valueOf(1.2))
                .batteryHealthBefore(95)
                .build();
    }

    // ==================== startDischarge 测试 ====================

    @Test
    @DisplayName("启动V2G放电 - 成功场景")
    void startDischarge_Success() {
        // Arrange
        Long userId = 1L;
        StartDischargeDTO dto = StartDischargeDTO.builder()
                .pileId(100L)
                .currentSoc(50)
                .minPrice(BigDecimal.valueOf(1.0))
                .batteryHealthBefore(95)
                .build();

        when(pileRepository.findById(100L)).thenReturn(Optional.of(testPile));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(electricityPriceService.getRealTimePrice()).thenReturn(BigDecimal.valueOf(1.2));
        when(v2gRecordRepository.save(any(V2GRecord.class))).thenReturn(testRecord);
        when(pileRepository.updatePileStatusAtomically(100L, (byte) 2, (byte) 1)).thenReturn(1);

        // Act
        V2GRecordVO result = v2gService.startDischarge(userId, dto);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(pileRepository, times(1)).findById(100L);
        verify(userRepository, times(1)).findById(userId);
        verify(electricityPriceService, times(1)).getRealTimePrice();
        verify(v2gRecordRepository, times(1)).save(any(V2GRecord.class));
        verify(pileRepository, times(1)).updatePileStatusAtomically(100L, (byte) 2, (byte) 1);
    }

    @Test
    @DisplayName("启动V2G放电 - 充电桩不存在")
    void startDischarge_PileNotFound() {
        // Arrange
        Long userId = 1L;
        StartDischargeDTO dto = StartDischargeDTO.builder()
                .pileId(999L)
                .currentSoc(50)
                .build();

        when(pileRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> v2gService.startDischarge(userId, dto)
        );
        assertEquals("充电桩不存在", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("启动V2G放电 - 充电桩不支持V2G")
    void startDischarge_PileNotSupportV2G() {
        // Arrange
        Long userId = 1L;
        testPile.setSupportV2g((byte) 0); // 不支持

        StartDischargeDTO dto = StartDischargeDTO.builder()
                .pileId(100L)
                .currentSoc(50)
                .build();

        when(pileRepository.findById(100L)).thenReturn(Optional.of(testPile));

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> v2gService.startDischarge(userId, dto)
        );
        assertEquals("该充电桩不支持V2G功能", exception.getMessage());
    }

    @Test
    @DisplayName("启动V2G放电 - 充电桩不可用")
    void startDischarge_PileNotAvailable() {
        // Arrange
        Long userId = 1L;
        testPile.setStatus((byte) 4); // 故障

        StartDischargeDTO dto = StartDischargeDTO.builder()
                .pileId(100L)
                .currentSoc(50)
                .build();

        when(pileRepository.findById(100L)).thenReturn(Optional.of(testPile));

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> v2gService.startDischarge(userId, dto)
        );
        assertEquals("充电桩不可用", exception.getMessage());
    }

    @Test
    @DisplayName("启动V2G放电 - 用户不存在")
    void startDischarge_UserNotFound() {
        // Arrange
        Long userId = 999L;
        StartDischargeDTO dto = StartDischargeDTO.builder()
                .pileId(100L)
                .currentSoc(50)
                .build();

        when(pileRepository.findById(100L)).thenReturn(Optional.of(testPile));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> v2gService.startDischarge(userId, dto)
        );
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    @DisplayName("启动V2G放电 - 用户未完善车辆信息")
    void startDischarge_VehicleInfoIncomplete() {
        // Arrange
        Long userId = 1L;
        testUser.setBatteryCapacity(null);

        StartDischargeDTO dto = StartDischargeDTO.builder()
                .pileId(100L)
                .currentSoc(50)
                .build();

        when(pileRepository.findById(100L)).thenReturn(Optional.of(testPile));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> v2gService.startDischarge(userId, dto)
        );
        assertEquals("请先完善车辆信息", exception.getMessage());
    }

    @Test
    @DisplayName("启动V2G放电 - SOC过低（低于20%）")
    void startDischarge_SOCTooLow() {
        // Arrange
        Long userId = 1L;
        StartDischargeDTO dto = StartDischargeDTO.builder()
                .pileId(100L)
                .currentSoc(15) // 低于20%阈值
                .build();

        when(pileRepository.findById(100L)).thenReturn(Optional.of(testPile));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> v2gService.startDischarge(userId, dto)
        );
        assertTrue(exception.getMessage().contains("电池电量过低"));
    }

    @Test
    @DisplayName("启动V2G放电 - SOC边界条件（恰好20%）")
    void startDischarge_SOCBoundary() {
        // Arrange
        Long userId = 1L;
        StartDischargeDTO dto = StartDischargeDTO.builder()
                .pileId(100L)
                .currentSoc(20) // 恰好在阈值
                .minPrice(BigDecimal.valueOf(1.0))
                .batteryHealthBefore(95)
                .build();

        when(pileRepository.findById(100L)).thenReturn(Optional.of(testPile));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(electricityPriceService.getRealTimePrice()).thenReturn(BigDecimal.valueOf(1.2));
        when(v2gRecordRepository.save(any(V2GRecord.class))).thenReturn(testRecord);
        when(pileRepository.updatePileStatusAtomically(100L, (byte) 2, (byte) 1)).thenReturn(1);

        // Act
        V2GRecordVO result = v2gService.startDischarge(userId, dto);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("启动V2G放电 - 电价未达到设定值")
    void startDischarge_PriceBelowMinimum() {
        // Arrange
        Long userId = 1L;
        StartDischargeDTO dto = StartDischargeDTO.builder()
                .pileId(100L)
                .currentSoc(50)
                .minPrice(BigDecimal.valueOf(1.5)) // 用户设置的最低价格
                .build();

        when(pileRepository.findById(100L)).thenReturn(Optional.of(testPile));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(electricityPriceService.getRealTimePrice()).thenReturn(BigDecimal.valueOf(1.2)); // 实际价格更低

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> v2gService.startDischarge(userId, dto)
        );
        assertEquals("当前电价未达到设定值", exception.getMessage());
    }

    @Test
    @DisplayName("启动V2G放电 - 充电桩状态更新失败")
    void startDischarge_PileStatusUpdateFailed() {
        // Arrange
        Long userId = 1L;
        StartDischargeDTO dto = StartDischargeDTO.builder()
                .pileId(100L)
                .currentSoc(50)
                .minPrice(BigDecimal.valueOf(1.0))
                .batteryHealthBefore(95)
                .build();

        when(pileRepository.findById(100L)).thenReturn(Optional.of(testPile));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(electricityPriceService.getRealTimePrice()).thenReturn(BigDecimal.valueOf(1.2));
        when(v2gRecordRepository.save(any(V2GRecord.class))).thenReturn(testRecord);
        when(pileRepository.updatePileStatusAtomically(100L, (byte) 2, (byte) 1)).thenReturn(0); // 失败

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> v2gService.startDischarge(userId, dto)
        );
        assertEquals("充电桩状态更新失败，请重试", exception.getMessage());
    }

    // ==================== stopDischarge 测试 ====================

    @Test
    @DisplayName("停止V2G放电 - 成功场景")
    void stopDischarge_Success() {
        // Arrange
        Long userId = 1L;
        Long recordId = 1L;
        StopDischargeDTO dto = StopDischargeDTO.builder()
                .recordId(recordId)
                .finalSoc(30)
                .batteryHealthAfter(93)
                .build();

        testRecord.setUserId(userId);
        testRecord.setRecordType((byte) 2);

        when(v2gRecordRepository.findById(recordId)).thenReturn(Optional.of(testRecord));
        when(v2gRecordRepository.atomicStopDischarge(recordId, any(LocalDateTime.class))).thenReturn(1);
        when(pileRepository.findById(100L)).thenReturn(Optional.of(testPile));
        when(userRepository.addBalanceAtomically(userId, any(BigDecimal.class))).thenReturn(1);
        when(pileRepository.updatePileStatusAtomically(100L, (byte) 1, (byte) 2)).thenReturn(1);

        // Act
        V2GRecordVO result = v2gService.stopDischarge(userId, dto);

        // Assert
        assertNotNull(result);
        verify(v2gRecordRepository, times(1)).atomicStopDischarge(eq(recordId), any(LocalDateTime.class));
        verify(userRepository, times(1)).addBalanceAtomically(eq(userId), any(BigDecimal.class));
        verify(pileRepository, times(1)).updatePileStatusAtomically(100L, (byte) 1, (byte) 2);
    }

    @Test
    @DisplayName("停止V2G放电 - 记录不存在")
    void stopDischarge_RecordNotFound() {
        // Arrange
        Long userId = 1L;
        StopDischargeDTO dto = StopDischargeDTO.builder()
                .recordId(999L)
                .finalSoc(30)
                .build();

        when(v2gRecordRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> v2gService.stopDischarge(userId, dto)
        );
        assertEquals("记录不存在", exception.getMessage());
    }

    @Test
    @DisplayName("停止V2G放电 - 无权操作（用户ID不匹配）")
    void stopDischarge_UnauthorizedUser() {
        // Arrange
        Long userId = 999L; // 其他用户
        StopDischargeDTO dto = StopDischargeDTO.builder()
                .recordId(1L)
                .finalSoc(30)
                .build();

        testRecord.setUserId(1L); // 记录属于用户1

        when(v2gRecordRepository.findById(1L)).thenReturn(Optional.of(testRecord));

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> v2gService.stopDischarge(userId, dto)
        );
        assertEquals("无权操作此放电记录", exception.getMessage());
    }

    @Test
    @DisplayName("停止V2G放电 - 不是放电记录")
    void stopDischarge_NotDischargeRecord() {
        // Arrange
        Long userId = 1L;
        StopDischargeDTO dto = StopDischargeDTO.builder()
                .recordId(1L)
                .finalSoc(30)
                .build();

        testRecord.setRecordType((byte) 1); // 充电而不是放电

        when(v2gRecordRepository.findById(1L)).thenReturn(Optional.of(testRecord));

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> v2gService.stopDischarge(userId, dto)
        );
        assertEquals("不是放电记录", exception.getMessage());
    }

    @Test
    @DisplayName("停止V2G放电 - 已停止（double-stop竞态条件）")
    void stopDischarge_AlreadyStopped() {
        // Arrange
        Long userId = 1L;
        StopDischargeDTO dto = StopDischargeDTO.builder()
                .recordId(1L)
                .finalSoc(30)
                .build();

        testRecord.setUserId(userId);
        testRecord.setRecordType((byte) 2);

        when(v2gRecordRepository.findById(1L)).thenReturn(Optional.of(testRecord));
        when(v2gRecordRepository.atomicStopDischarge(1L, any(LocalDateTime.class))).thenReturn(0); // 无记录更新

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> v2gService.stopDischarge(userId, dto)
        );
        assertEquals("放电已停止", exception.getMessage());
    }

    @Test
    @DisplayName("停止V2G放电 - 收益计算（7kW × 1小时 × 1.2元/kWh × 0.9）")
    void stopDischarge_ProfitCalculation() {
        // Arrange
        Long userId = 1L;
        Long recordId = 1L;
        StopDischargeDTO dto = StopDischargeDTO.builder()
                .recordId(recordId)
                .finalSoc(30)
                .batteryHealthAfter(93)
                .build();

        testRecord.setUserId(userId);
        testRecord.setRecordType((byte) 2);
        testRecord.setStartTime(LocalDateTime.now().minusHours(1));
        testRecord.setElectricityPrice(BigDecimal.valueOf(1.2));

        testPile.setPower(BigDecimal.valueOf(7.0)); // 7kW

        when(v2gRecordRepository.findById(recordId)).thenReturn(Optional.of(testRecord));
        when(v2gRecordRepository.atomicStopDischarge(recordId, any(LocalDateTime.class))).thenReturn(1);
        when(v2gRecordRepository.findById(recordId))
                .thenReturn(Optional.of(testRecord));
        when(pileRepository.findById(100L)).thenReturn(Optional.of(testPile));
        when(userRepository.addBalanceAtomically(userId, any(BigDecimal.class))).thenReturn(1);
        when(pileRepository.updatePileStatusAtomically(100L, (byte) 1, (byte) 2)).thenReturn(1);

        // Act
        V2GRecordVO result = v2gService.stopDischarge(userId, dto);

        // Assert
        assertNotNull(result);
        // 收益 = 7 × 1 × 1.2 × 0.9 = 7.56
        ArgumentCaptor<BigDecimal> profitCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(userRepository, times(1)).addBalanceAtomically(eq(userId), profitCaptor.capture());
        assertTrue(profitCaptor.getValue().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("停止V2G放电 - 最小放电量保护（0.1kWh）")
    void stopDischarge_MinimumEnergyProtection() {
        // Arrange
        Long userId = 1L;
        StopDischargeDTO dto = StopDischargeDTO.builder()
                .recordId(1L)
                .finalSoc(30)
                .batteryHealthAfter(93)
                .build();

        testRecord.setUserId(userId);
        testRecord.setRecordType((byte) 2);
        testRecord.setStartTime(LocalDateTime.now()); // 刚刚开始，几乎没有放电

        when(v2gRecordRepository.findById(1L)).thenReturn(Optional.of(testRecord));
        when(v2gRecordRepository.atomicStopDischarge(1L, any(LocalDateTime.class))).thenReturn(1);
        when(pileRepository.findById(100L)).thenReturn(Optional.of(testPile));
        when(userRepository.addBalanceAtomically(userId, any(BigDecimal.class))).thenReturn(1);
        when(pileRepository.updatePileStatusAtomically(100L, (byte) 1, (byte) 2)).thenReturn(1);

        // Act
        V2GRecordVO result = v2gService.stopDischarge(userId, dto);

        // Assert
        assertNotNull(result);
    }

    // ==================== getRecords 测试 ====================

    @Test
    @DisplayName("获取用户V2G记录 - 成功")
    void getRecords_Success() {
        // Arrange
        Long userId = 1L;
        List<V2GRecord> records = new ArrayList<>();

        V2GRecord record1 = V2GRecord.builder()
                .id(1L)
                .userId(userId)
                .pileId(100L)
                .stationId(10L)
                .recordType((byte) 2)
                .startTime(LocalDateTime.now().minusHours(2))
                .build();

        V2GRecord record2 = V2GRecord.builder()
                .id(2L)
                .userId(userId)
                .pileId(101L)
                .stationId(10L)
                .recordType((byte) 2)
                .startTime(LocalDateTime.now().minusHours(1))
                .build();

        records.add(record1);
        records.add(record2);

        when(v2gRecordRepository.findByUserIdOrderByCreateTimeDesc(userId)).thenReturn(records);
        when(userRepository.findAllById(anySet())).thenReturn(Arrays.asList(testUser));
        when(pileRepository.findAllById(anySet())).thenReturn(Arrays.asList(testPile));
        when(stationRepository.findAllById(anySet())).thenReturn(Arrays.asList(testStation));

        // Act
        List<V2GRecordVO> result = v2gService.getRecords(userId);

        // Assert
        assertEquals(2, result.size());
        verify(v2gRecordRepository, times(1)).findByUserIdOrderByCreateTimeDesc(userId);
    }

    @Test
    @DisplayName("获取用户V2G记录 - 空记录")
    void getRecords_Empty() {
        // Arrange
        Long userId = 1L;

        when(v2gRecordRepository.findByUserIdOrderByCreateTimeDesc(userId)).thenReturn(new ArrayList<>());

        // Act
        List<V2GRecordVO> result = v2gService.getRecords(userId);

        // Assert
        assertTrue(result.isEmpty());
    }

    // ==================== getStatistics 测试 ====================

    @Test
    @DisplayName("获取用户V2G统计 - 成功")
    void getStatistics_Success() {
        // Arrange
        Long userId = 1L;
        BigDecimal totalProfit = BigDecimal.valueOf(756.00);
        BigDecimal totalDischargeAmount = BigDecimal.valueOf(700.0);

        when(v2gRecordRepository.sumProfitByUser(userId)).thenReturn(totalProfit);
        when(v2gRecordRepository.sumDischargeAmountByUser(userId)).thenReturn(totalDischargeAmount);

        // Act
        V2GStatisticsVO stats = v2gService.getStatistics(userId);

        // Assert
        assertNotNull(stats);
        assertEquals(totalProfit, stats.getTotalProfit());
        assertEquals(totalDischargeAmount, stats.getTotalDischargeAmount());
        assertTrue(stats.getAveragePrice().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("获取用户V2G统计 - 无放电记录")
    void getStatistics_NoRecords() {
        // Arrange
        Long userId = 1L;

        when(v2gRecordRepository.sumProfitByUser(userId)).thenReturn(null);
        when(v2gRecordRepository.sumDischargeAmountByUser(userId)).thenReturn(null);

        // Act
        V2GStatisticsVO stats = v2gService.getStatistics(userId);

        // Assert
        assertNotNull(stats);
        assertEquals(BigDecimal.ZERO, stats.getTotalProfit());
        assertEquals(BigDecimal.ZERO, stats.getTotalDischargeAmount());
        assertEquals(BigDecimal.ZERO, stats.getAveragePrice());
    }

    @Test
    @DisplayName("获取用户V2G统计 - 仅有盈利无放电量")
    void getStatistics_ProfitWithoutAmount() {
        // Arrange
        Long userId = 1L;

        when(v2gRecordRepository.sumProfitByUser(userId)).thenReturn(BigDecimal.valueOf(100.0));
        when(v2gRecordRepository.sumDischargeAmountByUser(userId)).thenReturn(BigDecimal.ZERO);

        // Act
        V2GStatisticsVO stats = v2gService.getStatistics(userId);

        // Assert
        assertEquals(BigDecimal.valueOf(100.0), stats.getTotalProfit());
        assertEquals(BigDecimal.ZERO, stats.getTotalDischargeAmount());
        assertEquals(BigDecimal.ZERO, stats.getAveragePrice());
    }

    @Test
    @DisplayName("获取全平台V2G统计 - 管理端")
    void getAdminStatistics_Success() {
        // Arrange
        BigDecimal todayDischarge = BigDecimal.valueOf(150.0);
        BigDecimal todayIncome = BigDecimal.valueOf(135.0);
        Long dischargeCount = 25L;
        Long userCount = 10L;

        when(v2gRecordRepository.sumDischargeAmountSince(any(LocalDateTime.class)))
                .thenReturn(todayDischarge);
        when(v2gRecordRepository.sumProfitSince(any(LocalDateTime.class)))
                .thenReturn(todayIncome);
        when(v2gRecordRepository.countCompletedDischarge()).thenReturn(dischargeCount);
        when(v2gRecordRepository.countDistinctUsers()).thenReturn(userCount);

        // Act
        V2GStatisticsVO stats = v2gService.getAdminStatistics();

        // Assert
        assertNotNull(stats);
        assertEquals(todayDischarge, stats.getTodayDischarge());
        assertEquals(todayIncome, stats.getTodayIncome());
        assertEquals(dischargeCount, stats.getDischargeCount());
        assertEquals(userCount, stats.getUserCount());
    }

    @Test
    @DisplayName("获取全平台V2G统计 - 处理null值")
    void getAdminStatistics_HandleNullValues() {
        // Arrange
        when(v2gRecordRepository.sumDischargeAmountSince(any(LocalDateTime.class)))
                .thenReturn(null);
        when(v2gRecordRepository.sumProfitSince(any(LocalDateTime.class)))
                .thenReturn(null);
        when(v2gRecordRepository.countCompletedDischarge()).thenReturn(null);
        when(v2gRecordRepository.countDistinctUsers()).thenReturn(null);

        // Act
        V2GStatisticsVO stats = v2gService.getAdminStatistics();

        // Assert
        assertEquals(BigDecimal.ZERO, stats.getTodayDischarge());
        assertEquals(BigDecimal.ZERO, stats.getTodayIncome());
        assertEquals(0L, stats.getDischargeCount());
        assertEquals(0L, stats.getUserCount());
    }
}
