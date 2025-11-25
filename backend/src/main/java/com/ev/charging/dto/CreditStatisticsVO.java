package com.ev.charging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "碳积分统计信息")
public class CreditStatisticsVO {

    /**
     * 当前积分余额
     */
    @Schema(description = "当前积分余额", example = "2500")
    private Integer currentBalance;

    /**
     * 累计获得积分
     */
    @Schema(description = "累计获得的总积分", example = "5000")
    private Integer totalEarned;

    /**
     * 累计消耗积分
     */
    @Schema(description = "累计消耗的总积分", example = "2500")
    private Integer totalSpent;

    /**
     * 本月获得积分
     */
    @Schema(description = "本月获得积分", example = "300")
    private Integer monthEarned;

    /**
     * 本月消耗积分
     */
    @Schema(description = "本月消耗积分", example = "100")
    private Integer monthSpent;

    /**
     * 连续签到天数
     */
    @Schema(description = "连续签到天数", example = "7")
    private Integer continuousCheckInDays;

    /**
     * 是否已签到
     */
    @Schema(description = "今日是否已签到", example = "true")
    private Boolean hasCheckedInToday;
}
