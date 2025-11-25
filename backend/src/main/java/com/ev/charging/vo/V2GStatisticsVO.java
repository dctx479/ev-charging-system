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
public class V2GStatisticsVO {
    private BigDecimal totalProfit;  // 总收益
    private BigDecimal totalDischargeAmount;  // 总放电量
    private BigDecimal averagePrice;  // 平均电价

    // 管理端统计字段
    private BigDecimal todayDischarge;  // 今日放电量
    private BigDecimal todayIncome;     // 今日收益
    private Long dischargeCount;        // 放电次数（已完成）
    private Long userCount;             // 参与用户数
}
