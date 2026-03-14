package com.ev.charging.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分商品实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "credit_product")
public class CreditProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 商品名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 商品描述
     */
    @Column(length = 500)
    private String description;

    /**
     * 商品图片URL
     */
    @Column(name = "image_url", length = 255)
    private String imageUrl;

    /**
     * 商品类型: coupon-优惠券, discount-充电折扣, gift-实物礼品
     */
    @Column(nullable = false, length = 20)
    private String category;

    /**
     * 所需积分
     */
    @Column(name = "points_required", nullable = false)
    private Integer pointsRequired;

    /**
     * 库存数量
     */
    @Column(nullable = false)
    private Integer stock;

    /**
     * 已兑换数量
     */
    @Builder.Default
    @Column(name = "exchanged_count")
    private Integer exchangedCount = 0;

    /**
     * 商品价值（元）
     */
    @Column(name = "original_price", precision = 8, scale = 2)
    private BigDecimal originalPrice;

    /**
     * 状态: 1-上架, 0-下架
     */
    @Builder.Default
    @Column(nullable = false)
    private Byte status = 1;

    /**
     * 每人限兑数量，0表示不限
     */
    @Builder.Default
    @Column(name = "limit_per_user")
    private Integer limitPerUser = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateTime;
}
