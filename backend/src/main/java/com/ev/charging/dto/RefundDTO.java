package com.ev.charging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 退款请求DTO
 */
@Data
@Schema(description = "订单退款请求")
public class RefundDTO {

    /**
     * 退款金额
     */
    @Schema(description = "退款金额（元）", example = "50.00", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0.01")
    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0")
    @DecimalMax(value = "99999.99", message = "退款金额不能超过99999.99元")
    private BigDecimal refundAmount;

    /**
     * 退款原因
     */
    @Schema(description = "退款原因说明", example = "设备故障导致充电中断", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "退款原因不能为空")
    @Size(max = 500, message = "退款原因长度不能超过500个字符")
    private String refundReason;

    /**
     * 操作人（管理员）
     */
    @Schema(description = "操作人（管理员姓名）", example = "张三")
    @Size(max = 50, message = "操作人长度不能超过50个字符")
    private String operator;
}
