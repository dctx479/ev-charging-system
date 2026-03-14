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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "energy_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnergyData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_id", nullable = false)
    private Long stationId;
    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;

    @Column(name = "solar_generation", precision = 8, scale = 2)
    private BigDecimal solarGeneration;  // 光伏发电量(kWh)
    @Column(name = "grid_purchase", precision = 8, scale = 2)
    private BigDecimal gridPurchase;     // 电网购电量(kWh)
    @Column(name = "battery_charge", precision = 8, scale = 2)
    private BigDecimal batteryCharge;    // 储能充电量(kWh)
    @Column(name = "battery_discharge", precision = 8, scale = 2)
    private BigDecimal batteryDischarge; // 储能放电量(kWh)
    @Column(name = "battery_soc")
    private Byte batterySoc;             // 储能电池电量%
    @Column(name = "total_consumption", precision = 8, scale = 2)
    private BigDecimal totalConsumption; // 总消耗电量(kWh)

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}
