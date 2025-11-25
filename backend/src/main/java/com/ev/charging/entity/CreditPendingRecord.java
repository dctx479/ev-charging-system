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
 * 积分待发放记录实体类
 * 用于记录积分发放失败的订单，支持自动重试和补偿机制
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "credit_pending_record")
public class CreditPendingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 订单ID
     */
    @Column(nullable = false)
    private Long orderId;

    /**
     * 用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 充电量(kWh) - 用于计算积分
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal chargeAmount;

    /**
     * 重试次数
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer retryCount = 0;

    /**
     * 最后一次错误信息
     */
    @Column(length = 500)
    private String lastError;

    /**
     * 状态：0-待重试 1-已成功 2-已放弃
     */
    @Builder.Default
    @Column(nullable = false)
    private Byte status = 0;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateTime;
}
