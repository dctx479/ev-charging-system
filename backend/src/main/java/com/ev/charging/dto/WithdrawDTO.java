package com.ev.charging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 提现请求DTO
 */
@Data
@Schema(description = "提现请求")
public class WithdrawDTO {

    @Schema(description = "提现金额（元）", example = "50.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "提现金额不能为空")
    @DecimalMin(value = "0.01", message = "提现金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "银行账号")
    @Size(max = 50, message = "银行账号长度不能超过50个字符")
    private String bankAccount;
}
