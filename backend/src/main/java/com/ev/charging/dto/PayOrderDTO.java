package com.ev.charging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 支付订单请求DTO
 */
@Data
@Schema(description = "支付订单请求")
public class PayOrderDTO {

    @Schema(description = "支付方式（1微信 2支付宝 3余额）", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "支付方式不能为空")
    @Min(value = 1, message = "支付方式无效")
    @Max(value = 3, message = "支付方式无效")
    private Byte paymentMethod;
}
