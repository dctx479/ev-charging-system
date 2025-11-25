package com.ev.charging.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 维护计划DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "充电桩维护计划")
public class MaintenancePlanDTO {

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
     * 计划类型：1预测性维护 2定期保养 3紧急维护
     */
    @Schema(description = "维护计划类型", example = "1", allowableValues = {"1", "2", "3"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划类型不能为空")
    @Min(value = 1, message = "计划类型无效")
    @Max(value = 3, message = "计划类型无效")
    private Byte planType;

    /**
     * 预测故障概率(%)
     */
    @Schema(description = "AI预测的故障概率（百分比）", example = "75.5", minimum = "0", maximum = "100")
    @Min(value = 0, message = "故障概率不能小于0")
    @Max(value = 100, message = "故障概率不能大于100")
    private BigDecimal predictedFaultProbability;

    /**
     * 维护内容
     */
    @Schema(description = "维护工作内容详情", example = "更换老化电路板，检查连接器")
    private String maintenanceContent;

    /**
     * 计划维护时间
     */
    @Schema(description = "计划执行维护的时间", example = "2024-01-25 09:00:00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计划维护时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime plannedTime;

    /**
     * 负责人
     */
    @Schema(description = "维护负责人姓名", example = "张三")
    private String assignedPerson;
}
