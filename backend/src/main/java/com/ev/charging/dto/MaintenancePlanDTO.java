package com.ev.charging.dto;

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
public class MaintenancePlanDTO {

    /**
     * 充电桩ID
     */
    @NotNull(message = "充电桩ID不能为空")
    private Long pileId;

    /**
     * 站点ID
     */
    @NotNull(message = "站点ID不能为空")
    private Long stationId;

    /**
     * 计划类型：1预测性维护 2定期保养 3紧急维护
     */
    @NotNull(message = "计划类型不能为空")
    private Byte planType;

    /**
     * 预测故障概率(%)
     */
    private BigDecimal predictedFaultProbability;

    /**
     * 维护内容
     */
    private String maintenanceContent;

    /**
     * 计划维护时间
     */
    @NotNull(message = "计划维护时间不能为空")
    private LocalDateTime plannedTime;

    /**
     * 负责人
     */
    private String assignedPerson;
}
