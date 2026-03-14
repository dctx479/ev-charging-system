package com.ev.charging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 开始V2G放电DTO
 */
@Data
@Schema(description = "开始V2G放电请求")
public class StartDischargeDTO {

    /**
     * 充电桩ID
     */
    @Schema(description = "充电桩ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "充电桩ID不能为空")
    private Long pileId;

    /**
     * 最低放电电价
     */
    @Schema(description = "最低可接受的放电电价（元/kWh）", example = "1.2", minimum = "0")
    @DecimalMin(value = "0", message = "最低电价不能为负数")
    private BigDecimal minPrice;

    /**
     * 当前电池健康度
     */
    @Schema(description = "放电前电池健康度（0-100）", example = "95", minimum = "0", maximum = "100")
    @Min(value = 0, message = "电池健康度不能小于0")
    @Max(value = 100, message = "电池健康度不能大于100")
    private Byte batteryHealthBefore;

    /**
     * 当前 SOC（电池电量百分比）
     * 用于防止电池过度放电（最低保护阈值 20%）
     */
    @Schema(description = "当前SOC（电池电量百分比，0-100）", example = "75", minimum = "0", maximum = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "当前SOC不能为空")
    @Min(value = 0, message = "当前SOC不能小于0")
    @Max(value = 100, message = "当前SOC不能大于100")
    private Integer currentSoc;
}
