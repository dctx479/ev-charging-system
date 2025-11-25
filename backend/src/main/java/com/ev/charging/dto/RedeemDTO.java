package com.ev.charging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 积分兑换DTO
 */
@Data
@Schema(description = "碳积分兑换请求")
public class RedeemDTO {

    /**
     * 兑换金额（积分）
     */
    @Schema(description = "兑换积分数量", example = "1000", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
    @NotNull(message = "兑换金额不能为空")
    @Min(value = 1, message = "兑换金额必须大于0")
    private Integer amount;

    /**
     * 兑换描述
     */
    @Schema(description = "兑换描述/用途", example = "兑换10元充电优惠券", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "兑换描述不能为空")
    private String description;
}
