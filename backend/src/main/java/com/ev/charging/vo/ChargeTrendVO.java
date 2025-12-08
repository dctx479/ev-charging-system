package com.ev.charging.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 充电趋势VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeTrendVO {

    /**
     * 日期列表
     */
    private List<String> dates;

    /**
     * 充电量列表（kWh）
     */
    private List<BigDecimal> chargeAmounts;

    /**
     * 收入列表（元）
     */
    private List<BigDecimal> incomes;

    /**
     * 订单数列表
     */
    private List<Integer> orderCounts;
}
