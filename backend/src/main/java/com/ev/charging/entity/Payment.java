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
 * 支付记录实体
 */
@Entity
@Table(name = "payment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 订单ID
     */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * 订单编号
     */
    @Column(name = "order_no", nullable = false, length = 32)
    private String orderNo;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 支付流水号
     */
    @Column(name = "payment_no", unique = true, length = 32)
    private String paymentNo;

    /**
     * 支付金额（元）
     */
    @Column(name = "payment_amount", precision = 8, scale = 2)
    private BigDecimal amount;

    /**
     * 支付方式：1-微信 2-支付宝 3-余额
     */
    @Column(name = "payment_method", nullable = false)
    private Byte paymentMethod;

    /**
     * 支付状态：0-待支付 1-支付成功 2-支付失败 3-已退款
     */
    @Builder.Default
    @Column(name = "payment_status", nullable = false)
    private Byte paymentStatus = 0;

    /**
     * 支付完成时间
     */
    @Column(name = "paid_time")
    private LocalDateTime paymentTime;

    /**
     * 第三方交易ID
     */
    @Column(name = "transaction_id", length = 64)
    private String transactionId;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 退款金额（元）
     */
    @Column(name = "refund_amount", precision = 8, scale = 2)
    private BigDecimal refundAmount;

    /**
     * 退款时间
     */
    @Column(name = "refund_time")
    private LocalDateTime refundTime;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 乐观锁版本号
     */
    @Column(name = "version")
    private Long version;
}
