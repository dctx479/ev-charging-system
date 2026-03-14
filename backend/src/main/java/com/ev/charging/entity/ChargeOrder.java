package com.ev.charging.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
     * 预测充电时长（分钟）
     */
    @Column(name = "predicted_duration")
    private Integer predictedDuration;

    /**
     * 实际充电时长（分钟） - Maps to actual_duration in DDL
     */
    @Column(name = "actual_duration")
    private Integer actualDuration;

    /**
     * 充电量（kWh） - DECIMAL(8,2) in DDL
     */
    @Column(name = "charge_amount", precision = 8, scale = 2)
    private BigDecimal chargeAmount;

    /**
     * 电费（元） - DECIMAL(8,2) in DDL
     */
    @Column(name = "electricity_fee", precision = 8, scale = 2)
    private BigDecimal electricityFee;

    /**
     * 服务费（元） - DECIMAL(8,2) in DDL
     */
    @Column(name = "service_fee", precision = 8, scale = 2)
    private BigDecimal serviceFee;

    /**
     * 停车费（元） - DECIMAL(8,2) in DDL
     */
    @Column(name = "parking_fee", precision = 8, scale = 2)
    private BigDecimal parkingFee;

    /**
     * 总费用（元） - DECIMAL(8,2) in DDL
     */
    @Column(name = "total_fee", precision = 8, scale = 2)
    private BigDecimal totalFee;

    /**
     * 碳减排量（kg）
     */
    @Column(name = "carbon_reduction", precision = 6, scale = 2)
    private BigDecimal carbonReduction;

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
     * 订单状态：1-待支付 2-充电中 3-已完成 4-已取消 5-异常 (参见OrderConstants)
     */
    @Column(name = "order_status", nullable = false)
    private Byte orderStatus;

    /**
     * 开始SOC（%）- TINYINT in DDL
     */
    @Column(name = "start_soc")
    private Byte startSoc;

    /**
     * 结束SOC（%）- TINYINT in DDL
     */
    @Column(name = "end_soc")
    private Byte endSoc;

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
     * 是否V2G放电：1是 0否 - TINYINT in DDL
     */
    @Column(name = "is_v2g")
    private Byte isV2g;

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

    /**
     * 乐观锁版本号
     */
    @Version
    @Column(name = "version")
    private Integer version;

    /**
     * 碳积分获得数量
     */
    @Column(name = "carbon_credit_earned")
    private Integer carbonCreditEarned;
}
