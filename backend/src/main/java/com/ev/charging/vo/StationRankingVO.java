package com.ev.charging.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 站点排行VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationRankingVO {

    private Long stationId;
    private String stationName;
    private BigDecimal totalIncome;
    private BigDecimal totalChargeAmount;
    private Integer totalOrderCount;
}
