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
@Table(name = "charging_piles")
public class ChargingPile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 充电桩编号（唯一）
     */
    @Column(nullable = false, unique = true, length = 50)
    private String pileCode;

    /**
     * 所属充电站ID
     */
    @Column(nullable = false)
    private Long stationId;

    /**
     * 充电桩名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 充电桩类型：DC-直流，AC-交流
     */
    @Column(nullable = false, length = 20)
    private String pileType;

    /**
     * 额定功率（kW）
     */
    @Column(nullable = false)
    private Double ratedPower;

    /**
     * 当前功率（kW）
     */
    @Column
    private Double currentPower = 0.0;

    /**
     * 充电价格（元/kWh）
     */
    @Column(nullable = false)
    private Double price;

    /**
     * 充电桩状态：AVAILABLE-可用，CHARGING-充电中，FAULT-故障，MAINTENANCE-维护中，OFFLINE-离线
     */
    @Column(nullable = false, length = 20)
    private String status = "AVAILABLE";

    /**
     * 电压（V）
     */
    @Column
    private Double voltage;

    /**
     * 电流（A）
     */
    @Column
    private Double current;

    /**
     * 温度（℃）
     */
    @Column
    private Double temperature;

    /**
     * 累计充电次数
     */
    @Column
    private Integer totalChargingTimes = 0;

    /**
     * 累计充电电量（kWh）
     */
    @Column
    private Double totalChargingEnergy = 0.0;

    /**
     * 最后维护时间
     */
    @Column
    private LocalDateTime lastMaintenanceTime;

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
