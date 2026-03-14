package com.ev.charging.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充电站实体类
 */
@Data
@Entity
@Table(name = "charging_station")
public class ChargingStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 运营商ID
     */
    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    /**
     * 运营商关系（延迟加载）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", insertable = false, updatable = false)
    private Operator operator;

    /**
     * 充电站名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 省份
     */
    @Column(nullable = false, length = 50)
    private String province;

    /**
     * 城市
     */
    @Column(nullable = false, length = 50)
    private String city;

    /**
     * 区县
     */
    @Column(length = 50)
    private String district;

    /**
     * 充电站地址
     */
    @Column(nullable = false, length = 255)
    private String address;

    /**
     * 经度
     */
    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    /**
     * 纬度
     */
    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    /**
     * 停车费(元/小时)
     */
    @Column(name = "parking_fee", precision = 5, scale = 2)
    private BigDecimal parkingFee = BigDecimal.ZERO;

    /**
     * 营业时间
     */
    @Column(name = "business_hours", length = 50)
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
     * 充电站状态：1-营业中，2-维护中，0-关闭
     */
    @Column(nullable = false)
    private Byte status = 1;

    /**
     * 充电站图片URL
     * NOT persisted to database - transient field for API responses
     */
    @Transient
    private String imageUrl;

    /**
     * 充电站描述
     * NOT persisted to database - transient field for API responses
     */
    @Transient
    private String description;

    /**
     * 评分（0-5）
     * NOT persisted to database - calculated from reviews table
     */
    @Transient
    private Double rating = 0.0;

    /**
     * 评价数量
     * NOT persisted to database - calculated from reviews table
     */
    @Transient
    private Integer reviewCount = 0;

    /**
     * 是否有光伏设施 0-无 1-有
     */
    @Column(name = "has_solar")
    private Byte hasSolar = 0;

    /**
     * 是否有储能设施 0-无 1-有
     */
    @Column(name = "has_storage")
    private Byte hasStorage = 0;

    /**
     * 光伏装机容量(kW)
     */
    @Column(name = "solar_capacity", precision = 8, scale = 2)
    private BigDecimal solarCapacity;

    /**
     * 储能容量(kWh)
     */
    @Column(name = "storage_capacity", precision = 8, scale = 2)
    private BigDecimal storageCapacity;

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
