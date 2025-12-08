package com.ev.charging.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 充电桩实体类
 */
@Data
@Entity
@Table(name = "charging_pile")
public class ChargingPile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 充电桩编号（唯一）
     */
    @Column(name = "pile_no", nullable = false, unique = true, length = 50)
    private String pileNo;

    /**
     * 所属充电站ID
     */
    @Column(nullable = false)
    private Long stationId;

    /**
     * 充电桩名称
     */
    @Column(name = "pile_name", length = 100)
    private String pileName;

    /**
     * 充电桩类型：1快充 2慢充 3超充
     */
    @Column(name = "pile_type", nullable = false)
    private Byte pileType;

    /**
     * 额定功率（kW）
     */
    @Column(name = "power", nullable = false)
    private Double power;

    /**
     * 额定电压（V）
     */
    @Column
    private Integer voltage;

    /**
     * 额定电流（A）
     */
    @Column
    private Double current;

    /**
     * 接口类型：国标/欧标/特斯拉等
     */
    @Column(name = "connector_type", length = 50)
    private String connectorType;

    /**
     * 峰时电价（元/度）
     */
    @Column(name = "price_peak")
    private Double pricePeak;

    /**
     * 平时电价（元/度）
     */
    @Column(name = "price_flat")
    private Double priceFlat;

    /**
     * 谷时电价（元/度）
     */
    @Column(name = "price_valley")
    private Double priceValley;

    /**
     * 服务费（元/度）
     */
    @Column(name = "service_fee")
    private Double serviceFee = 0.0;

    /**
     * 充电桩状态：1空闲 2充电中 3预约中 4故障 5离线
     */
    @Column(nullable = false)
    private Byte status = 1;

    /**
     * 二维码URL
     */
    @Column(name = "qr_code", length = 255)
    private String qrCode;

    /**
     * 累计充电次数
     */
    @Column(name = "total_charge_count")
    private Integer totalChargeCount = 0;

    /**
     * 累计充电电量（kWh）
     */
    @Column(name = "total_charge_amount")
    private Double totalChargeAmount = 0.0;

    /**
     * 健康度评分（0-100）
     */
    @Column(name = "health_score")
    private Byte healthScore = 100;

    /**
     * 最后维护时间
     */
    @Column(name = "last_maintenance_time")
    private LocalDateTime lastMaintenanceTime;

    /**
     * 是否支持V2G：1是 0否
     */
    @Column(name = "support_v2g")
    private Byte supportV2g = 0;

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
