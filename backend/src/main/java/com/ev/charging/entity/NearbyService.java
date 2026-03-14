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
     * 服务类型：1-餐饮 2-娱乐 3-购物 4-休闲
     */
    @Column(name = "service_type", nullable = false)
    private Byte serviceType;

    /**
     * 距离（米）
     */
    @Column(name = "distance")
    private Integer distance;

    /**
     * 地址
     */
    @Column(name = "address", length = 255)
    private String address;

    /**
     * 联系电话
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 平均消费时间（分钟）
     */
    @Column(name = "avg_consume_time")
    private Integer avgConsumeTime;

    /**
     * 推荐等待时间最小值（分钟）
     */
    @Column(name = "recommended_wait_time_min")
    private Integer recommendedWaitTimeMin;

    /**
     * 推荐等待时间最大值（分钟）
     */
    @Column(name = "recommended_wait_time_max")
    private Integer recommendedWaitTimeMax;

    /**
     * 优惠信息
     */
    @Column(name = "coupon_info", length = 255)
    private String couponInfo;

    /**
     * 评分（1-5分）
     */
    @Column(name = "rating", precision = 2, scale = 1)
    private BigDecimal rating;

    /**
     * 状态：1正常 0下架
     */
    @Builder.Default
    @Column(name = "status", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Byte status = 1;

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
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}
