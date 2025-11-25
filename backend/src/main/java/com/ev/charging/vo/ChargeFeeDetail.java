package com.ev.charging.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 充电费用明细VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeFeeDetail {

    /**
     * 谷时电量（kWh）
     */
    private BigDecimal valleyAmount;

    /**
     * 谷时费用（元）
     */
    private BigDecimal valleyFee;

    /**
     * 平时电量（kWh）
     */
    private BigDecimal flatAmount;

    /**
     * 平时费用（元）
     */
    private BigDecimal flatFee;

    /**
     * 峰时电量（kWh）
     */
    private BigDecimal peakAmount;

    /**
     * 峰时费用（元）
     */
    private BigDecimal peakFee;

    /**
     * 总电费（元）
     */
    private BigDecimal totalElectricityFee;

    /**
     * 服务费（元）
     */
    private BigDecimal serviceFee;

    /**
     * 总费用（元）
     */
    private BigDecimal totalAmount;

    /**
     * 谷时分钟数
     */
    private Long valleyMinutes;

    /**
     * 平时分钟数
     */
    private Long flatMinutes;

    /**
     * 峰时分钟数
     */
    private Long peakMinutes;

    /**
     * 总充电时长（分钟）
     */
    private Long totalMinutes;
}
