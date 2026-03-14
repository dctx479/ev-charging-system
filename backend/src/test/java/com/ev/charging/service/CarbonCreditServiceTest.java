package com.ev.charging.service;

import com.ev.charging.entity.CarbonCreditRecord;
import com.ev.charging.entity.User;
import com.ev.charging.exception.BusinessException;
import com.ev.charging.repository.CarbonCreditRecordRepository;
import com.ev.charging.repository.UserRepository;
import com.ev.charging.vo.CreditRecordVO;
import com.ev.charging.vo.CreditStatisticsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 碳积分服务 - 单元测试
 * 测试积分增加、兑换、查询等核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("碳积分服务测试")
class CarbonCreditServiceTest {

    @Mock
    private CarbonCreditRecordRepository creditRecordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CarbonCreditService carbonCreditService;

    private User testUser;
    private CarbonCreditRecord testRecord;

    @BeforeEach
    void setUp() {
        // 初始化Redis mock
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 初始化测试用户
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .nickname("测试用户")
                .carbonCredits(100)
                .batteryCapacity(BigDecimal.valueOf(75.0))
                .build();

        // 初始化测试积分记录
        testRecord = CarbonCreditRecord.builder()
                .id(1L)
                .userId(1L)
                .orderId(100L)
                .changeAmount(78)
                .changeType((byte) 1)
                .description("充电10kWh，获得碳积分")
                .balanceAfter(178)
                .build();
    }

    // ==================== addCreditsForCharging 测试 ====================

    @Test
    @DisplayName("充电积分添加 - 成功场景")
    void addCreditsForCharging_Success() {
        // Arrange
        Long orderId = 100L;
        Long userId = 1L;
        BigDecimal chargeAmount = BigDecimal.valueOf(10.0);

        when(creditRecordRepository.existsByOrderId(orderId)).thenReturn(false);
        when(userRepository.addCarbonCreditsAtomically(userId, 78)).thenReturn(1);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(creditRecordRepository.save(any(CarbonCreditRecord.class))).thenReturn(testRecord);

        // Act
        Integer credits = carbonCreditService.addCreditsForCharging(orderId, userId, chargeAmount);

        // Assert
        assertEquals(78, credits);
        verify(creditRecordRepository, times(1)).existsByOrderId(orderId);
        verify(userRepository, times(1)).addCarbonCreditsAtomically(userId, 78);
        verify(creditRecordRepository, times(1)).save(any(CarbonCreditRecord.class));
    }

    @Test
    @DisplayName("充电积分添加 - 重复订单（幂等防护）")
    void addCreditsForCharging_DuplicateOrder() {
        // Arrange
        Long orderId = 100L;
        Long userId = 1L;
        BigDecimal chargeAmount = BigDecimal.valueOf(10.0);

        when(creditRecordRepository.existsByOrderId(orderId)).thenReturn(true);

        // Act
        Integer credits = carbonCreditService.addCreditsForCharging(orderId, userId, chargeAmount);

        // Assert
        assertEquals(0, credits);
        verify(creditRecordRepository, times(1)).existsByOrderId(orderId);
        verify(userRepository, never()).addCarbonCreditsAtomically(anyLong(), anyInt());
        verify(creditRecordRepository, never()).save(any(CarbonCreditRecord.class));
    }

    @Test
    @DisplayName("充电积分添加 - 数据库唯一约束冲突（并发场景）")
    void addCreditsForCharging_DataIntegrityViolation() {
        // Arrange
        Long orderId = 100L;
        Long userId = 1L;
        BigDecimal chargeAmount = BigDecimal.valueOf(10.0);

        when(creditRecordRepository.existsByOrderId(orderId)).thenReturn(false);
        when(userRepository.addCarbonCreditsAtomically(userId, 78))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        // Act
        Integer credits = carbonCreditService.addCreditsForCharging(orderId, userId, chargeAmount);

        // Assert
        assertEquals(0, credits);
        verify(creditRecordRepository, times(1)).existsByOrderId(orderId);
    }

    @Test
    @DisplayName("充电积分添加 - 计算公式验证（75kWh充电）")
    void addCreditsForCharging_CalculationFormula() {
        // Arrange: 积分 = 充电量(kWh) × 碳排放因子(0.785) × 10 = 75 × 0.785 × 10 = 589
        Long orderId = 101L;
        Long userId = 1L;
        BigDecimal chargeAmount = BigDecimal.valueOf(75.0);
        int expectedCredits = 589;

        when(creditRecordRepository.existsByOrderId(orderId)).thenReturn(false);
        when(userRepository.addCarbonCreditsAtomically(userId, expectedCredits)).thenReturn(1);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(creditRecordRepository.save(any(CarbonCreditRecord.class))).thenReturn(testRecord);

        // Act
        Integer credits = carbonCreditService.addCreditsForCharging(orderId, userId, chargeAmount);

        // Assert
        assertEquals(expectedCredits, credits);
    }

    // ==================== dailyCheckIn 测试 ====================

    @Test
    @DisplayName("每日签到 - 成功签到（基础积分）")
    void dailyCheckIn_Success() {
        // Arrange
        Long userId = 1L;
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));

        when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);
        when(creditRecordRepository.existsByUserIdAndChangeTypeAndCreateTimeBetween(
                userId, (byte) 2, any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(false);
        when(creditRecordRepository.findCheckInRecords(userId, (byte) 2)).thenReturn(new ArrayList<>());
        when(userRepository.addCarbonCreditsAtomically(userId, 5)).thenReturn(1);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(creditRecordRepository.save(any(CarbonCreditRecord.class))).thenReturn(testRecord);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // Act
        Integer credits = carbonCreditService.dailyCheckIn(userId);

        // Assert
        assertEquals(5, credits); // 基础签到积分
        verify(valueOperations, times(1)).setIfAbsent(anyString(), eq("1"), any());
        verify(userRepository, times(1)).addCarbonCreditsAtomically(userId, 5);
    }

    @Test
    @DisplayName("每日签到 - 分布式锁竞争失败")
    void dailyCheckIn_LockAcquisitionFailed() {
        // Arrange
        Long userId = 1L;

        when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(false);

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> carbonCreditService.dailyCheckIn(userId)
        );
        assertEquals("签到处理中，请勿重复操作", exception.getMessage());
        verify(userRepository, never()).addCarbonCreditsAtomically(anyLong(), anyInt());
    }

    @Test
    @DisplayName("每日签到 - 今天已签到")
    void dailyCheckIn_AlreadyCheckedInToday() {
        // Arrange
        Long userId = 1L;

        when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);
        when(creditRecordRepository.existsByUserIdAndChangeTypeAndCreateTimeBetween(
                userId, (byte) 2, any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(true);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> carbonCreditService.dailyCheckIn(userId)
        );
        assertEquals("今天已签到，请明天再来", exception.getMessage());
        verify(userRepository, never()).addCarbonCreditsAtomically(anyLong(), anyInt());
    }

    @Test
    @DisplayName("每日签到 - 连续签到7天获得额外奖励")
    void dailyCheckIn_ContinuousDaysBonus() {
        // Arrange
        Long userId = 1L;
        List<CarbonCreditRecord> checkInRecords = new ArrayList<>();

        // 模拟7条连续签到记录（从最新往前）
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        for (int i = 6; i >= 0; i--) {
            checkInRecords.add(CarbonCreditRecord.builder()
                    .userId(userId)
                    .createTime(now.minusDays(i))
                    .changeType((byte) 2)
                    .build());
        }

        when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);
        when(creditRecordRepository.existsByUserIdAndChangeTypeAndCreateTimeBetween(
                userId, (byte) 2, any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(false);
        when(creditRecordRepository.findCheckInRecords(userId, (byte) 2)).thenReturn(checkInRecords);
        when(userRepository.addCarbonCreditsAtomically(userId, 25)).thenReturn(1); // 5 + 20
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(creditRecordRepository.save(any(CarbonCreditRecord.class))).thenReturn(testRecord);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // Act
        Integer credits = carbonCreditService.dailyCheckIn(userId);

        // Assert
        assertEquals(25, credits); // 基础5 + 额外20
    }

    // ==================== redeemCredits 测试 ====================

    @Test
    @DisplayName("兑换积分 - 成功场景")
    void redeemCredits_Success() {
        // Arrange
        Long userId = 1L;
        Integer amount = 50;
        String description = "兑换充电优惠券";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.deductCarbonCreditsAtomically(userId, amount)).thenReturn(1);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(creditRecordRepository.save(any(CarbonCreditRecord.class))).thenReturn(testRecord);

        // Act
        assertDoesNotThrow(() -> carbonCreditService.redeemCredits(userId, amount, description));

        // Assert
        verify(userRepository, times(1)).deductCarbonCreditsAtomically(userId, amount);
        ArgumentCaptor<CarbonCreditRecord> captor = ArgumentCaptor.forClass(CarbonCreditRecord.class);
        verify(creditRecordRepository, times(1)).save(captor.capture());

        CarbonCreditRecord savedRecord = captor.getValue();
        assertEquals(-amount, (int) savedRecord.getChangeAmount());
        assertEquals((byte) 3, savedRecord.getChangeType());
    }

    @Test
    @DisplayName("兑换积分 - 用户不存在")
    void redeemCredits_UserNotFound() {
        // Arrange
        Long userId = 999L;
        Integer amount = 50;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> carbonCreditService.redeemCredits(userId, amount, "description")
        );
        assertEquals("用户不存在", exception.getMessage());
        verify(userRepository, never()).deductCarbonCreditsAtomically(anyLong(), anyInt());
    }

    @Test
    @DisplayName("兑换积分 - 积分不足")
    void redeemCredits_InsufficientCredits() {
        // Arrange
        Long userId = 1L;
        Integer amount = 200; // 用户只有100

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.deductCarbonCreditsAtomically(userId, amount)).thenReturn(0);

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> carbonCreditService.redeemCredits(userId, amount, "description")
        );
        assertEquals("积分不足", exception.getMessage());
        verify(creditRecordRepository, never()).save(any(CarbonCreditRecord.class));
    }

    @Test
    @DisplayName("兑换积分 - 边界条件：恰好够兑换")
    void redeemCredits_ExactAmount() {
        // Arrange
        Long userId = 1L;
        Integer amount = 100; // 恰好等于用户余额

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.deductCarbonCreditsAtomically(userId, amount)).thenReturn(1);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(creditRecordRepository.save(any(CarbonCreditRecord.class))).thenReturn(testRecord);

        // Act
        assertDoesNotThrow(() -> carbonCreditService.redeemCredits(userId, amount, "description"));

        // Assert
        verify(userRepository, times(1)).deductCarbonCreditsAtomically(userId, amount);
    }

    // ==================== getCreditBalance 测试 ====================

    @Test
    @DisplayName("获取积分余额 - 成功")
    void getCreditBalance_Success() {
        // Arrange
        Long userId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act
        Integer balance = carbonCreditService.getCreditBalance(userId);

        // Assert
        assertEquals(100, balance);
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("获取积分余额 - 用户不存在")
    void getCreditBalance_UserNotFound() {
        // Arrange
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> carbonCreditService.getCreditBalance(userId)
        );
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    @DisplayName("获取积分余额 - 用户没有积分")
    void getCreditBalance_NoCredits() {
        // Arrange
        Long userId = 1L;
        testUser.setCarbonCredits(0);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // Act
        Integer balance = carbonCreditService.getCreditBalance(userId);

        // Assert
        assertEquals(0, balance);
    }

    // ==================== getCreditHistory 测试 ====================

    @Test
    @DisplayName("获取积分历史 - 全部类型")
    void getCreditHistory_AllTypes() {
        // Arrange
        Long userId = 1L;
        List<CarbonCreditRecord> records = new ArrayList<>();

        CarbonCreditRecord record1 = CarbonCreditRecord.builder()
                .id(1L)
                .userId(userId)
                .changeAmount(78)
                .changeType((byte) 1)
                .description("充电10kWh")
                .balanceAfter(178)
                .createTime(LocalDateTime.now())
                .build();

        CarbonCreditRecord record2 = CarbonCreditRecord.builder()
                .id(2L)
                .userId(userId)
                .changeAmount(5)
                .changeType((byte) 2)
                .description("每日签到")
                .balanceAfter(183)
                .createTime(LocalDateTime.now().minusHours(1))
                .build();

        records.add(record1);
        records.add(record2);

        when(creditRecordRepository.findByUserIdOrderByCreateTimeDesc(userId)).thenReturn(records);

        // Act
        List<CreditRecordVO> history = carbonCreditService.getCreditHistory(userId, null);

        // Assert
        assertEquals(2, history.size());
        verify(creditRecordRepository, times(1)).findByUserIdOrderByCreateTimeDesc(userId);
    }

    @Test
    @DisplayName("获取积分历史 - 按类型筛选")
    void getCreditHistory_FilterByType() {
        // Arrange
        Long userId = 1L;
        Byte creditType = (byte) 1; // 仅充电获得

        List<CarbonCreditRecord> records = new ArrayList<>();
        CarbonCreditRecord record = CarbonCreditRecord.builder()
                .id(1L)
                .userId(userId)
                .changeAmount(78)
                .changeType(creditType)
                .description("充电10kWh")
                .balanceAfter(178)
                .createTime(LocalDateTime.now())
                .build();
        records.add(record);

        when(creditRecordRepository.findByUserIdAndChangeTypeOrderByCreateTimeDesc(userId, creditType))
                .thenReturn(records);

        // Act
        List<CreditRecordVO> history = carbonCreditService.getCreditHistory(userId, creditType);

        // Assert
        assertEquals(1, history.size());
        assertEquals((byte) 1, history.get(0).getCreditType());
    }

    @Test
    @DisplayName("获取积分历史 - 空历史")
    void getCreditHistory_Empty() {
        // Arrange
        Long userId = 1L;

        when(creditRecordRepository.findByUserIdOrderByCreateTimeDesc(userId)).thenReturn(new ArrayList<>());

        // Act
        List<CreditRecordVO> history = carbonCreditService.getCreditHistory(userId, null);

        // Assert
        assertTrue(history.isEmpty());
    }

    // ==================== getCreditStatistics 测试 ====================

    @Test
    @DisplayName("获取积分统计 - 完整统计")
    void getCreditStatistics_Success() {
        // Arrange
        Long userId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(creditRecordRepository.sumTotalEarnedCredits(userId)).thenReturn(500);
        when(creditRecordRepository.sumTotalSpentCredits(userId)).thenReturn(100);
        when(creditRecordRepository.sumEarnedCreditsByPeriod(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(200);
        when(creditRecordRepository.sumSpentCreditsByPeriod(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(50);
        when(creditRecordRepository.findCheckInRecords(userId, (byte) 2)).thenReturn(new ArrayList<>());
        when(creditRecordRepository.existsByUserIdAndChangeTypeAndCreateTimeBetween(
                eq(userId), eq((byte) 2), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(false);

        // Act
        CreditStatisticsVO stats = carbonCreditService.getCreditStatistics(userId);

        // Assert
        assertNotNull(stats);
        assertEquals(100, stats.getCurrentBalance());
        assertEquals(500, stats.getTotalEarned());
        assertEquals(100, stats.getTotalSpent());
        assertEquals(200, stats.getMonthEarned());
        assertEquals(50, stats.getMonthSpent());
        assertFalse(stats.isHasCheckedInToday());
    }

    @Test
    @DisplayName("获取积分统计 - 用户不存在")
    void getCreditStatistics_UserNotFound() {
        // Arrange
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> carbonCreditService.getCreditStatistics(userId)
        );
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    @DisplayName("获取积分统计 - 处理null值")
    void getCreditStatistics_HandleNullValues() {
        // Arrange
        Long userId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(creditRecordRepository.sumTotalEarnedCredits(userId)).thenReturn(null);
        when(creditRecordRepository.sumTotalSpentCredits(userId)).thenReturn(null);
        when(creditRecordRepository.sumEarnedCreditsByPeriod(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(null);
        when(creditRecordRepository.sumSpentCreditsByPeriod(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(null);
        when(creditRecordRepository.findCheckInRecords(userId, (byte) 2)).thenReturn(new ArrayList<>());
        when(creditRecordRepository.existsByUserIdAndChangeTypeAndCreateTimeBetween(
                eq(userId), eq((byte) 2), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(false);

        // Act
        CreditStatisticsVO stats = carbonCreditService.getCreditStatistics(userId);

        // Assert
        assertEquals(100, stats.getCurrentBalance());
        assertEquals(0, stats.getTotalEarned());
        assertEquals(0, stats.getTotalSpent());
        assertEquals(0, stats.getMonthEarned());
        assertEquals(0, stats.getMonthSpent());
    }
}
