package com.ev.charging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 结束充电请求DTO
 */
@Data
@Schema(description = "结束充电请求")
public class EndChargingDTO {

    @Schema(description = "结束时电池SOC（0-100）", example = "85", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "endSoc不能为空")
    @Min(value = 0, message = "endSoc必须在0-100之间")
    @Max(value = 100, message = "endSoc必须在0-100之间")
    private Integer endSoc;
}
