package com.ev.charging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 完成代充订单DTO
 */
@Data
@Schema(description = "完成代充订单请求")
public class CompleteValetOrderDTO {

    /**
     * 充电桩ID
     */
    @Schema(description = "使用的充电桩ID", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "充电桩ID不能为空")
    private Long pileId;
}
