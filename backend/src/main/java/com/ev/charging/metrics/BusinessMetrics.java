package com.ev.charging.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 业务指标监控组件
 * 提供充电系统核心业务指标的监控和记录
 */
@Slf4j
@Component
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;

    // ==================== 订单相关指标 ====================

    /** 订单创建总数 */
    private final Counter orderCreatedCounter;

    /** 订单完成总数 */
    private final Counter orderCompletedCounter;

    /** 订单失败总数 */
    private final Counter orderFailedCounter;

    /** 订单取消总数 */
    private final Counter orderCancelledCounter;

    /** 订单处理时长 */
    private final Timer orderDurationTimer;

    // ==================== 充电桩相关指标 ====================

    /** 在线充电桩数量 */
    private final AtomicLong onlinePilesGauge;

    /** 正在充电的充电桩数量 */
    private final AtomicLong chargingPilesGauge;

    /** 空闲充电桩数量 */
    private final AtomicLong idlePilesGauge;

    /** 故障充电桩数量 */
    private final AtomicLong faultPilesGauge;

    /** 充电桩利用率 */
    private final Gauge utilizationRateGauge;

    // ==================== 用户相关指标 ====================

    /** 活跃用户数 */
    private final Counter activeUsersCounter;

    /** 新注册用户数 */
    private final Counter newUsersCounter;

    // ==================== 业务指标 ====================

    /** 实时营收 */
    private final AtomicLong revenueGauge;

    /** 总充电量 (kWh) */
    private final Counter totalEnergyCounter;

    /** 碳积分发放总数 */
    private final Counter carbonCreditsCounter;

    // ==================== 队列相关指标 ====================

    /** 队列中等待人数 */
    private final AtomicLong queueLengthGauge;

    /** 平均等待时长 */
    private final Timer queueWaitTimer;

    // ==================== 支付相关指标 ====================

    /** 支付成功数 */
    private final Counter paymentSuccessCounter;

    /** 支付失败数 */
    private final Counter paymentFailedCounter;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 初始化订单指标
        this.orderCreatedCounter = Counter.builder("orders.created")
                .description("订单创建总数")
                .tag("type", "charge")
                .register(meterRegistry);

        this.orderCompletedCounter = Counter.builder("orders.completed")
                .description("订单完成总数")
                .tag("type", "charge")
                .register(meterRegistry);

        this.orderFailedCounter = Counter.builder("orders.failed")
                .description("订单失败总数")
                .tag("type", "charge")
                .register(meterRegistry);

        this.orderCancelledCounter = Counter.builder("orders.cancelled")
                .description("订单取消总数")
                .tag("type", "charge")
                .register(meterRegistry);

        this.orderDurationTimer = Timer.builder("orders.duration")
                .description("订单处理时长")
                .tag("type", "charge")
                .register(meterRegistry);

        // 初始化充电桩指标
        this.onlinePilesGauge = new AtomicLong(0);
        Gauge.builder("piles.online", onlinePilesGauge, AtomicLong::get)
                .description("在线充电桩数量")
                .register(meterRegistry);

        this.chargingPilesGauge = new AtomicLong(0);
        Gauge.builder("piles.charging", chargingPilesGauge, AtomicLong::get)
                .description("正在充电的充电桩数量")
                .register(meterRegistry);

        this.idlePilesGauge = new AtomicLong(0);
        Gauge.builder("piles.idle", idlePilesGauge, AtomicLong::get)
                .description("空闲充电桩数量")
                .register(meterRegistry);

        this.faultPilesGauge = new AtomicLong(0);
        Gauge.builder("piles.fault", faultPilesGauge, AtomicLong::get)
                .description("故障充电桩数量")
                .register(meterRegistry);

        // 充电桩利用率 = 正在充电数 / 在线总数
        this.utilizationRateGauge = Gauge.builder("piles.utilization.rate", this,
                BusinessMetrics::calculateUtilizationRate)
                .description("充电桩利用率")
                .register(meterRegistry);

        // 初始化用户指标
        this.activeUsersCounter = Counter.builder("users.active")
                .description("活跃用户数")
                .register(meterRegistry);

        this.newUsersCounter = Counter.builder("users.new")
                .description("新注册用户数")
                .register(meterRegistry);

        // 初始化业务指标
        this.revenueGauge = new AtomicLong(0);
        Gauge.builder("business.revenue", revenueGauge, value -> value.get() / 100.0)
                .description("实时营收(元)")
                .baseUnit("yuan")
                .register(meterRegistry);

        this.totalEnergyCounter = Counter.builder("business.energy.total")
                .description("总充电量(kWh)")
                .baseUnit("kWh")
                .register(meterRegistry);

        this.carbonCreditsCounter = Counter.builder("business.carbon.credits")
                .description("碳积分发放总数")
                .register(meterRegistry);

        // 初始化队列指标
        this.queueLengthGauge = new AtomicLong(0);
        Gauge.builder("queue.length", queueLengthGauge, AtomicLong::get)
                .description("队列中等待人数")
                .register(meterRegistry);

        this.queueWaitTimer = Timer.builder("queue.wait.time")
                .description("平均等待时长")
                .register(meterRegistry);

        // 初始化支付指标
        this.paymentSuccessCounter = Counter.builder("payment.success")
                .description("支付成功数")
                .register(meterRegistry);

        this.paymentFailedCounter = Counter.builder("payment.failed")
                .description("支付失败数")
                .register(meterRegistry);

        log.info("业务指标监控组件初始化完成");
    }

    // ==================== 订单指标方法 ====================

    /**
     * 记录订单创建
     */
    public void recordOrderCreated() {
        orderCreatedCounter.increment();
        log.debug("记录订单创建指标");
    }

    /**
     * 记录订单完成
     */
    public void recordOrderCompleted() {
        orderCompletedCounter.increment();
        log.debug("记录订单完成指标");
    }

    /**
     * 记录订单失败
     */
    public void recordOrderFailed() {
        orderFailedCounter.increment();
        log.warn("记录订单失败指标");
    }

    /**
     * 记录订单取消
     */
    public void recordOrderCancelled() {
        orderCancelledCounter.increment();
        log.debug("记录订单取消指标");
    }

    /**
     * 记录订单处理时长
     * @param milliseconds 处理时长(毫秒)
     */
    public void recordOrderDuration(long milliseconds) {
        orderDurationTimer.record(milliseconds, TimeUnit.MILLISECONDS);
        log.debug("记录订单处理时长: {}ms", milliseconds);
    }

    // ==================== 充电桩指标方法 ====================

    /**
     * 更新充电桩在线数量
     */
    public void updateOnlinePiles(long count) {
        onlinePilesGauge.set(count);
    }

    /**
     * 更新正在充电的充电桩数量
     */
    public void updateChargingPiles(long count) {
        chargingPilesGauge.set(count);
    }

    /**
     * 更新空闲充电桩数量
     */
    public void updateIdlePiles(long count) {
        idlePilesGauge.set(count);
    }

    /**
     * 更新故障充电桩数量
     */
    public void updateFaultPiles(long count) {
        faultPilesGauge.set(count);
    }

    /**
     * 计算充电桩利用率
     */
    private double calculateUtilizationRate() {
        long online = onlinePilesGauge.get();
        long charging = chargingPilesGauge.get();
        return online > 0 ? (double) charging / online : 0.0;
    }

    // ==================== 用户指标方法 ====================

    /**
     * 记录活跃用户
     */
    public void recordActiveUser() {
        activeUsersCounter.increment();
    }

    /**
     * 记录新注册用户
     */
    public void recordNewUser() {
        newUsersCounter.increment();
        log.debug("记录新用户注册指标");
    }

    // ==================== 业务指标方法 ====================

    /**
     * 增加营收
     * @param amountInCents 金额(分)
     */
    public void addRevenue(long amountInCents) {
        revenueGauge.addAndGet(amountInCents);
        log.debug("增加营收: {}分", amountInCents);
    }

    /**
     * 记录充电量
     * @param energyKwh 充电量(kWh)
     */
    public void recordEnergy(double energyKwh) {
        totalEnergyCounter.increment(energyKwh);
        log.debug("记录充电量: {}kWh", energyKwh);
    }

    /**
     * 记录碳积分发放
     * @param credits 积分数量
     */
    public void recordCarbonCredits(int credits) {
        carbonCreditsCounter.increment(credits);
        log.debug("记录碳积分发放: {}", credits);
    }

    // ==================== 队列指标方法 ====================

    /**
     * 更新队列长度
     */
    public void updateQueueLength(long length) {
        queueLengthGauge.set(length);
    }

    /**
     * 记录队列等待时长
     * @param milliseconds 等待时长(毫秒)
     */
    public void recordQueueWaitTime(long milliseconds) {
        queueWaitTimer.record(milliseconds, TimeUnit.MILLISECONDS);
        log.debug("记录队列等待时长: {}ms", milliseconds);
    }

    // ==================== 支付指标方法 ====================

    /**
     * 记录支付成功
     */
    public void recordPaymentSuccess() {
        paymentSuccessCounter.increment();
        log.debug("记录支付成功指标");
    }

    /**
     * 记录支付失败
     */
    public void recordPaymentFailed() {
        paymentFailedCounter.increment();
        log.warn("记录支付失败指标");
    }
}
