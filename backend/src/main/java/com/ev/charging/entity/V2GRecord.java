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
@Table(name = "v2g_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class V2GRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "pile_id", nullable = false)
    private Long pileId;
    @Column(name = "station_id", nullable = false)
    private Long stationId;

    @Column(name = "record_type", nullable = false)
    private Byte recordType;  // 1充电 2放电

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "energy_amount", precision = 8, scale = 2)
    private BigDecimal energyAmount;  // 电量(kWh)
    @Column(name = "electricity_price", precision = 5, scale = 2)
    private BigDecimal electricityPrice;  // 实时电价(元/度)
    @Column(name = "amount", precision = 8, scale = 2)
    private BigDecimal amount;  // 金额(元)

    @Column(name = "battery_health_before")
    private Byte batteryHealthBefore;  // 操作前电池健康度
    @Column(name = "battery_health_after")
    private Byte batteryHealthAfter;   // 操作后电池健康度

    @Builder.Default
    @Column(name = "profit", precision = 8, scale = 2)
    private BigDecimal profit = BigDecimal.ZERO;  // 收益(元),仅放电时有效

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}
