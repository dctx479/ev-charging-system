package com.ev.charging.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 积分记录VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "碳积分变动记录")
public class CreditRecordVO {

    /**
     * 记录ID
     */
    @Schema(description = "记录ID", example = "1")
    private Long id;

    /**
     * 积分变动（正为获得，负为消耗）
     */
    @Schema(description = "积分变动数量（正数为获得，负数为消耗）", example = "100")
    private Integer creditChange;

    /**
     * 积分类型：1-充电获得 2-签到 3-兑换消耗 4-活动奖励
     */
    @Schema(description = "积分类型", example = "1", allowableValues = {"1", "2", "3", "4"})
    private Byte creditType;

    /**
     * 积分类型文本
     */
    @Schema(description = "积分类型文本描述", example = "充电获得")
    private String creditTypeText;

    /**
     * 描述信息
     */
    @Schema(description = "积分变动详细说明", example = "充电30kWh获得积分")
    private String description;

    /**
     * 变动后余额
     */
    @Schema(description = "变动后积分余额", example = "1500")
    private Integer balanceAfter;

    /**
     * 对应碳减排量(kg)
     */
    @Schema(description = "对应碳减排量(kg)", example = "23.55")
    private BigDecimal carbonReduction;

    /**
     * 创建时间
     */
    @Schema(description = "记录创建时间", example = "2024-01-20 14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 格式化后的创建时间
     */
    @Schema(description = "格式化后的创建时间", example = "2024-01-20 14:30")
    private String createTimeFormatted;

    /**
     * 获取积分类型文本
     */
    public static String getCreditTypeText(Byte creditType) {
        if (creditType == null) return "其他";
        return switch (creditType) {
            case 1 -> "充电获得";
            case 2 -> "每日签到";
            case 3 -> "兑换消耗";
            case 4 -> "活动奖励";
            default -> "其他";
        };
    }
}
