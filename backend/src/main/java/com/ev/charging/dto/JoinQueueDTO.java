package com.ev.charging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 加入排队请求DTO
 */
@Data
@Schema(description = "加入充电站排队请求")
public class JoinQueueDTO {

    /**
     * 充电站ID
     */
    @Schema(description = "充电站ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "充电站ID不能为空")
    private Long stationId;

    /**
     * 充电类型：1快充 2慢充
     */
    @Schema(description = "充电类型：1快充 2慢充", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "充电类型不能为空")
    private Byte pileType;

    /**
     * 备注信息（可选）
     */
    @Schema(description = "备注信息", example = "需要快充")
    private String remark;
}
