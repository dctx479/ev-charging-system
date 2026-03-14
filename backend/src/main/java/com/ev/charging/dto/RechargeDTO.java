package com.ev.charging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 充值请求DTO
 */
@Data
@Schema(description = "充值请求")
public class RechargeDTO {

    @Schema(description = "充值金额（元）", example = "100.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "支付方式")
    private Byte paymentMethod;
}
