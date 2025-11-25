package com.ev.charging.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 故障上报DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "充电桩故障上报请求")
public class FaultReportDTO {

    /**
     * 充电桩ID
     */
    @Schema(description = "充电桩ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "充电桩ID不能为空")
    private Long pileId;

    /**
     * 站点ID
     */
    @Schema(description = "充电站ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "站点ID不能为空")
    private Long stationId;

    /**
     * 故障类型：1通信故障 2硬件故障 3软件故障 4其他
     */
    @Schema(description = "故障类型", example = "2", allowableValues = {"1", "2", "3", "4"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "故障类型不能为空")
    @Min(value = 1, message = "故障类型无效")
    @Max(value = 4, message = "故障类型无效")
    private Byte faultType;

    /**
     * 故障代码
     */
    @Schema(description = "故障代码（系统自动生成）", example = "ERR_HW_001")
    private String faultCode;

    /**
     * 故障描述
     */
    @Schema(description = "故障详细描述", example = "充电接口无法连接", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "故障描述不能为空")
    private String faultDescription;

    /**
     * 严重程度：1轻微 2一般 3严重 4紧急
     */
    @Schema(description = "故障严重程度", example = "3", allowableValues = {"1", "2", "3", "4"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "严重程度不能为空")
    @Min(value = 1, message = "严重程度无效")
    @Max(value = 4, message = "严重程度无效")
    private Byte severity;

    /**
     * 上报来源：1系统自动 2用户上报 3运维发现
     */
    @Schema(description = "上报来源", example = "2", allowableValues = {"1", "2", "3"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "上报来源不能为空")
    @Min(value = 1, message = "上报来源无效")
    @Max(value = 3, message = "上报来源无效")
    private Byte reportSource;

    /**
     * 上报人ID
     */
    @Schema(description = "上报人用户ID", example = "1001")
    private Long reporterId;

    /**
     * 故障发生时间
     */
    @Schema(description = "故障发生时间", example = "2024-01-20 14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime faultTime;
}
