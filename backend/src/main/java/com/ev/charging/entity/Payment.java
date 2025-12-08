package com.ev.charging.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

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
     * 支付流水号
     */
    @Column(name = "payment_no", unique = true, length = 64)
    private String paymentNo;

    /**
     * 支付金额（元）
     */
    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    /**
     * 支付方式：1-微信 2-支付宝 3-余额
     */
    @Column(name = "payment_method", nullable = false)
    private Byte paymentMethod;

    /**
     * 支付状态：0-待支付 1-支付成功 2-支付失败 3-已退款
     */
    @Column(name = "payment_status", nullable = false)
    private Byte paymentStatus;

    /**
     * 支付完成时间
     */
    @Column(name = "payment_time")
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
}
