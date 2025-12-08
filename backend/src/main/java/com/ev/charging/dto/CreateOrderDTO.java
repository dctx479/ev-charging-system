package com.ev.charging.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单DTO
 */
@Data
public class CreateOrderDTO {

    /**
     * 充电桩ID
     */
    @NotNull(message = "充电桩ID不能为空")
    private Long pileId;

    /**
     * 充电模式：1-充满 2-按金额 3-按电量 4-按时间
     */
    @NotNull(message = "充电模式不能为空")
    @Min(value = 1, message = "充电模式无效")
    @Max(value = 4, message = "充电模式无效")
    private Byte chargeMode;

    /**
     * 目标值（根据充电模式：金额/电量/时长）
     */
    private BigDecimal targetValue;

    /**
     * 开始SOC（%）
     */
    @NotNull(message = "开始电量不能为空")
    @Min(value = 0, message = "开始电量不能小于0")
    @Max(value = 100, message = "开始电量不能大于100")
    private Integer startSoc;

    /**
     * 电池容量（kWh）
     */
    @NotNull(message = "电池容量不能为空")
    private BigDecimal batteryCapacity;

    /**
     * 充电功率（kW）
     */
    private BigDecimal chargePower;
}
