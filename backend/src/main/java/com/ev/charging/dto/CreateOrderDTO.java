package com.ev.charging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单DTO
 */
@Data
@Schema(description = "创建充电订单请求")
public class CreateOrderDTO {

    /**
     * 充电桩ID
     */
    @Schema(description = "充电桩ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "充电桩ID不能为空")
    private Long pileId;

    /**
     * 充电模式：1-充满 2-按金额 3-按电量 4-按时间
     */
    @Schema(description = "充电模式", example = "1", allowableValues = {"1", "2", "3", "4"},
            requiredMode = Schema.RequiredMode.REQUIRED,
            implementation = Byte.class)
    @NotNull(message = "充电模式不能为空")
    @Min(value = 1, message = "充电模式无效")
    @Max(value = 4, message = "充电模式无效")
    private Byte chargeMode;

    /**
     * 目标值（根据充电模式：金额/电量/时长）
     */
    @Schema(description = "目标值（模式2:金额元，模式3:电量kWh，模式4:时长分钟）", example = "50.00")
    @DecimalMin(value = "0", message = "目标值必须大于等于0")
    private BigDecimal targetValue;

    /**
     * 开始SOC（%）
     */
    @Schema(description = "开始电量SOC（%）", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开始电量不能为空")
    @Min(value = 0, message = "开始电量不能小于0")
    @Max(value = 100, message = "开始电量不能大于100")
    private Integer startSoc;

    /**
     * 电池容量（kWh）
     */
    @Schema(description = "电池容量（kWh）", example = "75.0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "电池容量不能为空")
    @DecimalMin(value = "1", message = "电池容量不能小于1kWh")
    private BigDecimal batteryCapacity;

    /**
     * 充电功率（kW）
     */
    @Schema(description = "充电功率（kW）", example = "120.0")
    @DecimalMin(value = "0.1", message = "充电功率必须大于0")
    private BigDecimal chargePower;
}
