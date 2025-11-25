package com.ev.charging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

/**
 * V2G放电策略DTO
 */
@Data
@Schema(description = "V2G放电策略配置")
public class DischargeStrategyDTO {

    /**
     * 最低放电电价
     */
    @Schema(description = "最低可接受放电电价（元/kWh）", example = "1.2", minimum = "0")
    @DecimalMin(value = "0", message = "最低放电电价不能为负数")
    private BigDecimal minPrice;

    /**
     * 最大放电量
     */
    @Schema(description = "单次最大放电量（kWh）", example = "20.0", minimum = "0")
    @DecimalMin(value = "0", message = "最大放电量不能为负数")
    private BigDecimal maxDischargeAmount;

    /**
     * 最低电池健康度
     */
    @Schema(description = "允许放电的最低电池健康度（0-100）", example = "80", minimum = "0", maximum = "100")
    @Min(value = 0, message = "电池健康度不能小于0")
    @Max(value = 100, message = "电池健康度不能大于100")
    private Byte minBatteryHealth;
}
