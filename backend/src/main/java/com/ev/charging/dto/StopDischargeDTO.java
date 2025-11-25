package com.ev.charging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 停止V2G放电DTO
 */
@Data
@Schema(description = "停止V2G放电请求")
public class StopDischargeDTO {

    /**
     * 记录ID
     */
    @Schema(description = "V2G放电记录ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "记录ID不能为空")
    private Long recordId;

    /**
     * 放电量(kWh)
     */
    @Schema(description = "实际放电量（kWh）", example = "15.5", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
    @NotNull(message = "放电量不能为空")
    private BigDecimal energyAmount;

    /**
     * 放电后电池健康度
     */
    @Schema(description = "放电后电池健康度（0-100）", example = "94", minimum = "0", maximum = "100")
    private Byte batteryHealthAfter;

    /**
     * 最终 SOC（电池电量百分比）
     * 用于监控电池放电过程，记录日志和统计
     */
    @Schema(description = "最终SOC（电池电量百分比，0-100）", example = "45", minimum = "0", maximum = "100")
    private Integer finalSoc;
}
