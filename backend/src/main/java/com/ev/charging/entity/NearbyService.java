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
 * 周边服务实体
 */
@Entity
@Table(name = "nearby_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 充电站ID
     */
    @Column(name = "station_id", nullable = false)
    private Long stationId;

    /**
     * 服务名称
     */
    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    /**
     * 服务类型：1-餐饮 2-咖啡厅 3-购物 4-娱乐 5-其他
     */
    @Column(name = "service_type", nullable = false)
    private Byte serviceType;

    /**
     * 服务描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 距离（米）
     */
    @Column(name = "distance")
    private Integer distance;

    /**
     * 平均消费时间（分钟）
     */
    @Column(name = "avg_consume_time")
    private Integer avgConsumeTime;

    /**
     * 评分（1-5分）
     */
    @Column(name = "rating", precision = 3, scale = 1)
    private BigDecimal rating;

    /**
     * 地址
     */
    @Column(name = "address", length = 200)
    private String address;

    /**
     * 联系电话
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 营业时间
     */
    @Column(name = "business_hours", length = 100)
    private String businessHours;

    /**
     * 优惠信息
     */
    @Column(name = "coupon_info", length = 200)
    private String couponInfo;

    /**
     * 经度
     */
    @Column(name = "longitude", precision = 10, scale = 6)
    private BigDecimal longitude;

    /**
     * 纬度
     */
    @Column(name = "latitude", precision = 10, scale = 6)
    private BigDecimal latitude;

    /**
     * 状态：1正常 0下架
     */
    @Column(name = "status", nullable = false)
    private Byte status;

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
