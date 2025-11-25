package com.ev.charging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 故障查询条件DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "故障记录查询条件")
public class FaultQueryDTO {

    /**
     * 站点ID
     */
    @Schema(description = "充电站ID（筛选特定站点）", example = "1")
    private Long stationId;

    /**
     * 维修状态：0待维修 1维修中 2已完成
     */
    @Schema(description = "维修状态", example = "0", allowableValues = {"0", "1", "2"})
    @Min(value = 0, message = "维修状态无效")
    @Max(value = 2, message = "维修状态无效")
    private Byte repairStatus;

    /**
     * 严重程度：1轻微 2一般 3严重 4紧急
     */
    @Schema(description = "严重程度", example = "3", allowableValues = {"1", "2", "3", "4"})
    @Min(value = 1, message = "严重程度无效")
    @Max(value = 4, message = "严重程度无效")
    private Byte severity;

    /**
     * 故障类型：1通信故障 2硬件故障 3软件故障 4其他
     */
    @Schema(description = "故障类型", example = "2", allowableValues = {"1", "2", "3", "4"})
    @Min(value = 1, message = "故障类型无效")
    @Max(value = 4, message = "故障类型无效")
    private Byte faultType;

    /**
     * 页码（从0开始，Spring Page 0-indexed）
     */
    @Schema(description = "页码（从0开始）", example = "0", minimum = "0")
    @Min(value = 0, message = "页码不能小于0")
    @Builder.Default
    private Integer page = 0;

    /**
     * 每页大小（兼容前端size参数）
     */
    @Schema(description = "每页记录数", example = "10", minimum = "1", maximum = "100")
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能超过100")
    @Builder.Default
    private Integer size = 10;

    /**
     * 获取每页大小（兼容旧的pageSize字段名）
     */
    public Integer getPageSize() {
        return size;
    }
}
