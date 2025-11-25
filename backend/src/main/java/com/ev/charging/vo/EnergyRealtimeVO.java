package com.ev.charging.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnergyRealtimeVO {
    private BigDecimal solarGeneration;
    private BigDecimal gridPurchase;
    private BigDecimal batteryCharge;
    private BigDecimal batteryDischarge;
    private Byte batterySoc;
    private BigDecimal totalConsumption;
    private BigDecimal solarCapacity;
    private BigDecimal storageCapacity;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTime;
}
