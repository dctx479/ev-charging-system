package com.ev.charging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 积分记录VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditRecordVO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 积分变动（正为获得，负为消耗）
     */
    private Integer creditChange;

    /**
     * 积分类型：1-充电获得 2-签到 3-兑换消耗 4-活动奖励
     */
    private Byte creditType;

    /**
     * 积分类型文本
     */
    private String creditTypeText;

    /**
     * 描述信息
     */
    private String description;

    /**
     * 变动后余额
     */
    private Integer balanceAfter;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 格式化后的创建时间
     */
    private String createTimeFormatted;

    /**
     * 获取积分类型文本
     */
    public static String getCreditTypeText(Byte creditType) {
        return switch (creditType) {
            case 1 -> "充电获得";
            case 2 -> "每日签到";
            case 3 -> "兑换消耗";
            case 4 -> "活动奖励";
            default -> "其他";
        };
    }
}
