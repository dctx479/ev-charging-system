package com.ev.charging.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 积分兑换DTO
 */
@Data
public class RedeemDTO {

    /**
     * 兑换金额（积分）
     */
    @NotNull(message = "兑换金额不能为空")
    @Min(value = 1, message = "兑换金额必须大于0")
    private Integer amount;

    /**
     * 兑换描述
     */
    @NotBlank(message = "兑换描述不能为空")
    private String description;
}
