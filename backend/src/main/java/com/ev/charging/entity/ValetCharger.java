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

@Entity
@Table(name = "valet_charger")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValetCharger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联用户ID，用于所有权验证
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 姓名
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * 手机号（唯一）
     */
    @Column(name = "phone", nullable = false, unique = true, length = 11)
    private String phone;

    /**
     * 身份证号（加密）
     */
    @Column(name = "id_card", nullable = false, length = 18)
    private String idCard;

    /**
     * 头像
     */
    @Column(name = "avatar", length = 255)
    private String avatar;

    /**
     * 服务区域
     */
    @Column(name = "service_area", length = 100)
    private String serviceArea;

    /**
     * 驾驶证照片
     */
    @Column(name = "driving_license", length = 255)
    private String drivingLicense;

    /**
     * 服务车辆车牌
     */
    @Column(name = "vehicle_plate", length = 20)
    private String vehiclePlate;

    /**
     * 总订单数
     */
    @Builder.Default
    @Column(name = "total_orders")
    private Integer totalOrders = 0;

    /**
     * 完成订单数
     */
    @Builder.Default
    @Column(name = "completed_orders")
    private Integer completedOrders = 0;

    /**
     * 评分
     */
    @Builder.Default
    @Column(name = "rating", precision = 2, scale = 1)
    private BigDecimal rating = BigDecimal.valueOf(5.0);

    /**
     * 服务价格(元/次)
     */
    @Builder.Default
    @Column(name = "service_price", precision = 6, scale = 2)
    private BigDecimal servicePrice = BigDecimal.valueOf(30.0);

    /**
     * 状态：1在线 2忙碌 3离线
     */
    @Builder.Default
    @Column(name = "status", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Byte status = 1;

    /**
     * 是否认证：1是 0否
     */
    @Builder.Default
    @Column(name = "is_certified", nullable = false, columnDefinition = "TINYINT DEFAULT 0")
    private Byte isCertified = 0;

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
