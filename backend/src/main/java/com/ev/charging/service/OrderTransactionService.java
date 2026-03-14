package com.ev.charging.service;

import com.ev.charging.dto.CreateOrderDTO;
import com.ev.charging.entity.ChargeOrder;
import com.ev.charging.entity.ChargingPile;
import com.ev.charging.entity.QueueRecord;
import com.ev.charging.repository.ChargeOrderRepository;
import com.ev.charging.repository.ChargingPileRepository;
import com.ev.charging.repository.QueueRecordRepository;
import com.ev.charging.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.ev.charging.constant.OrderConstants.ORDER_STATUS_CHARGING;
import static com.ev.charging.constant.OrderConstants.PAYMENT_STATUS_UNPAID;
import static com.ev.charging.service.OrderService.generateOrderNo;

/**
 * 订单事务服务
 *
 * 解决 @Transactional 自调用问题：
 * 由于 Spring AOP 基于代理实现，同一类中方法互相调用不会触发事务代理。
 * 将事务性操作提取到独立的 Service 中，确保事务注解正确生效。
 *
 * @see OrderService#createOrder(Long, CreateOrderDTO) 调用此服务的事务方法
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTransactionService {

    private final ChargeOrderRepository orderRepository;
    private final ChargingPileRepository pileRepository;
    private final QueueRecordRepository queueRecordRepository;
    private final CacheService cacheService;

    /**
     * 事务内创建订单
     * 确保订单创建、充电桩状态更新、排队记录更新在同一事务中
     *
     * @param userId      用户ID
     * @param dto         创建订单DTO
     * @param pile        充电桩实体
     * @param calledQueue 已叫号的排队记录（可选）
     * @return 订单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createOrderInTransaction(Long userId, CreateOrderDTO dto, ChargingPile pile,
                                         Optional<QueueRecord> calledQueue) {
        // 生成订单号
        String orderNo = generateOrderNo();

        // 创建订单
        ChargeOrder order = ChargeOrder.builder()
                .orderNo(orderNo)
                .userId(userId)
                .stationId(pile.getStationId())
                .pileId(dto.getPileId())
                .startTime(LocalDateTime.now())
                .chargeMode(dto.getChargeMode())
                .targetValue(dto.getTargetValue())
                .startSoc(dto.getStartSoc() != null ? dto.getStartSoc().byteValue() : null)
                .orderStatus(ORDER_STATUS_CHARGING) // 充电中
                .paymentStatus(PAYMENT_STATUS_UNPAID) // 未支付
                .chargeAmount(BigDecimal.ZERO)
                .electricityFee(BigDecimal.ZERO)
                .serviceFee(BigDecimal.ZERO)
                .totalFee(BigDecimal.ZERO)
                .build();

        order = orderRepository.save(order);

        // 更新充电桩状态为"充电中"
        pile.setStatus((byte) 2);
        pileRepository.save(pile);

        // 如果有排队记录，更新排队记录中的订单ID
        if (calledQueue.isPresent()) {
            QueueRecord queueRecord = calledQueue.get();
            queueRecord.setOrderId(order.getId());
            queueRecordRepository.save(queueRecord);
        }

        log.info("创建订单成功: orderNo={}, userId={}, pileId={}, fromQueue={}",
                orderNo, userId, dto.getPileId(), calledQueue.isPresent());

        return order.getId();
    }

    /**
     * 从排队系统在事务内创建订单
     *
     * 此方法在事务内执行，确保以下操作的原子性：
     * 1. 创建充电订单
     * 2. 更新充电桩状态为"充电中"
     * 3. 清除充电桩缓存
     *
     * @param userId 用户ID
     * @param pileId 充电桩ID
     * @param pile   充电桩实体对象
     * @return 订单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createOrderFromQueueInTransaction(Long userId, Long pileId, ChargingPile pile) {
        // 生成订单号
        String orderNo = generateOrderNo();

        // 创建订单（使用默认值）
        ChargeOrder order = ChargeOrder.builder()
                .orderNo(orderNo)
                .userId(userId)
                .stationId(pile.getStationId())
                .pileId(pileId)
                .startTime(LocalDateTime.now())
                .chargeMode((byte) 1) // 默认充满模式
                .targetValue(BigDecimal.valueOf(100)) // 默认目标值100%
                .startSoc((byte) 20) // 默认起始电量20%
                .orderStatus(ORDER_STATUS_CHARGING) // 充电中
                .paymentStatus(PAYMENT_STATUS_UNPAID) // 未支付
                .chargeAmount(BigDecimal.ZERO)
                .electricityFee(BigDecimal.ZERO)
                .serviceFee(BigDecimal.ZERO)
                .totalFee(BigDecimal.ZERO)
                .build();

        order = orderRepository.save(order);

        // 更新充电桩状态为"充电中"
        pile.setStatus((byte) 2);
        pileRepository.save(pile);
        cacheService.evictPileCache(pileId);

        log.info("从排队自动创建订单成功: orderNo={}, userId={}, pileId={}", orderNo, userId, pileId);

        return order.getId();
    }
}
