package com.ev.charging.service;

import com.ev.charging.dto.CreateOrderDTO;
import com.ev.charging.entity.ChargeOrder;
import com.ev.charging.entity.ChargingPile;
import com.ev.charging.entity.ChargingStation;
import com.ev.charging.entity.Payment;
import com.ev.charging.repository.ChargeOrderRepository;
import com.ev.charging.repository.ChargingPileRepository;
import com.ev.charging.repository.ChargingStationRepository;
import com.ev.charging.repository.PaymentRepository;
import com.ev.charging.vo.OrderDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 订单服务
 */
@Service
@Slf4j
public class OrderService {

    @Autowired
    private ChargeOrderRepository orderRepository;

    @Autowired
    private ChargingPileRepository pileRepository;

    @Autowired
    private ChargingStationRepository stationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CarbonCreditService carbonCreditService;

    /**
     * 创建订单（开始充电）
     */
    @Transactional
    public Long createOrder(Long userId, CreateOrderDTO dto) {
        // 1. 检查充电桩状态
        ChargingPile pile = pileRepository.findById(dto.getPileId())
                .orElseThrow(() -> new RuntimeException("充电桩不存在"));

        if (pile.getStatus() != 1) {
            throw new RuntimeException("充电桩不可用，当前状态：" + getStatusText(pile.getStatus()));
        }

        // 2. 检查用户是否有进行中的订单
        Optional<ChargeOrder> existingOrder = orderRepository.findByUserIdAndOrderStatus(userId, (byte) 0);
        if (existingOrder.isPresent()) {
            throw new RuntimeException("您有正在进行的充电订单，请先结束后再开始新的充电");
        }

        // 3. 检查充电桩是否被占用
        Optional<ChargeOrder> pileOrder = orderRepository.findByPileIdAndOrderStatus(dto.getPileId(), (byte) 0);
        if (pileOrder.isPresent()) {
            throw new RuntimeException("该充电桩正在使用中");
        }

        // 4. 生成订单号
        String orderNo = generateOrderNo();

        // 5. 创建订单
        ChargeOrder order = ChargeOrder.builder()
                .orderNo(orderNo)
                .userId(userId)
                .stationId(pile.getStationId())
                .pileId(dto.getPileId())
                .startTime(LocalDateTime.now())
                .chargeMode(dto.getChargeMode())
                .targetValue(dto.getTargetValue())
                .startSoc(dto.getStartSoc())
                .orderStatus((byte) 0) // 进行中
                .paymentStatus((byte) 0) // 未支付
                .chargeAmount(BigDecimal.ZERO)
                .electricityFee(BigDecimal.ZERO)
                .serviceFee(BigDecimal.ZERO)
                .totalFee(BigDecimal.ZERO)
                .build();

        order = orderRepository.save(order);

        // 6. 更新充电桩状态为"充电中"
        pile.setStatus((byte) 2);
        pileRepository.save(pile);

        log.info("创建订单成功: orderNo={}, userId={}, pileId={}", orderNo, userId, dto.getPileId());

        return order.getId();
    }

    /**
     * 结束充电
     */
    @Transactional
    public void endCharging(Long orderId, Integer endSoc, BigDecimal actualChargeAmount) {
        // 1. 查询订单
        ChargeOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (order.getOrderStatus() != 0) {
            throw new RuntimeException("订单状态异常，无法结束充电");
        }

        // 2. 计算充电时长
        LocalDateTime endTime = LocalDateTime.now();
        order.setEndTime(endTime);
        Duration duration = Duration.between(order.getStartTime(), endTime);
        int chargeDuration = (int) duration.toMinutes();
        order.setChargeDuration(chargeDuration);

        // 3. 设置结束SOC和实际充电量
        order.setEndSoc(endSoc);
        order.setChargeAmount(actualChargeAmount);

        // 4. 计算费用（峰谷平电价）
        BigDecimal electricityFee = calculateElectricityFee(order.getStartTime(), endTime, actualChargeAmount);
        BigDecimal serviceFee = actualChargeAmount.multiply(BigDecimal.valueOf(0.5)).setScale(2, RoundingMode.HALF_UP); // 服务费：0.5元/kWh
        BigDecimal totalFee = electricityFee.add(serviceFee);

        order.setElectricityFee(electricityFee);
        order.setServiceFee(serviceFee);
        order.setTotalFee(totalFee);

        // 5. 更新订单状态为"已完成"
        order.setOrderStatus((byte) 1);

        orderRepository.save(order);

        // 6. 更新充电桩状态为"空闲"
        ChargingPile pile = pileRepository.findById(order.getPileId()).orElseThrow();
        pile.setStatus((byte) 1);
        pileRepository.save(pile);

        log.info("结束充电: orderId={}, chargeDuration={}分钟, chargeAmount={}kWh, totalFee={}元",
                orderId, chargeDuration, actualChargeAmount, totalFee);
    }

    /**
     * 计算电费（峰谷平电价）
     * 谷时（23:00-07:00）: 0.4元/kWh
     * 平时（07:00-10:00, 15:00-18:00, 21:00-23:00）: 0.8元/kWh
     * 峰时（10:00-15:00, 18:00-21:00）: 1.2元/kWh
     */
    private BigDecimal calculateElectricityFee(LocalDateTime startTime, LocalDateTime endTime, BigDecimal totalAmount) {
        // 简化计算：基于开始时间所在时段的电价
        LocalTime startTimeOfDay = startTime.toLocalTime();
        BigDecimal pricePerKwh = getPriceByTime(startTimeOfDay);

        // 更精确的方式是按分钟计算不同时段的电量，这里为简化处理采用开始时间的电价
        BigDecimal fee = totalAmount.multiply(pricePerKwh).setScale(2, RoundingMode.HALF_UP);

        log.debug("计算电费: startTime={}, pricePerKwh={}, totalAmount={}, fee={}",
                startTime, pricePerKwh, totalAmount, fee);

        return fee;
    }

    /**
     * 根据时间获取电价
     */
    private BigDecimal getPriceByTime(LocalTime time) {
        int hour = time.getHour();

        // 谷时（23:00-07:00）
        if (hour >= 23 || hour < 7) {
            return BigDecimal.valueOf(0.4);
        }
        // 峰时（10:00-15:00, 18:00-21:00）
        else if ((hour >= 10 && hour < 15) || (hour >= 18 && hour < 21)) {
            return BigDecimal.valueOf(1.2);
        }
        // 平时（07:00-10:00, 15:00-18:00, 21:00-23:00）
        else {
            return BigDecimal.valueOf(0.8);
        }
    }

    /**
     * 获取订单列表（分页）
     */
    public Page<OrderDetailVO> getOrderList(Long userId, Byte orderStatus, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChargeOrder> orderPage;

        if (orderStatus != null) {
            orderPage = orderRepository.findByUserIdAndOrderStatusOrderByCreateTimeDesc(userId, orderStatus, pageable);
        } else {
            orderPage = orderRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable);
        }

        return orderPage.map(this::convertToVO);
    }

    /**
     * 获取订单详情
     */
    public OrderDetailVO getOrderDetail(Long orderId) {
        ChargeOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        return convertToVO(order);
    }

    /**
     * 取消订单
     */
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        ChargeOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权取消此订单");
        }

        if (order.getOrderStatus() != 0) {
            throw new RuntimeException("只能取消进行中的订单");
        }

        // 更新订单状态为"已取消"
        order.setOrderStatus((byte) 2);
        order.setEndTime(LocalDateTime.now());
        orderRepository.save(order);

        // 更新充电桩状态为"空闲"
        ChargingPile pile = pileRepository.findById(order.getPileId()).orElseThrow();
        pile.setStatus((byte) 1);
        pileRepository.save(pile);

        log.info("取消订单: orderId={}, userId={}", orderId, userId);
    }

    /**
     * 支付订单（模拟支付）
     */
    @Transactional
    public void payOrder(Long orderId, Long userId, Byte paymentMethod) {
        ChargeOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权支付此订单");
        }

        if (order.getPaymentStatus() == 1) {
            throw new RuntimeException("订单已支付");
        }

        if (order.getOrderStatus() != 1) {
            throw new RuntimeException("只能支付已完成的订单");
        }

        // 生成支付流水号
        String paymentNo = "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);

        // 创建支付记录
        Payment payment = Payment.builder()
                .orderId(orderId)
                .paymentNo(paymentNo)
                .amount(order.getTotalFee())
                .paymentMethod(paymentMethod)
                .paymentStatus((byte) 1) // 支付成功
                .paymentTime(LocalDateTime.now())
                .transactionId("TXN" + System.currentTimeMillis()) // 模拟第三方交易ID
                .build();

        paymentRepository.save(payment);

        // 更新订单支付状态
        order.setPaymentStatus((byte) 1);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(LocalDateTime.now());
        orderRepository.save(order);

        // 7. 支付成功后发放碳积分
        try {
            Integer credits = carbonCreditService.addCreditsForCharging(
                    orderId,
                    userId,
                    order.getChargeAmount()
            );
            log.info("订单支付成功，发放碳积分: orderId={}, credits={}", orderId, credits);
        } catch (Exception e) {
            log.error("发放碳积分失败: orderId={}", orderId, e);
            // 碳积分发放失败不影响支付成功，只记录日志
        }

        log.info("支付订单成功: orderId={}, paymentNo={}, amount={}", orderId, paymentNo, order.getTotalFee());
    }

    /**
     * 转换为VO
     */
    private OrderDetailVO convertToVO(ChargeOrder order) {
        ChargingPile pile = pileRepository.findById(order.getPileId()).orElse(null);
        ChargingStation station = stationRepository.findById(order.getStationId()).orElse(null);

        return OrderDetailVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .stationName(station != null ? station.getName() : "未知站点")
                .pileNo(pile != null ? pile.getPileNo() : "未知充电桩")
                .pilePower(pile != null ? BigDecimal.valueOf(pile.getPower()) : BigDecimal.ZERO)
                .startTime(order.getStartTime())
                .endTime(order.getEndTime())
                .chargeDuration(order.getChargeDuration())
                .chargeAmount(order.getChargeAmount())
                .electricityFee(order.getElectricityFee())
                .serviceFee(order.getServiceFee())
                .totalFee(order.getTotalFee())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentTime(order.getPaymentTime())
                .orderStatus(order.getOrderStatus())
                .startSoc(order.getStartSoc())
                .endSoc(order.getEndSoc())
                .chargeMode(order.getChargeMode())
                .targetValue(order.getTargetValue())
                .createTime(order.getCreateTime())
                .updateTime(order.getUpdateTime())
                .build();
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "CO" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    /**
     * 获取充电桩状态文本
     */
    private String getStatusText(Byte status) {
        return switch (status) {
            case 0 -> "离线";
            case 1 -> "空闲";
            case 2 -> "充电中";
            case 3 -> "预约中";
            case 4 -> "故障";
            default -> "未知";
        };
    }
}
