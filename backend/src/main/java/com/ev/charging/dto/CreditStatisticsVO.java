package com.ev.charging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 积分统计VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditStatisticsVO {

    /**
     * 当前积分余额
     */
    private Integer currentBalance;

    /**
     * 累计获得积分
     */
    private Integer totalEarned;

    /**
     * 累计消耗积分
     */
    private Integer totalSpent;

    /**
     * 本月获得积分
     */
    private Integer monthEarned;

    /**
     * 本月消耗积分
     */
    private Integer monthSpent;

    /**
     * 连续签到天数
     */
    private Integer continuousCheckInDays;

    /**
     * 是否已签到
     */
    private Boolean hasCheckedInToday;
}
