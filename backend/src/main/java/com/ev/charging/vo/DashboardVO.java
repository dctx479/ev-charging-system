package com.ev.charging.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 仪表板统计数据VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardVO {

    /**
     * 今日充电量（kWh）
     */
    private BigDecimal todayChargeAmount;

    /**
     * 今日收入（元）
     */
    private BigDecimal todayIncome;

    /**
     * 在线充电桩数
     */
    private Integer onlinePileCount;

    /**
     * 故障充电桩数
     */
    private Integer faultPileCount;

    /**
     * 充电中的充电桩数
     */
    private Integer chargingPileCount;

    /**
     * 今日订单数
     */
    private Integer todayOrderCount;

    /**
     * 待维修故障数
     */
    private Integer pendingFaultCount;

    /**
     * 紧急故障数
     */
    private Integer urgentFaultCount;

    /**
     * 总充电桩数
     */
    private Integer totalPileCount;

    /**
     * 总站点数
     */
    private Integer totalStationCount;
}
