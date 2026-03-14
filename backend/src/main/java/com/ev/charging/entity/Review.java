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

import java.time.LocalDateTime;

/**
 * 评价表实体类
 * 存储用户对充电站点、充电桩和服务的评价信息
 */
@Entity
@Table(name = "review")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    /**
     * 评价ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 订单ID
     */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 充电桩ID
     */
    @Column(name = "pile_id", nullable = false)
    private Long pileId;

    /**
     * 站点ID
     */
    @Column(name = "station_id", nullable = false)
    private Long stationId;

    /**
     * 评分(1-5)
     */
    @Column(name = "rating", nullable = false)
    private Byte rating;

    /**
     * 服务评分
     */
    @Column(name = "service_rating")
    private Byte serviceRating;

    /**
     * 设施评分
     */
    @Column(name = "facility_rating")
    private Byte facilityRating;

    /**
     * 环境评分
     */
    @Column(name = "environment_rating")
    private Byte environmentRating;

    /**
     * 标签（多个用逗号分隔）
     */
    @Column(name = "tags")
    private String tags;

    /**
     * 评价内容
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * 图片URL（多个用逗号分隔）
     */
    @Column(name = "images", columnDefinition = "TEXT")
    private String images;

    /**
     * 是否匿名：1是 0否
     */
    @Column(name = "is_anonymous")
    private Byte isAnonymous;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
