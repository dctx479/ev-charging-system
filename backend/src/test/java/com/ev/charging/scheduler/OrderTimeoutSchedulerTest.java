package com.ev.charging.scheduler;

import com.ev.charging.entity.ChargeOrder;
import com.ev.charging.entity.ChargingPile;
import com.ev.charging.repository.ChargeOrderRepository;
import com.ev.charging.repository.ChargingPileRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 订单超时定时任务测试
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
class OrderTimeoutSchedulerTest {

    @Mock
    private ChargeOrderRepository orderRepository;

    @Mock
    private ChargingPileRepository pileRepository;

    @InjectMocks
    private OrderTimeoutScheduler scheduler;

    @BeforeEach
    void setUp() {
        // 设置超时配置
        ReflectionTestUtils.setField(scheduler, "chargingTimeoutHours", 4);
        ReflectionTestUtils.setField(scheduler, "paymentTimeoutHours", 24);
    }

    @Test
    void testCheckChargingTimeout_WithTimeoutOrders() {
        // 准备测试数据
        ChargeOrder order = ChargeOrder.builder()
                .id(1L)
                .orderNo("CO123456")
                .userId(100L)
                .pileId(10L)
                .stationId(1L)
                .startTime(LocalDateTime.now().minusHours(5)) // 5小时前开始充电
                .orderStatus((byte) 2) // 充电中 (ORDER_STATUS_CHARGING = 2)
                .paymentStatus((byte) 0)
                .build();

        ChargingPile pile = ChargingPile.builder()
                .id(10L)
                .pileNo("PILE-001")
                .status((byte) 2) // 充电中
                .build();

        when(orderRepository.findChargingTimeoutOrders(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(order));
        when(pileRepository.findById(10L)).thenReturn(Optional.of(pile));

        // 执行测试
        scheduler.checkChargingTimeout();

        // 验证：订单状态应设为异常(5)，充电桩状态应设为空闲(1)
        verify(orderRepository).save(argThat(o ->
                o.getOrderStatus() == 5 && o.getEndTime() != null
        ));
        verify(pileRepository).save(argThat(p -> p.getStatus() == 1));

        log.info("充电超时检查测试通过");
    }

    @Test
    void testCheckChargingTimeout_NoTimeoutOrders() {
        when(orderRepository.findChargingTimeoutOrders(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        scheduler.checkChargingTimeout();

        verify(orderRepository, never()).save(any());
        verify(pileRepository, never()).save(any());

        log.info("无超时订单测试通过");
    }

    @Test
    void testCheckPaymentTimeout_WithTimeoutOrders() {
        // 准备测试数据
        ChargeOrder order = ChargeOrder.builder()
                .id(2L)
                .orderNo("CO789012")
                .userId(100L)
                .pileId(10L)
                .stationId(1L)
                .startTime(LocalDateTime.now().minusHours(26))
                .endTime(LocalDateTime.now().minusHours(25)) // 25小时前结束
                .orderStatus((byte) 3) // 已完成 (ORDER_STATUS_COMPLETED = 3)
                .paymentStatus((byte) 0) // 未支付
                .totalFee(BigDecimal.valueOf(50.00))
                .build();

        when(orderRepository.findPaymentTimeoutOrders(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(order));

        // 执行测试
        scheduler.checkPaymentTimeout();

        // 验证：订单状态应设为已取消(4)
        verify(orderRepository).save(argThat(o -> o.getOrderStatus() == 4));

        log.info("支付超时检查测试通过");
    }

    @Test
    void testCheckPaymentTimeout_NoTimeoutOrders() {
        when(orderRepository.findPaymentTimeoutOrders(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        scheduler.checkPaymentTimeout();

        verify(orderRepository, never()).save(any());

        log.info("无支付超时订单测试通过");
    }

    @Test
    void testCheckChargingTimeout_MultipleOrders() {
        // 准备多个超时订单
        ChargeOrder order1 = createTimeoutOrder(1L, "CO001", 10L);
        ChargeOrder order2 = createTimeoutOrder(2L, "CO002", 11L);
        ChargeOrder order3 = createTimeoutOrder(3L, "CO003", 12L);

        when(orderRepository.findChargingTimeoutOrders(any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(order1, order2, order3));

        when(pileRepository.findById(anyLong()))
                .thenAnswer(invocation -> {
                    Long pileId = invocation.getArgument(0);
                    return Optional.of(ChargingPile.builder()
                            .id(pileId)
                            .pileNo("PILE-" + pileId)
                            .status((byte) 2)
                            .build());
                });

        // 执行测试
        scheduler.checkChargingTimeout();

        // 验证处理了所有订单
        verify(orderRepository, times(3)).save(any(ChargeOrder.class));
        verify(pileRepository, times(3)).save(any(ChargingPile.class));

        log.info("批量处理超时订单测试通过");
    }

    private ChargeOrder createTimeoutOrder(Long id, String orderNo, Long pileId) {
        return ChargeOrder.builder()
                .id(id)
                .orderNo(orderNo)
                .userId(100L)
                .pileId(pileId)
                .stationId(1L)
                .startTime(LocalDateTime.now().minusHours(5))
                .orderStatus((byte) 2) // 充电中 (ORDER_STATUS_CHARGING = 2)
                .paymentStatus((byte) 0)
                .build();
    }
}
