package com.ev.charging.service;

import com.ev.charging.constant.OrderConstants;
import com.ev.charging.dto.CreateOrderDTO;
import com.ev.charging.entity.ChargeOrder;
import com.ev.charging.entity.ChargingPile;
import com.ev.charging.entity.ChargingStation;
import com.ev.charging.entity.CreditPendingRecord;
import com.ev.charging.entity.Payment;
import com.ev.charging.entity.QueueRecord;
import com.ev.charging.repository.ChargeOrderRepository;
import com.ev.charging.repository.ChargingPileRepository;
import com.ev.charging.repository.ChargingStationRepository;
import com.ev.charging.repository.CreditPendingRecordRepository;
import com.ev.charging.repository.PaymentRepository;
import com.ev.charging.repository.QueueRecordRepository;
import com.ev.charging.vo.ChargeFeeDetail;
import com.ev.charging.vo.OrderDetailVO;
import com.ev.charging.util.RedisLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

import static com.ev.charging.constant.OrderConstants.*;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
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

    @Autowired
    private com.ev.charging.mq.producer.MessageProducer messageProducer;

    @Autowired
    private QueueRecordRepository queueRecordRepository;

    @Autowired
    private CreditPendingRecordRepository creditPendingRecordRepository;

    @Autowired
    private RedisLockService redisLockService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private OrderTransactionService orderTransactionService;

    // 排队状态常量
    private static final byte QUEUE_STATUS_QUEUING = 1;    // 排队中
    private static final byte QUEUE_STATUS_CALLED = 2;     // 已叫号
    private static final byte QUEUE_STATUS_EXPIRED = 4;    // 已过号
    private static final byte QUEUE_STATUS_CANCELLED = 3;  // 已取消

    // 电价常量（元/kWh）
    private static final BigDecimal PRICE_VALLEY = new BigDecimal("0.4");   // 谷时电价
    private static final BigDecimal PRICE_FLAT = new BigDecimal("0.8");     // 平时电价
    private static final BigDecimal PRICE_PEAK = new BigDecimal("1.2");     // 峰时电价
    private static final BigDecimal SERVICE_FEE_PER_KWH = new BigDecimal("0.5"); // 服务费（元/kWh）
    private static final BigDecimal MAX_CHARGE_CAPACITY = new BigDecimal("200"); // 安全上限（kWh）

    /**
     * 创建订单（开始充电）- 支持排队系统集成
     * 使用分布式锁防止充电桩被并发占用
     * 优化：将锁检查移到事务外，减少事务持有时间
     * 并发安全：锁释放在事务提交后（afterCompletion回调）
     */
    public Long createOrder(Long userId, CreateOrderDTO dto) {
        // 使用分布式锁防止充电桩被并发占用（事务外获取锁）
        // 使用owner-safe锁，防止锁过期后其他线程的锁被误删
        String lockKey = RedisLockService.buildPileLockKey(dto.getPileId());
        String lockOwner = redisLockService.tryLockWithOwner(lockKey, 30);
        if (lockOwner == null) {
            throw new RuntimeException("充电桩正在被占用，请稍后重试");
        }

        try {
            // 在事务外进行预检查（减少事务持有时间）
            ChargingPile pile = pileRepository.findById(dto.getPileId())
                    .orElseThrow(() -> new RuntimeException("充电桩不存在: ID=" + dto.getPileId()));

            Optional<ChargeOrder> existingOrder = orderRepository.findByUserIdAndOrderStatus(userId, ORDER_STATUS_CHARGING);
            if (existingOrder.isPresent()) {
                throw new RuntimeException("您有正在进行的充电订单，请先结束后再开始新的充电");
            }

            Optional<QueueRecord> calledQueue = queueRecordRepository.findByUserIdAndQueueStatusIn(
                    userId, Arrays.asList(QUEUE_STATUS_CALLED)
            );

            if (calledQueue.isPresent()) {
                QueueRecord queueRecord = calledQueue.get();
                if (!queueRecord.getAssignedPileId().equals(dto.getPileId())) {
                    throw new RuntimeException("您已被叫号，请使用分配的充电桩：" +
                            pileRepository.findById(queueRecord.getAssignedPileId())
                                    .map(ChargingPile::getPileNo)
                                    .orElse("未知"));
                }
            } else {
                if (pile.getStatus() == null || pile.getStatus() != 1) {
                    throw new RuntimeException("充电桩不可用，当前状态：" + getStatusText(pile.getStatus()) +
                            "。如需使用，请先加入排队");
                }

                long queueCount = queueRecordRepository.countByStationIdAndQueueStatus(
                        pile.getStationId(), QUEUE_STATUS_QUEUING
                );
                if (queueCount > 0) {
                    throw new RuntimeException("当前站点有" + queueCount + "人排队，请先加入排队");
                }
            }

            Optional<ChargeOrder> pileOrder = orderRepository.findByPileIdAndOrderStatus(dto.getPileId(), ORDER_STATUS_CHARGING);
            if (pileOrder.isPresent()) {
                throw new RuntimeException("该充电桩正在使用中");
            }

            // 预检查通过后，调用独立的事务服务进行数据修改
            // 注意：使用独立Service是为了解决@Transactional自调用问题
            Long orderId = orderTransactionService.createOrderInTransaction(userId, dto, pile, calledQueue);

            // 注册锁释放回调，确保在事务提交后释放锁
            // 使用unlockSafe确保只释放自己持有的锁，防止锁过期后误删其他线程的锁
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCompletion(int status) {
                                redisLockService.unlockSafe(lockKey, lockOwner);
                                log.debug("事务完成后释放创建订单的分布式锁: lockKey={}, status={}", lockKey, status);
                            }
                        }
                );
            }

            return orderId;

        } catch (Exception e) {
            // 异常时立即释放锁，不等待事务回调
            redisLockService.unlockSafe(lockKey, lockOwner);
            throw e;
        }
    }

    /**
     * 从排队系统自动创建订单（叫号后自动创建）
     * 此方法由QueueService调用，不需要前端DTO
     *
     * 并发安全设计：
     * - 使用分布式锁防止充电桩被并发占用
     * - 锁在事务提交后释放，确保其他线程不会读到未提交数据
     * - 使用 TransactionSynchronizationManager 注册回调，在事务提交成功后释放锁
     *
     * @param userId 用户ID
     * @param pileId 充电桩ID
     * @return 订单ID
     */
    public Long createOrderFromQueue(Long userId, Long pileId) {
        // 获取分布式锁，防止并发创建（事务外）
        // 使用owner-safe锁，防止锁过期后其他线程的锁被误删
        String pileLockKey = RedisLockService.buildPileLockKey(pileId);
        String pileOwner = redisLockService.tryLockWithOwner(pileLockKey, 30);
        if (pileOwner == null) {
            throw new RuntimeException("充电桩正在被占用，请稍后重试");
        }

        // 用户级锁，防止同一用户并发创建多个订单
        String userLockKey = RedisLockService.buildUserQueueLockKey(userId);
        String userOwner = redisLockService.tryLockWithOwner(userLockKey, 30);
        if (userOwner == null) {
            redisLockService.unlockSafe(pileLockKey, pileOwner);
            throw new RuntimeException("您的操作过于频繁，请稍后重试");
        }

        try {
            // 在事务外进行预检查
            ChargingPile pile = pileRepository.findById(pileId)
                    .orElseThrow(() -> new RuntimeException("充电桩不存在: ID=" + pileId));

            // 2. 检查用户是否有进行中的订单
            Optional<ChargeOrder> existingOrder = orderRepository.findByUserIdAndOrderStatus(userId, ORDER_STATUS_CHARGING);
            if (existingOrder.isPresent()) {
                throw new RuntimeException("用户已有正在进行的充电订单: userId=" + userId + ", orderId=" + existingOrder.get().getId());
            }

            // 3. 检查充电桩是否被占用
            Optional<ChargeOrder> pileOrder = orderRepository.findByPileIdAndOrderStatus(pileId, ORDER_STATUS_CHARGING);
            if (pileOrder.isPresent()) {
                throw new RuntimeException("该充电桩正在使用中: pileId=" + pileId + ", orderId=" + pileOrder.get().getId());
            }

            // 调用事务服务进行创建操作
            Long orderId = orderTransactionService.createOrderFromQueueInTransaction(userId, pileId, pile);

            // 在事务提交成功后，注册锁释放回调
            // 使用unlockSafe确保只释放自己持有的锁
            registerLockReleaseCallback(pileLockKey, pileOwner, userLockKey, userOwner);

            return orderId;

        } catch (Exception e) {
            // 异常时立即释放锁
            redisLockService.unlockSafe(pileLockKey, pileOwner);
            redisLockService.unlockSafe(userLockKey, userOwner);
            throw e;
        }
    }

    /**
     * 注册事务同步回调，确保在事务提交后释放分布式锁
     * 使用owner-safe释放，防止误删其他线程持有的锁
     *
     * @param pileLockKey  充电桩锁键
     * @param pileOwner    充电桩锁标识
     * @param userLockKey  用户锁键
     * @param userOwner    用户锁标识
     */
    private void registerLockReleaseCallback(String pileLockKey, String pileOwner,
                                             String userLockKey, String userOwner) {
        // 只在事务进行中时才能注册同步
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            // 事务提交成功后释放锁
                            redisLockService.unlockSafe(pileLockKey, pileOwner);
                            redisLockService.unlockSafe(userLockKey, userOwner);
                            log.debug("事务提交后释放分布式锁: pileLockKey={}, userLockKey={}", pileLockKey, userLockKey);
                        }

                        @Override
                        public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED) {
                                redisLockService.unlockSafe(pileLockKey, pileOwner);
                                redisLockService.unlockSafe(userLockKey, userOwner);
                                log.debug("事务非提交状态下释放分布式锁: status={}", status);
                            }
                        }
                    }
            );
        } else {
            // 不在事务中时，直接释放锁（不应该走到这里，但保险起见）
            log.warn("不在事务同步上下文中，直接释放锁");
            redisLockService.unlockSafe(pileLockKey, pileOwner);
            redisLockService.unlockSafe(userLockKey, userOwner);
        }
    }

    /**
     * 结束充电
     * 安全修复: 移除 actualChargeAmount 参数，所有费用计算在服务端完成
     * 防止客户端篡改金额的安全漏洞
     * 并发安全: MQ消息发送在事务提交后（afterCommit回调）
     */
    @Transactional(rollbackFor = Exception.class)
    public void endCharging(Long orderId, Long userId, Integer endSoc) {
        // 1. 查询订单
        ChargeOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在: orderId=" + orderId));

        // 安全检查: 验证订单所有权，防止IDOR漏洞
        if (!order.getUserId().equals(userId)) {
            log.warn("IDOR攻击尝试: userId={} 试图结束 orderId={} (实际归属 userId={})",
                    userId, orderId, order.getUserId());
            throw new RuntimeException("无权操作此订单");
        }

        if (order.getOrderStatus() == null || order.getOrderStatus() != ORDER_STATUS_CHARGING) {
            throw new RuntimeException("订单状态异常，无法结束充电: orderId=" + orderId +
                    ", currentStatus=" + order.getOrderStatus());
        }

        // 2. 验证endSoc合法性
        if (endSoc == null || endSoc < 0 || endSoc > 100) {
            throw new RuntimeException("结束电量百分比无效: endSoc=" + endSoc + "，必须在0-100之间");
        }

        Byte startSoc = order.getStartSoc();
        if (startSoc == null || startSoc < 0 || startSoc > 100) {
            throw new RuntimeException("起始电量异常: startSoc=" + startSoc);
        }

        if (endSoc <= startSoc.intValue()) {
            throw new RuntimeException("结束电量必须大于起始电量: startSoc=" + startSoc +
                    ", endSoc=" + endSoc);
        }

        // 3. 计算充电时长
        LocalDateTime endTime = LocalDateTime.now();
        order.setEndTime(endTime);
        Duration duration = Duration.between(order.getStartTime(), endTime);
        int chargeDuration = (int) duration.toMinutes();
        order.setActualDuration(chargeDuration);

        // 4. 服务端计算充电量：基于充电桩实际功率和充电时长
        ChargingPile pile = pileRepository.findById(order.getPileId())
                .orElseThrow(() -> new RuntimeException("充电桩不存在: pileId=" + order.getPileId()));

        BigDecimal serverChargeAmount = pile.getPower()
                .multiply(BigDecimal.valueOf(chargeDuration))
                .divide(BigDecimal.valueOf(60.0), 2, RoundingMode.HALF_UP);
        // 安全上限：充电量不超过合理电池容量(200kWh，覆盖大部分EV车型)
        if (serverChargeAmount.compareTo(MAX_CHARGE_CAPACITY) > 0) {
            log.warn("充电量超过安全上限，截断: 计算值={}, 上限={}", serverChargeAmount, MAX_CHARGE_CAPACITY);
            serverChargeAmount = MAX_CHARGE_CAPACITY;
        }
        if (serverChargeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            serverChargeAmount = BigDecimal.valueOf(0.01); // 最小0.01 kWh
        }

        order.setEndSoc(endSoc != null ? endSoc.byteValue() : null);
        order.setChargeAmount(serverChargeAmount);
        // effectively final copy for use in anonymous inner class (TransactionSynchronization callback)
        final BigDecimal finalChargeAmount = serverChargeAmount;

        // 5. 计算费用（峰谷平电价）- 完全在服务端进行，不接受客户端任何金额参数
        BigDecimal electricityFee = calculateElectricityFee(order.getStartTime(), endTime, serverChargeAmount);
        BigDecimal serviceFee = serverChargeAmount.multiply(SERVICE_FEE_PER_KWH).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalFee = electricityFee.add(serviceFee);

        order.setElectricityFee(electricityFee);
        order.setServiceFee(serviceFee);
        order.setTotalFee(totalFee);

        // 6. 更新订单状态为"已完成"
        order.setOrderStatus(ORDER_STATUS_COMPLETED);

        orderRepository.save(order);

        // 7. 更新充电桩状态为"空闲"
        pile.setStatus((byte) 1);
        pileRepository.save(pile);
        cacheService.evictPileCache(pile.getId());

        // Fix 2: 发送订单完成事件在事务提交后（afterCommit回调）
        // 这样可以保证MQ消息只在事务提交成功后才发送
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                com.ev.charging.mq.event.OrderCompletedEvent event = com.ev.charging.mq.event.OrderCompletedEvent.builder()
                                        .orderId(order.getId())
                                        .orderNo(order.getOrderNo())
                                        .userId(order.getUserId())
                                        .stationId(order.getStationId())
                                        .pileId(order.getPileId())
                                        .chargeAmount(finalChargeAmount)
                                        .totalFee(totalFee)
                                        .electricityFee(electricityFee)
                                        .serviceFee(serviceFee)
                                        .chargeDuration(chargeDuration)
                                        .startTime(order.getStartTime())
                                        .endTime(endTime)
                                        .build();
                                messageProducer.sendOrderCompletedMessage(event);
                                log.info("事务提交后发送订单完成事件: orderId={}", order.getId());
                            } catch (Exception e) {
                                log.error("发送订单完成事件失败: orderId={}", orderId, e);
                            }
                        }
                    }
            );
        } else {
            // 不在事务中时，直接发送（不应该走到这里）
            try {
                com.ev.charging.mq.event.OrderCompletedEvent event = com.ev.charging.mq.event.OrderCompletedEvent.builder()
                        .orderId(order.getId())
                        .orderNo(order.getOrderNo())
                        .userId(order.getUserId())
                        .stationId(order.getStationId())
                        .pileId(order.getPileId())
                        .chargeAmount(finalChargeAmount)
                        .totalFee(totalFee)
                        .electricityFee(electricityFee)
                        .serviceFee(serviceFee)
                        .chargeDuration(chargeDuration)
                        .startTime(order.getStartTime())
                        .endTime(endTime)
                        .build();
                messageProducer.sendOrderCompletedMessage(event);
            } catch (Exception e) {
                log.error("发送订单完成事件失败: orderId={}", orderId, e);
            }
        }

        log.info("结束充电: orderId={}, chargeDuration={}分钟, chargeAmount={}kWh, electricityFee={}元, serviceFee={}元, totalFee={}元",
                orderId, chargeDuration, serverChargeAmount, electricityFee, serviceFee, totalFee);
    }

    /**
     * 计算电费（峰谷平电价 - O(1) 时段区间交集算法）
     * 谷时（23:00-07:00）: 0.4元/kWh
     * 平时（07:00-10:00, 15:00-18:00, 21:00-23:00）: 0.8元/kWh
     * 峰时（10:00-15:00, 18:00-21:00）: 1.2元/kWh
     */
    private BigDecimal calculateElectricityFee(LocalDateTime startTime, LocalDateTime endTime, BigDecimal totalAmount) {
        long totalMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
        if (totalMinutes <= 0) {
            return BigDecimal.ZERO;
        }

        // 定义一天内各时段区间（分钟，从0点算起）
        // Valley: [0,420) [1380,1440)  Flat: [420,600) [900,1080) [1260,1380)  Peak: [600,900) [1080,1260)
        long valleyMinutes = 0, flatMinutes = 0, peakMinutes = 0;

        // 按整天 + 零头 处理，对每天最多遍历7个区间（O(days)，通常≤1）
        LocalDateTime cursor = startTime;
        while (cursor.isBefore(endTime)) {
            LocalDateTime dayStart = cursor.toLocalDate().atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);
            // 本天内的充电区段
            LocalDateTime segStart = cursor;
            LocalDateTime segEnd = endTime.isBefore(dayEnd) ? endTime : dayEnd;
            long segStartMin = ChronoUnit.MINUTES.between(dayStart, segStart);
            long segEndMin   = ChronoUnit.MINUTES.between(dayStart, segEnd);

            // 各时段定义：[start, end) 单位=分钟
            long[][] periods = {{0,420},{420,600},{600,900},{900,1080},{1080,1260},{1260,1380},{1380,1440}};
            String[] types   = {"valley","flat","peak","flat","peak","flat","valley"};
            for (int i = 0; i < periods.length; i++) {
                long lo = Math.max(segStartMin, periods[i][0]);
                long hi = Math.min(segEndMin,   periods[i][1]);
                if (hi > lo) {
                    switch (types[i]) {
                        case "valley" -> valleyMinutes += hi - lo;
                        case "flat"   -> flatMinutes   += hi - lo;
                        case "peak"   -> peakMinutes   += hi - lo;
                    }
                }
            }
            cursor = dayEnd;
        }

        // 按各时段分钟占比分配充电量
        BigDecimal valleyAmount = totalAmount.multiply(BigDecimal.valueOf(valleyMinutes))
                .divide(BigDecimal.valueOf(totalMinutes), 4, RoundingMode.HALF_UP);
        BigDecimal flatAmount = totalAmount.multiply(BigDecimal.valueOf(flatMinutes))
                .divide(BigDecimal.valueOf(totalMinutes), 4, RoundingMode.HALF_UP);
        BigDecimal peakAmount = totalAmount.multiply(BigDecimal.valueOf(peakMinutes))
                .divide(BigDecimal.valueOf(totalMinutes), 4, RoundingMode.HALF_UP);

        BigDecimal valleyFee = valleyAmount.multiply(PRICE_VALLEY).setScale(2, RoundingMode.HALF_UP);
        BigDecimal flatFee   = flatAmount.multiply(PRICE_FLAT).setScale(2, RoundingMode.HALF_UP);
        BigDecimal peakFee   = peakAmount.multiply(PRICE_PEAK).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalFee  = valleyFee.add(flatFee).add(peakFee);

        log.info("电费计算明细: 总时长={}分钟, 总电量={}kWh | 谷时:{}分钟,{}元 | 平时:{}分钟,{}元 | 峰时:{}分钟,{}元 | 总电费:{}元",
                totalMinutes, totalAmount, valleyMinutes, valleyFee, flatMinutes, flatFee, peakMinutes, peakFee, totalFee);

        return totalFee;
    }

    /**
     * 计算详细的电费明细（包含各时段分解）
     * 可用于前端展示详细账单
     */
    public ChargeFeeDetail calculateChargeFeeDetail(LocalDateTime startTime, LocalDateTime endTime, BigDecimal totalAmount) {
        long totalMinutes = ChronoUnit.MINUTES.between(startTime, endTime);

        if (totalMinutes <= 0) {
            return ChargeFeeDetail.builder()
                    .valleyAmount(BigDecimal.ZERO).valleyFee(BigDecimal.ZERO)
                    .flatAmount(BigDecimal.ZERO).flatFee(BigDecimal.ZERO)
                    .peakAmount(BigDecimal.ZERO).peakFee(BigDecimal.ZERO)
                    .totalElectricityFee(BigDecimal.ZERO).serviceFee(BigDecimal.ZERO)
                    .totalAmount(BigDecimal.ZERO)
                    .valleyMinutes(0L).flatMinutes(0L).peakMinutes(0L).totalMinutes(0L)
                    .build();
        }

        // 使用 O(1) 时段区间交集算法（与 calculateElectricityFee 保持一致）
        long valleyMinutes = 0, flatMinutes = 0, peakMinutes = 0;
        long[][] periodDefs = {{0,420},{420,600},{600,900},{900,1080},{1080,1260},{1260,1380},{1380,1440}};
        String[] types      = {"valley","flat","peak","flat","peak","flat","valley"};

        LocalDateTime cursor = startTime;
        while (cursor.isBefore(endTime)) {
            LocalDateTime dayStart = cursor.toLocalDate().atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);
            LocalDateTime segStart = cursor;
            LocalDateTime segEnd = endTime.isBefore(dayEnd) ? endTime : dayEnd;
            long sMin = ChronoUnit.MINUTES.between(dayStart, segStart);
            long eMin = ChronoUnit.MINUTES.between(dayStart, segEnd);
            for (int i = 0; i < periodDefs.length; i++) {
                long lo = Math.max(sMin, periodDefs[i][0]);
                long hi = Math.min(eMin, periodDefs[i][1]);
                if (hi > lo) {
                    switch (types[i]) {
                        case "valley" -> valleyMinutes += hi - lo;
                        case "flat"   -> flatMinutes   += hi - lo;
                        case "peak"   -> peakMinutes   += hi - lo;
                    }
                }
            }
            cursor = dayEnd;
        }

        BigDecimal valleyAmount = totalAmount.multiply(BigDecimal.valueOf(valleyMinutes))
                .divide(BigDecimal.valueOf(totalMinutes), 4, RoundingMode.HALF_UP);
        BigDecimal flatAmount = totalAmount.multiply(BigDecimal.valueOf(flatMinutes))
                .divide(BigDecimal.valueOf(totalMinutes), 4, RoundingMode.HALF_UP);
        BigDecimal peakAmount = totalAmount.multiply(BigDecimal.valueOf(peakMinutes))
                .divide(BigDecimal.valueOf(totalMinutes), 4, RoundingMode.HALF_UP);

        BigDecimal valleyFee = valleyAmount.multiply(PRICE_VALLEY).setScale(2, RoundingMode.HALF_UP);
        BigDecimal flatFee   = flatAmount.multiply(PRICE_FLAT).setScale(2, RoundingMode.HALF_UP);
        BigDecimal peakFee   = peakAmount.multiply(PRICE_PEAK).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalElectricityFee = valleyFee.add(flatFee).add(peakFee);
        BigDecimal serviceFee = totalAmount.multiply(SERVICE_FEE_PER_KWH).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalFeeAmount = totalElectricityFee.add(serviceFee);

        return ChargeFeeDetail.builder()
                .valleyAmount(valleyAmount.setScale(2, RoundingMode.HALF_UP)).valleyFee(valleyFee)
                .flatAmount(flatAmount.setScale(2, RoundingMode.HALF_UP)).flatFee(flatFee)
                .peakAmount(peakAmount.setScale(2, RoundingMode.HALF_UP)).peakFee(peakFee)
                .totalElectricityFee(totalElectricityFee).serviceFee(serviceFee).totalAmount(totalFeeAmount)
                .valleyMinutes(valleyMinutes).flatMinutes(flatMinutes).peakMinutes(peakMinutes)
                .totalMinutes(totalMinutes)
                .build();
    }

    /**
     * 获取订单列表（分页）
     * 优化：批量查询关联数据，避免N+1查询问题
     */
    public Page<OrderDetailVO> getOrderList(Long userId, Byte orderStatus, Byte paymentStatus, String orderNo, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));

        Specification<ChargeOrder> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("userId"), userId));
            if (orderStatus != null) {
                predicates.add(cb.equal(root.get("orderStatus"), orderStatus));
            }
            if (paymentStatus != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), paymentStatus));
            }
            if (orderNo != null && !orderNo.trim().isEmpty()) {
                predicates.add(cb.like(root.get("orderNo"), "%" + orderNo.trim() + "%"));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<ChargeOrder> orderPage = orderRepository.findAll(spec, pageable);

        // 批量查询关联数据，避免N+1查询问题
        return convertToVOBatch(orderPage);
    }

    /**
     * 获取订单详情
     * 优化：添加明确的异常信息
     */
    public OrderDetailVO getOrderDetail(Long orderId) {
        ChargeOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在: orderId=" + orderId));

        // 批量查询关联数据
        ChargingPile pile = pileRepository.findById(order.getPileId())
                .orElse(null);
        ChargingStation station = stationRepository.findById(order.getStationId())
                .orElse(null);

        return convertToVO(order, pile, station);
    }

    /**
     * 获取当前进行中的订单
     */
    public OrderDetailVO getCurrentOrder(Long userId) {
        Optional<ChargeOrder> orderOpt = orderRepository.findByUserIdAndOrderStatus(userId, ORDER_STATUS_CHARGING);

        if (orderOpt.isEmpty()) {
            return null;
        }

        ChargeOrder order = orderOpt.get();
        // 批量查询关联数据
        ChargingPile pile = pileRepository.findById(order.getPileId())
                .orElse(null);
        ChargingStation station = stationRepository.findById(order.getStationId())
                .orElse(null);

        return convertToVO(order, pile, station);
    }

    /**
     * 取消订单
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId) {
        ChargeOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在: orderId=" + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权取消此订单: orderId=" + orderId + ", userId=" + userId);
        }

        if (order.getOrderStatus() == null || order.getOrderStatus() != ORDER_STATUS_CHARGING) {
            throw new RuntimeException("只能取消进行中的订单: orderId=" + orderId +
                    ", currentStatus=" + order.getOrderStatus());
        }

        // 更新订单状态为"已取消"
        order.setOrderStatus(ORDER_STATUS_CANCELLED);
        order.setEndTime(LocalDateTime.now());
        orderRepository.save(order);

        // 更新充电桩状态为"空闲"
        ChargingPile pile = pileRepository.findById(order.getPileId())
                .orElseThrow(() -> new RuntimeException("充电桩不存在: pileId=" + order.getPileId()));
        pile.setStatus((byte) 1);
        pileRepository.save(pile);
        cacheService.evictPileCache(pile.getId());

        log.info("取消订单: orderId={}, userId={}", orderId, userId);
    }

    /**
     * 支付订单（模拟支付）
     * 并发安全：使用原子化的支付状态更新（CAS操作），防止并发支付
     */
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId, Long userId, Byte paymentMethod) {
        ChargeOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在: orderId=" + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权支付此订单: orderId=" + orderId + ", userId=" + userId);
        }

        if (order.getOrderStatus() == null || order.getOrderStatus() != ORDER_STATUS_COMPLETED) {
            throw new RuntimeException("只能支付已完成的订单: orderId=" + orderId +
                    ", currentStatus=" + order.getOrderStatus());
        }

        // Fix 3: 原子性检查和更新支付状态，防止并发重复支付
        // 只有当前支付状态为未支付(0)时，才能更新为已支付(1)
        // 这避免了读-检查-写之间的竞态条件（race condition）
        Byte expectedPayStatus = PAYMENT_STATUS_UNPAID; // 期望状态：未支付
        Byte newPayStatus = PAYMENT_STATUS_PAID;        // 新状态：已支付

        int updated = orderRepository.atomicUpdatePayStatus(orderId, newPayStatus, expectedPayStatus);

        if (updated == 0) {
            // 原子更新失败：可能是重复支付或订单状态异常
            // 重新查询当前状态以提供更好的错误信息
            ChargeOrder currentOrder = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("订单不存在: orderId=" + orderId));
            if (currentOrder.getPaymentStatus() != null && currentOrder.getPaymentStatus() == PAYMENT_STATUS_PAID) {
                throw new RuntimeException("订单已支付: orderId=" + orderId);
            }
            throw new RuntimeException("订单支付状态异常: orderId=" + orderId + ", currentStatus=" + currentOrder.getPaymentStatus());
        }

        // 原子更新成功后，创建支付记录
        // 生成全局唯一支付流水号
        String paymentNo = "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 创建支付记录
        Payment payment = Payment.builder()
                .orderId(orderId)
                .orderNo(order.getOrderNo())
                .userId(userId)
                .paymentNo(paymentNo)
                .amount(order.getTotalFee())
                .paymentMethod(paymentMethod)
                .paymentStatus(PAYMENT_STATUS_PAID) // 支付成功
                .paymentTime(LocalDateTime.now())
                .transactionId("TXN" + System.currentTimeMillis()) // 模拟第三方交易ID
                .build();

        paymentRepository.save(payment);

        // 更新订单支付方式和时间（支付状态已通过原子操作更新）
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(LocalDateTime.now());
        order.setPaymentStatus(PAYMENT_STATUS_PAID); // 同步内存中的对象以保持一致性
        orderRepository.save(order);

        // 发送订单支付事件在事务提交后（afterCommit回调）
        // 确保MQ消息只在事务提交成功后才发送，避免事务回滚后消费者处理到脏数据
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                com.ev.charging.mq.event.OrderPaidEvent paidEvent = com.ev.charging.mq.event.OrderPaidEvent.builder()
                                        .orderId(orderId)
                                        .orderNo(order.getOrderNo())
                                        .userId(userId)
                                        .amount(order.getTotalFee())
                                        .paymentMethod(paymentMethod)
                                        .paymentNo(paymentNo)
                                        .chargeAmount(order.getChargeAmount())
                                        .paymentTime(LocalDateTime.now())
                                        .build();
                                messageProducer.sendOrderPaidMessage(paidEvent);
                                log.info("事务提交后发送订单支付事件，积分将异步发放: orderId={}", orderId);
                            } catch (Exception e) {
                                log.error("发送订单支付事件失败，积分将无法异步发放: orderId={}", orderId, e);
                            }
                        }
                    }
            );
        } else {
            // 不在事务中时，直接发送（不应该走到这里，因为方法标记了@Transactional）
            log.warn("payOrder不在事务同步上下文中，直接发送MQ消息: orderId={}", orderId);
            try {
                com.ev.charging.mq.event.OrderPaidEvent paidEvent = com.ev.charging.mq.event.OrderPaidEvent.builder()
                        .orderId(orderId)
                        .orderNo(order.getOrderNo())
                        .userId(userId)
                        .amount(order.getTotalFee())
                        .paymentMethod(paymentMethod)
                        .paymentNo(paymentNo)
                        .chargeAmount(order.getChargeAmount())
                        .paymentTime(LocalDateTime.now())
                        .build();
                messageProducer.sendOrderPaidMessage(paidEvent);
                log.info("订单支付事件已发送，积分将异步发放: orderId={}", orderId);
            } catch (Exception e) {
                log.error("发送订单支付事件失败，积分将无法异步发放: orderId={}", orderId, e);
            }
        }

        log.info("支付订单成功: orderId={}, paymentNo={}, amount={}", orderId, paymentNo, order.getTotalFee());
    }

    /**
     * 转换为VO（带关联数据）
     * 用于单个订单查询，避免N+1问题
     */
    private OrderDetailVO convertToVO(ChargeOrder order, ChargingPile pile, ChargingStation station) {
        return OrderDetailVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .stationName(station != null ? station.getName() : "未知站点")
                .pileNo(pile != null ? pile.getPileNo() : "未知充电桩")
                .pilePower(pile != null && pile.getPower() != null ? pile.getPower() : BigDecimal.ZERO)
                .startTime(order.getStartTime())
                .endTime(order.getEndTime())
                .chargeDuration(order.getActualDuration())
                .chargeAmount(order.getChargeAmount())
                .electricityFee(order.getElectricityFee())
                .serviceFee(order.getServiceFee())
                .totalFee(order.getTotalFee())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentTime(order.getPaymentTime())
                .orderStatus(order.getOrderStatus())
                .startSoc(order.getStartSoc() != null ? order.getStartSoc().intValue() : null)
                .endSoc(order.getEndSoc() != null ? order.getEndSoc().intValue() : null)
                .chargeMode(order.getChargeMode())
                .targetValue(order.getTargetValue())
                .createTime(order.getCreateTime())
                .updateTime(order.getUpdateTime())
                .carbonCredits(order.getCarbonCreditEarned())
                .pileId(order.getPileId())
                .build();
    }

    /**
     * 批量转换为VO（优化N+1查询）
     * 用于订单列表查询，一次性批量加载所有关联数据
     */
    private Page<OrderDetailVO> convertToVOBatch(Page<ChargeOrder> orderPage) {
        if (orderPage.isEmpty()) {
            return Page.empty();
        }

        // 1. 收集所有pileId和stationId
        java.util.Set<Long> pileIds = orderPage.getContent().stream()
                .map(ChargeOrder::getPileId)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Set<Long> stationIds = orderPage.getContent().stream()
                .map(ChargeOrder::getStationId)
                .collect(java.util.stream.Collectors.toSet());

        // 2. 批量查询所有关联的pile和station（只执行2次查询，而不是N次）
        java.util.Map<Long, ChargingPile> pileMap = pileRepository.findAllById(pileIds).stream()
                .collect(java.util.stream.Collectors.toMap(ChargingPile::getId, p -> p));

        java.util.Map<Long, ChargingStation> stationMap = stationRepository.findAllById(stationIds).stream()
                .collect(java.util.stream.Collectors.toMap(ChargingStation::getId, s -> s));

        // 3. 转换为VO
        return orderPage.map(order -> {
            ChargingPile pile = pileMap.get(order.getPileId());
            ChargingStation station = stationMap.get(order.getStationId());
            return convertToVO(order, pile, station);
        });
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
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "离线";
            case 1 -> "空闲";
            case 2 -> "充电中";
            case 3 -> "预约中";
            case 4 -> "故障";
            default -> "未知";
        };
    }

    /**
     * 保存积分待发放记录
     *
     * @param orderId      订单ID
     * @param userId       用户ID
     * @param chargeAmount 充电量
     * @param errorMessage 错误信息
     */
    private void savePendingCreditRecord(Long orderId, Long userId, BigDecimal chargeAmount, String errorMessage) {
        try {
            // 检查是否已存在待发放记录
            Optional<CreditPendingRecord> existingRecord = creditPendingRecordRepository.findByOrderId(orderId);
            if (existingRecord.isPresent()) {
                log.warn("订单 {} 的积分待发放记录已存在，跳过创建", orderId);
                return;
            }

            CreditPendingRecord record = CreditPendingRecord.builder()
                    .orderId(orderId)
                    .userId(userId)
                    .chargeAmount(chargeAmount)
                    .retryCount(0)
                    .lastError(errorMessage != null && errorMessage.length() > 500
                            ? errorMessage.substring(0, 500)
                            : errorMessage)
                    .status((byte) 0) // 待重试
                    .build();

            creditPendingRecordRepository.save(record);
            log.info("已创建积分待发放记录: orderId={}, userId={}, chargeAmount={}", orderId, userId, chargeAmount);
        } catch (Exception e) {
            log.error("保存积分待发放记录失败: orderId={}", orderId, e);
        }
    }
}
