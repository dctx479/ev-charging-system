package com.ev.charging.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 积分兑换记录实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "credit_exchange_record")
public class CreditExchangeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 商品ID
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * 商品名称（冗余存储）
     */
    @Column(name = "product_name", length = 100)
    private String productName;

    /**
     * 消耗积分
     */
    @Column(name = "points_used", nullable = false)
    private Integer pointsUsed;

    /**
     * 兑换数量
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer quantity = 1;

    /**
     * 状态: 0-待处理, 1-已完成, 2-已取消
     */
    @Builder.Default
    @Column(nullable = false)
    private Byte status = 0;

    /**
     * 收货地址（实物礼品需要）
     */
    @Column(length = 255)
    private String address;

    /**
     * 收货人
     */
    @Column(name = "receiver_name", length = 50)
    private String receiverName;

    /**
     * 收货电话
     */
    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    /**
     * 备注
     */
    @Column(length = 255)
    private String remark;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;
}
