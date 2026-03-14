package com.ev.charging.service;

import com.ev.charging.constant.OrderConstants;
import com.ev.charging.dto.CreateOrderDTO;
import com.ev.charging.entity.ChargeOrder;
import com.ev.charging.entity.ChargingPile;
import com.ev.charging.entity.ChargingStation;
import com.ev.charging.entity.CreditPendingRecord;
import com.ev.charging.entity.Payment;
import com.ev.charging.entity.QueueRecord;
import com.ev.charging.exception.BusinessException;
import com.ev.charging.mq.producer.MessageProducer;
import com.ev.charging.repository.ChargeOrderRepository;
import com.ev.charging.repository.ChargingPileRepository;
import com.ev.charging.repository.ChargingStationRepository;
import com.ev.charging.repository.CreditPendingRecordRepository;
import com.ev.charging.repository.PaymentRepository;
import com.ev.charging.repository.QueueRecordRepository;
import com.ev.charging.util.RedisLockService;
import com.ev.charging.vo.ChargeFeeDetail;
import com.ev.charging.vo.OrderDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
 * OrderService 单元测试
 * 测试订单创建、结束充电、费用计算、支付等核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("订单服务测试")
class OrderServiceTest {

    @Mock
    private ChargeOrderRepository orderRepository;

    @Mock
    private ChargingPileRepository pileRepository;

    @Mock
    private ChargingStationRepository stationRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CarbonCreditService carbonCreditService;

    @Mock
    private MessageProducer messageProducer;

    @Mock
    private QueueRecordRepository queueRecordRepository;

    @Mock
    private CreditPendingRecordRepository creditPendingRecordRepository;

    @Mock
    private RedisLockService redisLockService;

    @Mock
    private CacheService cacheService;

    @Mock
    private OrderTransactionService orderTransactionService;

    @InjectMocks
    private OrderService orderService;

    private Long userId;
    private Long stationId;
    private Long pileId;
    private Long orderId;
    private CreateOrderDTO createOrderDTO;
    private ChargingPile chargingPile;
    private ChargingStation chargingStation;
    private ChargeOrder chargeOrder;

    @BeforeEach
    void setUp() {
        userId = 1L;
        stationId = 10L;
        pileId = 100L;
        orderId = 1000L;

        // 初始化测试数据
        createOrderDTO = CreateOrderDTO.builder()
                .pileId(pileId)
                .startSoc((byte) 20)
                .targetValue(BigDecimal.valueOf(100))
                .chargeMode((byte) 1) // 快充
                .build();

        chargingPile = ChargingPile.builder()
                .id(pileId)
                .stationId(stationId)
                .pileNo("A001")
                .status((byte) 1) // 空闲
                .power(BigDecimal.valueOf(120)) // 120kW
                .build();

        chargingStation = ChargingStation.builder()
                .id(stationId)
                .name("测试充电站")
                .status((byte) 1)
                .build();

        chargeOrder = ChargeOrder.builder()
                .id(orderId)
                .userId(userId)
                .stationId(stationId)
                .pileId(pileId)
                .orderNo("CO20250101001")
                .orderStatus(OrderConstants.ORDER_STATUS_CHARGING)
                .paymentStatus(OrderConstants.PAYMENT_STATUS_UNPAID)
                .startTime(LocalDateTime.now().minusMinutes(30))
                .startSoc((byte) 20)
                .build();
    }

    // ======================== createOrder 测试 ========================

    @Test
    @DisplayName("创建订单 - 成功场景")
    void testCreateOrder_Success() {
        // Arrange
        String lockOwner = "lock-owner-1";
        when(redisLockService.tryLockWithOwner(anyString(), anyInt())).thenReturn(lockOwner);
        when(pileRepository.findById(pileId)).thenReturn(Optional.of(chargingPile));
        when(orderRepository.findByUserIdAndOrderStatus(userId, OrderConstants.ORDER_STATUS_CHARGING))
                .thenReturn(Optional.empty());
        when(queueRecordRepository.findByUserIdAndQueueStatusIn(userId, Arrays.asList((byte) 2)))
                .thenReturn(Optional.empty());
        when(queueRecordRepository.countByStationIdAndQueueStatus(stationId, (byte) 1))
                .thenReturn(0L);
        when(orderRepository.findByPileIdAndOrderStatus(pileId, OrderConstants.ORDER_STATUS_CHARGING))
                .thenReturn(Optional.empty());
        when(orderTransactionService.createOrderInTransaction(userId, createOrderDTO, chargingPile, Optional.empty()))
                .thenReturn(orderId);

        // Act
        Long result = orderService.createOrder(userId, createOrderDTO);

        // Assert
        assertEquals(orderId, result);
        verify(orderTransactionService, times(1))
                .createOrderInTransaction(userId, createOrderDTO, chargingPile, Optional.empty());
        verify(redisLockService, times(1)).tryLockWithOwner(anyString(), anyInt());
    }

    @Test
    @DisplayName("创建订单 - 获取锁失败")
    void testCreateOrder_LockFailure() {
        // Arrange
        when(redisLockService.tryLockWithOwner(anyString(), anyInt())).thenReturn(null);

        // Act & Assert
        assertThrows(BusinessException.class, () -> orderService.createOrder(userId, createOrderDTO));
        verify(orderTransactionService, never()).createOrderInTransaction(any(), any(), any(), any());
    }

    @Test
    @DisplayName("创建订单 - 充电桩不存在")
    void testCreateOrder_PileNotFound() {
        // Arrange
        String lockOwner = "lock-owner-1";
        when(redisLockService.tryLockWithOwner(anyString(), anyInt())).thenReturn(lockOwner);
        when(pileRepository.findById(pileId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BusinessException.class, () -> orderService.createOrder(userId, createOrderDTO));
        verify(redisLockService, times(1)).unlockSafe(anyString(), eq(lockOwner));
    }

    @Test
    @DisplayName("创建订单 - 用户已有进行中的订单")
    void testCreateOrder_UserAlreadyHasChargingOrder() {
        // Arrange
        String lockOwner = "lock-owner-1";
        when(redisLockService.tryLockWithOwner(anyString(), anyInt())).thenReturn(lockOwner);
        when(pileRepository.findById(pileId)).thenReturn(Optional.of(chargingPile));
        when(orderRepository.findByUserIdAndOrderStatus(userId, OrderConstants.ORDER_STATUS_CHARGING))
                .thenReturn(Optional.of(chargeOrder));

        // Act & Assert
        assertThrows(BusinessException.class, () -> orderService.createOrder(userId, createOrderDTO));
        verify(redisLockService, times(1)).unlockSafe(anyString(), eq(lockOwner));
    }

    @Test
    @DisplayName("创建订单 - 充电桩不可用")
    void testCreateOrder_PileUnavailable() {
        // Arrange
        String lockOwner = "lock-owner-1";
        chargingPile.setStatus((byte) 4); // 故障状态
        when(redisLockService.tryLockWithOwner(anyString(), anyInt())).thenReturn(lockOwner);
        when(pileRepository.findById(pileId)).thenReturn(Optional.of(chargingPile));
        when(orderRepository.findByUserIdAndOrderStatus(userId, OrderConstants.ORDER_STATUS_CHARGING))
                .thenReturn(Optional.empty());
        when(queueRecordRepository.findByUserIdAndQueueStatusIn(userId, Arrays.asList((byte) 2)))
                .thenReturn(Optional.empty());
        when(queueRecordRepository.countByStationIdAndQueueStatus(stationId, (byte) 1))
                .thenReturn(0L);

        // Act & Assert
        assertThrows(BusinessException.class, () -> orderService.createOrder(userId, createOrderDTO));
        verify(redisLockService, times(1)).unlockSafe(anyString(), eq(lockOwner));
    }

    @Test
    @DisplayName("创建订单 - 当前站点有人排队，不允许直接充电")
    void testCreateOrder_QueueExists() {
        // Arrange
        String lockOwner = "lock-owner-1";
        when(redisLockService.tryLockWithOwner(anyString(), anyInt())).thenReturn(lockOwner);
        when(pileRepository.findById(pileId)).thenReturn(Optional.of(chargingPile));
        when(orderRepository.findByUserIdAndOrderStatus(userId, OrderConstants.ORDER_STATUS_CHARGING))
                .thenReturn(Optional.empty());
        when(queueRecordRepository.findByUserIdAndQueueStatusIn(userId, Arrays.asList((byte) 2)))
                .thenReturn(Optional.empty());
        when(queueRecordRepository.countByStationIdAndQueueStatus(stationId, (byte) 1))
                .thenReturn(3L); // 有3人排队

        // Act & Assert
        assertThrows(BusinessException.class, () -> orderService.createOrder(userId, createOrderDTO));
        verify(redisLockService, times(1)).unlockSafe(anyString(), eq(lockOwner));
    }

    // ======================== endCharging 测试 ========================

    @Test
    @DisplayName("结束充电 - 成功场景")
    void testEndCharging_Success() {
        // Arrange
        chargeOrder.setStartTime(LocalDateTime.now().minusMinutes(60));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(chargeOrder));
        when(pileRepository.findById(pileId)).thenReturn(Optional.of(chargingPile));

        // Act
        orderService.endCharging(orderId, userId, 80);

        // Assert
        assertEquals(OrderConstants.ORDER_STATUS_COMPLETED, chargeOrder.getOrderStatus());
        assertEquals(80, chargeOrder.getEndSoc());
        assertNotNull(chargeOrder.getChargeAmount());
        assertNotNull(chargeOrder.getTotalFee());
        verify(orderRepository, times(1)).save(chargeOrder);
        verify(pileRepository, times(1)).save(chargingPile);
    }

    @Test
    @DisplayName("结束充电 - 订单不存在")
    void testEndCharging_OrderNotFound() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BusinessException.class, () -> orderService.endCharging(orderId, userId, 80));
    }

    @Test
    @DisplayName("结束充电 - 用户无权操作（IDOR攻击防护）")
    void testEndCharging_UnauthorizedUser() {
        // Arrange
        Long otherUserId = 999L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(chargeOrder));

        // Act & Assert
        assertThrows(BusinessException.class, () -> orderService.endCharging(orderId, otherUserId, 80));
    }

    @Test
    @DisplayName("结束充电 - 订单状态异常（非充电中）")
    void testEndCharging_InvalidOrderStatus() {
        // Arrange
        chargeOrder.setOrderStatus(OrderConstants.ORDER_STATUS_COMPLETED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(chargeOrder));

        // Act & Assert
        assertThrows(BusinessException.class, () -> orderService.endCharging(orderId, userId, 80));
    }

    @Test
    @DisplayName("结束充电 - 结束电量无效（超过100%）")
    void testEndCharging_InvalidEndSoc() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(chargeOrder));

        // Act & Assert
        assertThrows(BusinessException.class, () -> orderService.endCharging(orderId, userId, 105));
    }

    @Test
    @DisplayName("结束充电 - 结束电量小于等于起始电量")
    void testEndCharging_EndSocNotGreaterThanStartSoc() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(chargeOrder));

        // Act & Assert
        assertThrows(BusinessException.class, () -> orderService.endCharging(orderId, userId, 20));
    }

    @Test
    @DisplayName("结束充电 - 充电量超过安全上限（200kWh）")
    void testEndCharging_ChargeAmountExceedsLimit() {
        // Arrange
        chargeOrder.setStartTime(LocalDateTime.now().minusHours(10)); // 10小时
        chargingPile.setPower(BigDecimal.valueOf(300)); // 300kW
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(chargeOrder));
        when(pileRepository.findById(pileId)).thenReturn(Optional.of(chargingPile));

        // Act
        orderService.endCharging(orderId, userId, 80);

        // Assert
        assertTrue(chargeOrder.getChargeAmount().compareTo(new BigDecimal("200")) <= 0);
    }

    // ======================== calculateChargeFeeDetail 测试 ========================

    @Test
    @DisplayName("计算电费详情 - 谷时充电")
    void testCalculateChargeFeeDetail_ValleyTime() {
        // Arrange - 谷时：23:00-07:00
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 1, 23, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 1, 2, 1, 0); // 2小时谷时
        BigDecimal chargeAmount = BigDecimal.valueOf(100); // 100kWh

        // Act
        ChargeFeeDetail detail = orderService.calculateChargeFeeDetail(startTime, endTime, chargeAmount);

        // Assert
        assertEquals(120, detail.getTotalMinutes());
        assertNotNull(detail.getValleyFee());
        assertTrue(detail.getValleyFee().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(BigDecimal.ZERO, detail.getPeakFee());
    }

    @Test
    @DisplayName("计算电费详情 - 峰时充电")
    void testCalculateChargeFeeDetail_PeakTime() {
        // Arrange - 峰时：10:00-15:00
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 1, 1, 15, 0); // 5小时峰时
        BigDecimal chargeAmount = BigDecimal.valueOf(100); // 100kWh

        // Act
        ChargeFeeDetail detail = orderService.calculateChargeFeeDetail(startTime, endTime, chargeAmount);

        // Assert
        assertEquals(300, detail.getTotalMinutes());
        assertNotNull(detail.getPeakFee());
        assertTrue(detail.getPeakFee().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(BigDecimal.ZERO, detail.getValleyFee());
    }

    @Test
    @DisplayName("计算电费详情 - 跨越多时段充电")
    void testCalculateChargeFeeDetail_MultipleTimeSlots() {
        // Arrange - 从谷时跨到峰时（23:00 -> 11:00）
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 1, 23, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 1, 2, 11, 0); // 12小时
        BigDecimal chargeAmount = BigDecimal.valueOf(100); // 100kWh

        // Act
        ChargeFeeDetail detail = orderService.calculateChargeFeeDetail(startTime, endTime, chargeAmount);

        // Assert
        assertEquals(720, detail.getTotalMinutes());
        assertTrue(detail.getValleyFee().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(detail.getPeakFee().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(detail.getTotalElectricityFee().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("计算电费详情 - 零时长返回零费用")
    void testCalculateChargeFeeDetail_ZeroDuration() {
        // Arrange
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 12, 0);
        BigDecimal chargeAmount = BigDecimal.valueOf(100);

        // Act
        ChargeFeeDetail detail = orderService.calculateChargeFeeDetail(time, time, chargeAmount);

        // Assert
        assertEquals(0, detail.getTotalMinutes());
        assertEquals(BigDecimal.ZERO, detail.getValleyFee());
        assertEquals(BigDecimal.ZERO, detail.getPeakFee());
        assertEquals(BigDecimal.ZERO, detail.getTotalElectricityFee());
    }

    // ======================== getOrderDetail 测试 ========================

    @Test
    @DisplayName("获取订单详情 - 成功场景")
    void testGetOrderDetail_Success() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(chargeOrder));
        when(pileRepository.findById(pileId)).thenReturn(Optional.of(chargingPile));
        when(stationRepository.findById(stationId)).thenReturn(Optional.of(chargingStation));

        // Act
        OrderDetailVO result = orderService.getOrderDetail(orderId);

        // Assert
        assertNotNull(result);
        assertEquals(orderId, result.getId());
        assertEquals("CO20250101001", result.getOrderNo());
        assertEquals("A001", result.getPileNo());
        assertEquals("测试充电站", result.getStationName());
    }

    @Test
    @DisplayName("获取订单详情 - 订单不存在")
    void testGetOrderDetail_OrderNotFound() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BusinessException.class, () -> orderService.getOrderDetail(orderId));
    }

    @Test
    @DisplayName("获取订单详情 - 充电桩不存在返回未知")
    void testGetOrderDetail_PileNotFound() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(chargeOrder));
        when(pileRepository.findById(pileId)).thenReturn(Optional.empty());
        when(stationRepository.findById(stationId)).thenReturn(Optional.of(chargingStation));

        // Act
        OrderDetailVO result = orderService.getOrderDetail(orderId);

        // Assert
        assertNotNull(result);
        assertEquals("未知充电桩", result.getPileNo());
    }

    // ======================== cancelOrder 测试 ========================

    @Test
    @DisplayName("取消订单 - 成功场景")
    void testCancelOrder_Success() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(chargeOrder));
        when(pileRepository.findById(pileId)).thenReturn(Optional.of(chargingPile));

        // Act
        orderService.cancelOrder(orderId, userId);

        // Assert
        assertEquals(OrderConstants.ORDER_STATUS_CANCELLED, chargeOrder.getOrderStatus());
        assertEquals((byte) 1, chargingPile.getStatus());
        verify(orderRepository, times(1)).save(chargeOrder);
        verify(pileRepository, times(1)).save(chargingPile);
        verify(cacheService, times(1)).evictPileCache(pileId);
    }

    @Test
    @DisplayName("取消订单 - 订单不存在")
    void testCancelOrder_OrderNotFound() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BusinessException.class, () -> orderService.cancelOrder(orderId, userId));
    }

    @Test
    @DisplayName("取消订单 - 用户无权操作")
    void testCancelOrder_UnauthorizedUser() {
        // Arrange
        Long otherUserId = 999L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(chargeOrder));

        // Act & Assert
        assertThrows(BusinessException.class, () -> orderService.cancelOrder(orderId, otherUserId));
    }

    @Test
    @DisplayName("取消订单 - 订单已完成，不能取消")
    void testCancelOrder_OrderAlreadyCompleted() {
        // Arrange
        chargeOrder.setOrderStatus(OrderConstants.ORDER_STATUS_COMPLETED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(chargeOrder));

        // Act & Assert
        assertThrows(BusinessException.class, () -> orderService.cancelOrder(orderId, userId));
    }

    // ======================== payOrder 测试 ========================

    @Test
    @DisplayName("支付订单 - 成功场景")
    void testPayOrder_Success() {
        // Arrange
        chargeOrder.setOrderStatus(OrderConstants.ORDER_STATUS_COMPLETED);
        chargeOrder.setPaymentStatus(OrderConstants.PAYMENT_STATUS_UNPAID);
        chargeOrder.setTotalFee(new BigDecimal("150.00"));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(chargeOrder));
        when(orderRepository.atomicUpdatePayStatus(orderId,
                OrderConstants.PAYMENT_STATUS_PAID,
                OrderConstants.PAYMENT_STATUS_UNPAID))
                .thenReturn(1); // 原子更新成功

        // Act
        orderService.payOrder(orderId, userId, OrderConstants.PAYMENT_METHOD_WECHAT);

        // Assert
        assertEquals(OrderConstants.PAYMENT_STATUS_PAID, chargeOrder.getPaymentStatus());
        assertEquals(OrderConstants.PAYMENT_METHOD_WECHAT, chargeOrder.getPaymentMethod());
        assertNotNull(chargeOrder.getPaymentTime());
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(orderRepository, times(1)).save(chargeOrder);
    }

    @Test
    @DisplayName("支付订单 - 订单不存在")
    void testPayOrder_OrderNotFound() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BusinessException.class,
                () -> orderService.payOrder(orderId, userId, OrderConstants.PAYMENT_METHOD_WECHAT));
    }

    @Test
    @DisplayName("支付订单 - 用户无权操作")
    void testPayOrder_UnauthorizedUser() {
        // Arrange
        Long otherUserId = 999L;
        chargeOrder.setOrderStatus(OrderConstants.ORDER_STATUS_COMPLETED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(chargeOrder));

        // Act & Assert
        assertThrows(BusinessException.class,
                () -> orderService.payOrder(orderId, otherUserId, OrderConstants.PAYMENT_METHOD_WECHAT));
    }

    @Test
    @DisplayName("支付订单 - 订单未完成，无法支付")
    void testPayOrder_OrderNotCompleted() {
        // Arrange
        chargeOrder.setOrderStatus(OrderConstants.ORDER_STATUS_CHARGING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(chargeOrder));

        // Act & Assert
        assertThrows(BusinessException.class,
                () -> orderService.payOrder(orderId, userId, OrderConstants.PAYMENT_METHOD_WECHAT));
    }

    @Test
    @DisplayName("支付订单 - 重复支付失败")
    void testPayOrder_AlreadyPaid() {
        // Arrange
        chargeOrder.setOrderStatus(OrderConstants.ORDER_STATUS_COMPLETED);
        chargeOrder.setPaymentStatus(OrderConstants.PAYMENT_STATUS_PAID);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(chargeOrder));
        when(orderRepository.atomicUpdatePayStatus(orderId,
                OrderConstants.PAYMENT_STATUS_PAID,
                OrderConstants.PAYMENT_STATUS_UNPAID))
                .thenReturn(0); // 原子更新失败

        // Act & Assert
        assertThrows(BusinessException.class,
                () -> orderService.payOrder(orderId, userId, OrderConstants.PAYMENT_METHOD_WECHAT));
    }

    // ======================== getCurrentOrder 测试 ========================

    @Test
    @DisplayName("获取当前订单 - 存在进行中的订单")
    void testGetCurrentOrder_Success() {
        // Arrange
        when(orderRepository.findByUserIdAndOrderStatus(userId, OrderConstants.ORDER_STATUS_CHARGING))
                .thenReturn(Optional.of(chargeOrder));
        when(pileRepository.findById(pileId)).thenReturn(Optional.of(chargingPile));
        when(stationRepository.findById(stationId)).thenReturn(Optional.of(chargingStation));

        // Act
        OrderDetailVO result = orderService.getCurrentOrder(userId);

        // Assert
        assertNotNull(result);
        assertEquals(orderId, result.getId());
    }

    @Test
    @DisplayName("获取当前订单 - 无进行中的订单")
    void testGetCurrentOrder_NoActiveOrder() {
        // Arrange
        when(orderRepository.findByUserIdAndOrderStatus(userId, OrderConstants.ORDER_STATUS_CHARGING))
                .thenReturn(Optional.empty());

        // Act
        OrderDetailVO result = orderService.getCurrentOrder(userId);

        // Assert
        assertNull(result);
    }

    // ======================== getOrderList 测试 ========================

    @Test
    @DisplayName("获取订单列表 - 成功场景")
    void testGetOrderList_Success() {
        // Arrange
        List<ChargeOrder> orders = new ArrayList<>();
        orders.add(chargeOrder);
        Page<ChargeOrder> orderPage = new PageImpl<>(orders);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(orderPage);
        when(pileRepository.findAllById(any())).thenReturn(Arrays.asList(chargingPile));
        when(stationRepository.findAllById(any())).thenReturn(Arrays.asList(chargingStation));

        // Act
        Page<OrderDetailVO> result = orderService.getOrderList(userId, null, null, null, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("获取订单列表 - 空列表")
    void testGetOrderList_EmptyList() {
        // Arrange
        Page<ChargeOrder> orderPage = new PageImpl<>(new ArrayList<>());
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(orderPage);

        // Act
        Page<OrderDetailVO> result = orderService.getOrderList(userId, null, null, null, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("获取订单列表 - 按订单状态筛选")
    void testGetOrderList_FilterByStatus() {
        // Arrange
        List<ChargeOrder> orders = new ArrayList<>();
        ChargeOrder completedOrder = chargeOrder.toBuilder().orderStatus(OrderConstants.ORDER_STATUS_COMPLETED).build();
        orders.add(completedOrder);
        Page<ChargeOrder> orderPage = new PageImpl<>(orders);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(orderPage);
        when(pileRepository.findAllById(any())).thenReturn(Arrays.asList(chargingPile));
        when(stationRepository.findAllById(any())).thenReturn(Arrays.asList(chargingStation));

        // Act
        Page<OrderDetailVO> result = orderService.getOrderList(userId, OrderConstants.ORDER_STATUS_COMPLETED, null, null, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
