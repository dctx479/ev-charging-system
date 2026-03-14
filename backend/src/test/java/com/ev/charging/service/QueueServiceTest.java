package com.ev.charging.service;

import com.ev.charging.dto.JoinQueueDTO;
import com.ev.charging.entity.ChargingPile;
import com.ev.charging.entity.ChargingStation;
import com.ev.charging.entity.QueueRecord;
import com.ev.charging.vo.QueueStatusVO;
import com.ev.charging.vo.StationQueueInfoVO;
import com.ev.charging.util.RedisLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Lazy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * QueueService 单元测试
 * 测试排队系统的核心功能：加入排队、离开排队、叫号等
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("排队服务测试")
class QueueServiceTest {

    @Mock
    private com.ev.charging.repository.QueueRecordRepository queueRecordRepository;

    @Mock
    private com.ev.charging.repository.ChargingStationRepository stationRepository;

    @Mock
    private com.ev.charging.repository.ChargingPileRepository pileRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private RedisLockService redisLockService;

    @InjectMocks
    private QueueService queueService;

    private Long userId;
    private Long stationId;
    private Long pileId;
    private JoinQueueDTO joinQueueDTO;
    private ChargingStation chargingStation;
    private ChargingPile chargingPile;
    private QueueRecord queueRecord;

    private static final byte STATUS_QUEUING = 1;    // 排队中
    private static final byte STATUS_ASSIGNED = 2;   // 已分配
    private static final byte STATUS_CANCELLED = 3;  // 已取消
    private static final byte STATUS_EXPIRED = 4;    // 超时

    @BeforeEach
    void setUp() {
        userId = 1L;
        stationId = 10L;
        pileId = 100L;

        joinQueueDTO = JoinQueueDTO.builder()
                .stationId(stationId)
                .pileType((byte) 1) // 快充
                .build();

        chargingStation = ChargingStation.builder()
                .id(stationId)
                .name("测试充电站")
                .status((byte) 1)
                .build();

        chargingPile = ChargingPile.builder()
                .id(pileId)
                .stationId(stationId)
                .pileNo("A001")
                .status((byte) 1) // 空闲
                .power(BigDecimal.valueOf(120))
                .build();

        queueRecord = QueueRecord.builder()
                .id(1L)
                .userId(userId)
                .stationId(stationId)
                .queueNo("10-20250101-001")
                .queueStatus(STATUS_QUEUING)
                .queuePosition(1)
                .estimatedWaitTime(30)
                .joinTime(LocalDateTime.now())
                .pileType((byte) 1)
                .build();
    }

    // ======================== joinQueue 测试 ========================

    @Test
    @DisplayName("加入排队 - 成功场景")
    void testJoinQueue_Success() {
        // Arrange
        String userLockOwner = "user-lock-1";
        String stationLockOwner = "station-lock-1";

        when(redisLockService.tryLockWithOwner(contains("user_queue"), anyInt()))
                .thenReturn(userLockOwner);
        when(redisLockService.tryLockWithOwner(contains("station_queue"), anyInt()))
                .thenReturn(stationLockOwner);
        when(stationRepository.findById(stationId)).thenReturn(Optional.of(chargingStation));
        when(queueRecordRepository.findByUserIdAndStationIdAndQueueStatusIn(userId, stationId, Arrays.asList(STATUS_QUEUING, STATUS_ASSIGNED)))
                .thenReturn(Optional.empty());
        when(pileRepository.countByStationIdAndStatus(stationId, (byte) 1)).thenReturn(0L); // 无空闲充电桩
        when(queueRecordRepository.countByStationIdAndQueueStatus(stationId, STATUS_QUEUING)).thenReturn(0L);
        when(queueRecordRepository.countTodayQueue(stationId, any())).thenReturn(0L);

        QueueRecord saved = queueRecord.toBuilder().id(1L).build();
        when(queueRecordRepository.save(any(QueueRecord.class))).thenReturn(saved);

        // Act
        Long result = queueService.joinQueue(userId, joinQueueDTO);

        // Assert
        assertEquals(1L, result);
        verify(redisLockService, times(2)).tryLockWithOwner(anyString(), anyInt());
        verify(redisLockService, times(2)).unlockSafe(anyString(), anyString());
        verify(queueRecordRepository, times(1)).save(any(QueueRecord.class));
    }

    @Test
    @DisplayName("加入排队 - 获取用户锁失败")
    void testJoinQueue_UserLockFailed() {
        // Arrange
        when(redisLockService.tryLockWithOwner(contains("user_queue"), anyInt()))
                .thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> queueService.joinQueue(userId, joinQueueDTO));
        verify(queueRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("加入排队 - 获取站点锁失败")
    void testJoinQueue_StationLockFailed() {
        // Arrange
        String userLockOwner = "user-lock-1";
        when(redisLockService.tryLockWithOwner(contains("user_queue"), anyInt()))
                .thenReturn(userLockOwner);
        when(redisLockService.tryLockWithOwner(contains("station_queue"), anyInt()))
                .thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> queueService.joinQueue(userId, joinQueueDTO));
        verify(redisLockService, times(1)).unlockSafe(contains("user_queue"), eq(userLockOwner));
    }

    @Test
    @DisplayName("加入排队 - 充电站不存在")
    void testJoinQueue_StationNotFound() {
        // Arrange
        String userLockOwner = "user-lock-1";
        String stationLockOwner = "station-lock-1";

        when(redisLockService.tryLockWithOwner(contains("user_queue"), anyInt()))
                .thenReturn(userLockOwner);
        when(redisLockService.tryLockWithOwner(contains("station_queue"), anyInt()))
                .thenReturn(stationLockOwner);
        when(stationRepository.findById(stationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> queueService.joinQueue(userId, joinQueueDTO));
        verify(redisLockService, times(2)).unlockSafe(anyString(), anyString());
    }

    @Test
    @DisplayName("加入排队 - 充电站暂停营业")
    void testJoinQueue_StationClosed() {
        // Arrange
        String userLockOwner = "user-lock-1";
        String stationLockOwner = "station-lock-1";
        chargingStation.setStatus((byte) 0); // 暂停营业

        when(redisLockService.tryLockWithOwner(contains("user_queue"), anyInt()))
                .thenReturn(userLockOwner);
        when(redisLockService.tryLockWithOwner(contains("station_queue"), anyInt()))
                .thenReturn(stationLockOwner);
        when(stationRepository.findById(stationId)).thenReturn(Optional.of(chargingStation));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> queueService.joinQueue(userId, joinQueueDTO));
        verify(redisLockService, times(2)).unlockSafe(anyString(), anyString());
    }

    @Test
    @DisplayName("加入排队 - 用户已在该站点排队")
    void testJoinQueue_UserAlreadyQueuing() {
        // Arrange
        String userLockOwner = "user-lock-1";
        String stationLockOwner = "station-lock-1";

        when(redisLockService.tryLockWithOwner(contains("user_queue"), anyInt()))
                .thenReturn(userLockOwner);
        when(redisLockService.tryLockWithOwner(contains("station_queue"), anyInt()))
                .thenReturn(stationLockOwner);
        when(stationRepository.findById(stationId)).thenReturn(Optional.of(chargingStation));
        when(queueRecordRepository.findByUserIdAndStationIdAndQueueStatusIn(userId, stationId, Arrays.asList(STATUS_QUEUING, STATUS_ASSIGNED)))
                .thenReturn(Optional.of(queueRecord));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> queueService.joinQueue(userId, joinQueueDTO));
        verify(redisLockService, times(2)).unlockSafe(anyString(), anyString());
    }

    @Test
    @DisplayName("加入排队 - 当前有空闲充电桩，无需排队")
    void testJoinQueue_AvailablePilesExist() {
        // Arrange
        String userLockOwner = "user-lock-1";
        String stationLockOwner = "station-lock-1";

        when(redisLockService.tryLockWithOwner(contains("user_queue"), anyInt()))
                .thenReturn(userLockOwner);
        when(redisLockService.tryLockWithOwner(contains("station_queue"), anyInt()))
                .thenReturn(stationLockOwner);
        when(stationRepository.findById(stationId)).thenReturn(Optional.of(chargingStation));
        when(queueRecordRepository.findByUserIdAndStationIdAndQueueStatusIn(userId, stationId, Arrays.asList(STATUS_QUEUING, STATUS_ASSIGNED)))
                .thenReturn(Optional.empty());
        when(pileRepository.countByStationIdAndStatus(stationId, (byte) 1)).thenReturn(2L); // 有2个空闲充电桩

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> queueService.joinQueue(userId, joinQueueDTO));
        verify(redisLockService, times(2)).unlockSafe(anyString(), anyString());
    }

    @Test
    @DisplayName("加入排队 - 排队号生成正确")
    void testJoinQueue_QueueNoGeneration() {
        // Arrange
        String userLockOwner = "user-lock-1";
        String stationLockOwner = "station-lock-1";

        when(redisLockService.tryLockWithOwner(contains("user_queue"), anyInt()))
                .thenReturn(userLockOwner);
        when(redisLockService.tryLockWithOwner(contains("station_queue"), anyInt()))
                .thenReturn(stationLockOwner);
        when(stationRepository.findById(stationId)).thenReturn(Optional.of(chargingStation));
        when(queueRecordRepository.findByUserIdAndStationIdAndQueueStatusIn(userId, stationId, Arrays.asList(STATUS_QUEUING, STATUS_ASSIGNED)))
                .thenReturn(Optional.empty());
        when(pileRepository.countByStationIdAndStatus(stationId, (byte) 1)).thenReturn(0L);
        when(queueRecordRepository.countByStationIdAndQueueStatus(stationId, STATUS_QUEUING)).thenReturn(5L);
        when(queueRecordRepository.countTodayQueue(stationId, any())).thenReturn(5L);

        QueueRecord saved = queueRecord.toBuilder().id(1L).build();
        when(queueRecordRepository.save(any(QueueRecord.class))).thenReturn(saved);

        // Act
        Long result = queueService.joinQueue(userId, joinQueueDTO);

        // Assert
        assertEquals(1L, result);

        // 验证保存的排队记录的排队号格式
        verify(queueRecordRepository, times(1)).save(argThat(record ->
                record.getQueueNo().matches("\\d+-\\d+-\\d+") &&
                record.getQueuePosition() == 6 // 当前5人排队，新用户为第6位
        ));
    }

    // ======================== getQueueStatus 测试 ========================

    @Test
    @DisplayName("获取排队状态 - 排队中")
    void testGetQueueStatus_Queuing() {
        // Arrange
        queueRecord.setQueueStatus(STATUS_QUEUING);
        when(queueRecordRepository.findByUserIdAndQueueStatusIn(userId, Arrays.asList(STATUS_QUEUING, STATUS_ASSIGNED)))
                .thenReturn(Optional.of(queueRecord));
        when(queueRecordRepository.findByStationIdAndQueueStatusOrderByJoinTimeAscIdAsc(stationId, STATUS_QUEUING))
                .thenReturn(Arrays.asList(queueRecord));
        when(stationRepository.findById(stationId)).thenReturn(Optional.of(chargingStation));

        // Act
        QueueStatusVO result = queueService.getQueueStatus(userId);

        // Assert
        assertNotNull(result);
        assertEquals(STATUS_QUEUING, result.getStatus());
        assertEquals(1, result.getQueuePosition());
        assertEquals(0, result.getPeopleAhead());
    }

    @Test
    @DisplayName("获取排队状态 - 已分配（已叫号）")
    void testGetQueueStatus_Assigned() {
        // Arrange
        queueRecord.setQueueStatus(STATUS_ASSIGNED);
        queueRecord.setAssignedTime(LocalDateTime.now().minusMinutes(5));
        queueRecord.setAssignedPileId(pileId);
        when(queueRecordRepository.findByUserIdAndQueueStatusIn(userId, Arrays.asList(STATUS_QUEUING, STATUS_ASSIGNED)))
                .thenReturn(Optional.of(queueRecord));
        when(stationRepository.findById(stationId)).thenReturn(Optional.of(chargingStation));

        // Act
        QueueStatusVO result = queueService.getQueueStatus(userId);

        // Assert
        assertNotNull(result);
        assertEquals(STATUS_ASSIGNED, result.getStatus());
        assertEquals(0, result.getPeopleAhead()); // 已叫号的人，前面没有人
        assertNotNull(result.getCallTime());
        assertNotNull(result.getExpireTime());
    }

    @Test
    @DisplayName("获取排队状态 - 用户不在排队中")
    void testGetQueueStatus_NoActiveQueue() {
        // Arrange
        when(queueRecordRepository.findByUserIdAndQueueStatusIn(userId, Arrays.asList(STATUS_QUEUING, STATUS_ASSIGNED)))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> queueService.getQueueStatus(userId));
    }

    @Test
    @DisplayName("获取排队状态 - 计算即将过期")
    void testGetQueueStatus_WillExpireSoon() {
        // Arrange
        LocalDateTime assignedTime = LocalDateTime.now().minusMinutes(11); // 叫号后11分钟
        queueRecord.setQueueStatus(STATUS_ASSIGNED);
        queueRecord.setAssignedTime(assignedTime); // 已叫号11分钟
        queueRecord.setAssignedPileId(pileId);

        when(queueRecordRepository.findByUserIdAndQueueStatusIn(userId, Arrays.asList(STATUS_QUEUING, STATUS_ASSIGNED)))
                .thenReturn(Optional.of(queueRecord));
        when(stationRepository.findById(stationId)).thenReturn(Optional.of(chargingStation));

        // Act
        QueueStatusVO result = queueService.getQueueStatus(userId);

        // Assert
        assertNotNull(result);
        // 如果叫号后超过10分钟（15分钟超时 - 5分钟预警 = 10分钟）
        // 则 willExpireSoon 应为 true
        assertTrue(result.isWillExpireSoon());
    }

    // ======================== leaveQueue 测试 ========================

    @Test
    @DisplayName("离开队列 - 成功场景")
    void testLeaveQueue_Success() {
        // Arrange
        when(queueRecordRepository.findByUserIdAndQueueStatusIn(userId, Arrays.asList(STATUS_QUEUING, STATUS_ASSIGNED)))
                .thenReturn(Optional.of(queueRecord));
        when(queueRecordRepository.findByStationIdAndQueueStatusOrderByJoinTimeAscIdAsc(stationId, STATUS_QUEUING))
                .thenReturn(new ArrayList<>());

        // Act
        queueService.leaveQueue(userId);

        // Assert
        assertEquals(STATUS_CANCELLED, queueRecord.getQueueStatus());
        assertNotNull(queueRecord.getCancelTime());
        verify(queueRecordRepository, times(1)).save(queueRecord);
    }

    @Test
    @DisplayName("离开队列 - 用户不在排队中")
    void testLeaveQueue_NoActiveQueue() {
        // Arrange
        when(queueRecordRepository.findByUserIdAndQueueStatusIn(userId, Arrays.asList(STATUS_QUEUING, STATUS_ASSIGNED)))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> queueService.leaveQueue(userId));
    }

    @Test
    @DisplayName("离开队列 - 更新后续排队者位置")
    void testLeaveQueue_UpdateSubsequentPositions() {
        // Arrange
        QueueRecord secondRecord = queueRecord.toBuilder()
                .id(2L)
                .userId(2L)
                .queuePosition(2)
                .joinTime(LocalDateTime.now().plusMinutes(1))
                .build();

        when(queueRecordRepository.findByUserIdAndQueueStatusIn(userId, Arrays.asList(STATUS_QUEUING, STATUS_ASSIGNED)))
                .thenReturn(Optional.of(queueRecord));
        when(queueRecordRepository.findByStationIdAndQueueStatusOrderByJoinTimeAscIdAsc(stationId, STATUS_QUEUING))
                .thenReturn(Arrays.asList(secondRecord)); // 剩余一个排队者

        // Act
        queueService.leaveQueue(userId);

        // Assert
        assertEquals(STATUS_CANCELLED, queueRecord.getQueueStatus());
        // 验证位置更新被调用
        verify(queueRecordRepository).saveAll(anyList());
    }

    // ======================== getStationQueueInfo 测试 ========================

    @Test
    @DisplayName("获取站点排队信息 - 成功场景")
    void testGetStationQueueInfo_Success() {
        // Arrange
        when(stationRepository.findById(stationId)).thenReturn(Optional.of(chargingStation));
        when(queueRecordRepository.countByStationIdAndQueueStatus(stationId, STATUS_QUEUING)).thenReturn(3L);
        when(pileRepository.countByStationIdAndStatus(stationId, (byte) 1)).thenReturn(2L);

        // Act
        StationQueueInfoVO result = queueService.getStationQueueInfo(stationId);

        // Assert
        assertNotNull(result);
        assertEquals(stationId, result.getStationId());
        assertEquals("测试充电站", result.getStationName());
        assertEquals(3, result.getQueueCount());
        assertEquals(2, result.getAvailablePiles());
        assertNotNull(result.getAverageWaitTime());
        assertNotNull(result.getSuggestion());
    }

    @Test
    @DisplayName("获取站点排队信息 - 充电站不存在")
    void testGetStationQueueInfo_StationNotFound() {
        // Arrange
        when(stationRepository.findById(stationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> queueService.getStationQueueInfo(stationId));
    }

    @Test
    @DisplayName("获取站点排队信息 - 建议排队（人数少）")
    void testGetStationQueueInfo_RecommendQueue() {
        // Arrange
        when(stationRepository.findById(stationId)).thenReturn(Optional.of(chargingStation));
        when(queueRecordRepository.countByStationIdAndQueueStatus(stationId, STATUS_QUEUING)).thenReturn(2L);
        when(pileRepository.countByStationIdAndStatus(stationId, (byte) 1)).thenReturn(3L);

        // Act
        StationQueueInfoVO result = queueService.getStationQueueInfo(stationId);

        // Assert
        assertTrue(result.isRecommendQueue());
    }

    @Test
    @DisplayName("获取站点排队信息 - 不建议排队（人数多）")
    void testGetStationQueueInfo_NotRecommendQueue() {
        // Arrange
        when(stationRepository.findById(stationId)).thenReturn(Optional.of(chargingStation));
        when(queueRecordRepository.countByStationIdAndQueueStatus(stationId, STATUS_QUEUING)).thenReturn(15L);
        when(pileRepository.countByStationIdAndStatus(stationId, (byte) 1)).thenReturn(1L);

        // Act
        StationQueueInfoVO result = queueService.getStationQueueInfo(stationId);

        // Assert
        assertFalse(result.isRecommendQueue());
    }

    // ======================== callNext 测试 ========================

    @Test
    @DisplayName("叫下一号 - 成功场景")
    void testCallNext_Success() {
        // Arrange
        String stationLockOwner = "station-lock-1";
        when(redisLockService.tryLockWithOwner(contains("station_queue"), anyInt()))
                .thenReturn(stationLockOwner);
        when(queueRecordRepository.findByStationIdAndQueueStatusOrderByJoinTimeAscIdAsc(stationId, STATUS_QUEUING))
                .thenReturn(Arrays.asList(queueRecord));
        when(orderService.createOrderFromQueue(userId, pileId)).thenReturn(1000L);
        when(queueRecordRepository.findById(queueRecord.getId())).thenReturn(Optional.of(queueRecord));

        // Act
        queueService.callNext(stationId, pileId);

        // Assert
        assertEquals(STATUS_ASSIGNED, queueRecord.getQueueStatus());
        assertEquals(pileId, queueRecord.getAssignedPileId());
        assertNotNull(queueRecord.getAssignedTime());
        verify(orderService, times(1)).createOrderFromQueue(userId, pileId);
        verify(redisLockService, times(1)).unlockSafe(anyString(), eq(stationLockOwner));
    }

    @Test
    @DisplayName("叫下一号 - 获取锁失败")
    void testCallNext_LockFailed() {
        // Arrange
        when(redisLockService.tryLockWithOwner(contains("station_queue"), anyInt()))
                .thenReturn(null);

        // Act
        queueService.callNext(stationId, pileId);

        // Assert
        verify(queueRecordRepository, never()).save(any());
        verify(orderService, never()).createOrderFromQueue(anyLong(), anyLong());
    }

    @Test
    @DisplayName("叫下一号 - 队列为空")
    void testCallNext_EmptyQueue() {
        // Arrange
        String stationLockOwner = "station-lock-1";
        when(redisLockService.tryLockWithOwner(contains("station_queue"), anyInt()))
                .thenReturn(stationLockOwner);
        when(queueRecordRepository.findByStationIdAndQueueStatusOrderByJoinTimeAscIdAsc(stationId, STATUS_QUEUING))
                .thenReturn(new ArrayList<>()); // 空队列

        // Act
        queueService.callNext(stationId, pileId);

        // Assert
        verify(queueRecordRepository, never()).save(any());
        verify(orderService, never()).createOrderFromQueue(anyLong(), anyLong());
        verify(redisLockService, times(1)).unlockSafe(anyString(), eq(stationLockOwner));
    }

    @Test
    @DisplayName("叫下一号 - 自动创建订单失败")
    void testCallNext_CreateOrderFailed() {
        // Arrange
        String stationLockOwner = "station-lock-1";
        when(redisLockService.tryLockWithOwner(contains("station_queue"), anyInt()))
                .thenReturn(stationLockOwner);
        when(queueRecordRepository.findByStationIdAndQueueStatusOrderByJoinTimeAscIdAsc(stationId, STATUS_QUEUING))
                .thenReturn(Arrays.asList(queueRecord));
        when(orderService.createOrderFromQueue(userId, pileId))
                .thenThrow(new RuntimeException("订单创建失败"));

        // Act & Assert
        // 应该捕获异常，不抛出，继续处理
        assertDoesNotThrow(() -> queueService.callNext(stationId, pileId));

        // 即使创建订单失败，叫号状态仍应更新
        assertEquals(STATUS_ASSIGNED, queueRecord.getQueueStatus());
        verify(redisLockService, times(1)).unlockSafe(anyString(), eq(stationLockOwner));
    }

    @Test
    @DisplayName("叫下一号 - 更新排队记录中的订单ID")
    void testCallNext_UpdateOrderId() {
        // Arrange
        String stationLockOwner = "station-lock-1";
        Long orderId = 2000L;

        when(redisLockService.tryLockWithOwner(contains("station_queue"), anyInt()))
                .thenReturn(stationLockOwner);
        when(queueRecordRepository.findByStationIdAndQueueStatusOrderByJoinTimeAscIdAsc(stationId, STATUS_QUEUING))
                .thenReturn(Arrays.asList(queueRecord));
        when(orderService.createOrderFromQueue(userId, pileId)).thenReturn(orderId);

        QueueRecord refreshedRecord = queueRecord.toBuilder().orderId(orderId).build();
        when(queueRecordRepository.findById(queueRecord.getId())).thenReturn(Optional.of(refreshedRecord));

        // Act
        queueService.callNext(stationId, pileId);

        // Assert
        assertEquals(orderId, refreshedRecord.getOrderId());
    }

    // ======================== checkExpiredCalls 测试 ========================

    @Test
    @DisplayName("检查过号 - 检测过期记录")
    void testCheckExpiredCalls_DetectExpired() {
        // Arrange
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(16); // 超过15分钟
        QueueRecord expiredRecord = queueRecord.toBuilder()
                .assignedTime(expiredTime)
                .queueStatus(STATUS_ASSIGNED)
                .build();

        when(queueRecordRepository.findExpiredRecords(any(LocalDateTime.class), eq(STATUS_ASSIGNED)))
                .thenReturn(Arrays.asList(expiredRecord));

        // Act
        queueService.checkExpiredCalls();

        // Assert
        assertEquals(STATUS_EXPIRED, expiredRecord.getQueueStatus());
        verify(queueRecordRepository, atLeastOnce()).save(expiredRecord);
    }

    @Test
    @DisplayName("检查过号 - 没有过期记录")
    void testCheckExpiredCalls_NoExpiredRecords() {
        // Arrange
        when(queueRecordRepository.findExpiredRecords(any(LocalDateTime.class), eq(STATUS_ASSIGNED)))
                .thenReturn(new ArrayList<>());

        // Act
        queueService.checkExpiredCalls();

        // Assert
        verify(queueRecordRepository, never()).save(any());
    }

    // ======================== 辅助方法测试 ========================

    @Test
    @DisplayName("计算预计等待时间 - 正常场景")
    void testCalculateEstimatedWaitTime() {
        // 这是私有方法，通过 joinQueue 间接测试
        // Arrange
        String userLockOwner = "user-lock-1";
        String stationLockOwner = "station-lock-1";

        when(redisLockService.tryLockWithOwner(contains("user_queue"), anyInt()))
                .thenReturn(userLockOwner);
        when(redisLockService.tryLockWithOwner(contains("station_queue"), anyInt()))
                .thenReturn(stationLockOwner);
        when(stationRepository.findById(stationId)).thenReturn(Optional.of(chargingStation));
        when(queueRecordRepository.findByUserIdAndStationIdAndQueueStatusIn(userId, stationId, Arrays.asList(STATUS_QUEUING, STATUS_ASSIGNED)))
                .thenReturn(Optional.empty());
        when(pileRepository.countByStationIdAndStatus(stationId, (byte) 1)).thenReturn(0L);
        when(queueRecordRepository.countByStationIdAndQueueStatus(stationId, STATUS_QUEUING)).thenReturn(2L);
        when(queueRecordRepository.countTodayQueue(stationId, any())).thenReturn(2L);

        QueueRecord saved = queueRecord.toBuilder().id(1L).build();
        when(queueRecordRepository.save(any(QueueRecord.class))).thenReturn(saved);

        // Act
        queueService.joinQueue(userId, joinQueueDTO);

        // Assert
        // 预计等待时间应该根据队列位置和充电桩数量计算
        verify(queueRecordRepository, times(1)).save(argThat(record ->
                record.getEstimatedWaitTime() > 0 &&
                record.getQueuePosition() == 3 // 当前2人排队，新用户为第3位
        ));
    }
}
