package com.ev.charging.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnergyStatisticsVO {
    private BigDecimal totalSolarGeneration;
    private BigDecimal totalGridPurchase;
    private BigDecimal totalConsumption;
    private BigDecimal selfUseRate;
    private BigDecimal savedCost;
}
