package com.ev.charging.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充电订单实体
 */
@Entity
@Table(name = "charge_order")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 订单号
     */
    @Column(name = "order_no", unique = true, nullable = false, length = 32)
    private String orderNo;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 充电站ID
     */
    @Column(name = "station_id", nullable = false)
    private Long stationId;

    /**
     * 充电桩ID
     */
    @Column(name = "pile_id", nullable = false)
    private Long pileId;

    /**
     * 开始充电时间
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /**
     * 结束充电时间
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * 充电时长（分钟）
     */
    @Column(name = "charge_duration")
    private Integer chargeDuration;

    /**
     * 充电量（kWh）
     */
    @Column(name = "charge_amount", precision = 10, scale = 2)
    private BigDecimal chargeAmount;

    /**
     * 电费（元）
     */
    @Column(name = "electricity_fee", precision = 10, scale = 2)
    private BigDecimal electricityFee;

    /**
     * 服务费（元）
     */
    @Column(name = "service_fee", precision = 10, scale = 2)
    private BigDecimal serviceFee;

    /**
     * 总费用（元）
     */
    @Column(name = "total_fee", precision = 10, scale = 2)
    private BigDecimal totalFee;

    /**
     * 支付状态：0-未支付 1-已支付 2-已退款
     */
    @Column(name = "payment_status", nullable = false)
    private Byte paymentStatus;

    /**
     * 支付方式：1-微信 2-支付宝 3-余额
     */
    @Column(name = "payment_method")
    private Byte paymentMethod;

    /**
     * 支付时间
     */
    @Column(name = "payment_time")
    private LocalDateTime paymentTime;

    /**
     * 订单状态：0-进行中 1-已完成 2-已取消 3-异常
     */
    @Column(name = "order_status", nullable = false)
    private Byte orderStatus;

    /**
     * 开始SOC（%）
     */
    @Column(name = "start_soc")
    private Integer startSoc;

    /**
     * 结束SOC（%）
     */
    @Column(name = "end_soc")
    private Integer endSoc;

    /**
     * 充电模式：1-充满 2-按金额 3-按电量 4-按时间
     */
    @Column(name = "charge_mode")
    private Byte chargeMode;

    /**
     * 目标值（根据充电模式：金额/电量/时长）
     */
    @Column(name = "target_value", precision = 10, scale = 2)
    private BigDecimal targetValue;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
