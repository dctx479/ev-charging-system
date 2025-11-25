package com.ev.charging.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 充电站实体类
 */
@Data
@Entity
@Table(name = "charging_stations")
public class ChargingStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 充电站名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 充电站地址
     */
    @Column(nullable = false, length = 200)
    private String address;

    /**
     * 经度
     */
    @Column(nullable = false)
    private Double longitude;

    /**
     * 纬度
     */
    @Column(nullable = false)
    private Double latitude;

    /**
     * 联系电话
     */
    @Column(length = 20)
    private String phone;

    /**
     * 营业时间
     */
    @Column(length = 50)
    private String businessHours;

    /**
     * 总充电桩数量
     */
    @Column(nullable = false)
    private Integer totalPiles = 0;

    /**
     * 可用充电桩数量
     */
    @Column(nullable = false)
    private Integer availablePiles = 0;

    /**
     * 充电站状态：ACTIVE-营业中，MAINTENANCE-维护中，CLOSED-已关闭
     */
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    /**
     * 充电站图片URL
     */
    @Column(length = 500)
    private String imageUrl;

    /**
     * 充电站描述
     */
    @Column(length = 500)
    private String description;

    /**
     * 评分（0-5）
     */
    @Column
    private Double rating = 0.0;

    /**
     * 评价数量
     */
    @Column
    private Integer reviewCount = 0;

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
