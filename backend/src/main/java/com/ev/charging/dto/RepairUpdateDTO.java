package com.ev.charging.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
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
 * 维修更新DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "故障维修状态更新")
public class RepairUpdateDTO {

    /**
     * 维修状态：0待维修 1维修中 2已完成
     */
    @Schema(description = "维修状态", example = "2", allowableValues = {"0", "1", "2"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "维修状态不能为空")
    @Min(value = 0, message = "维修状态无效")
    @Max(value = 2, message = "维修状态无效")
    private Byte repairStatus;

    /**
     * 维修人员
     */
    @Schema(description = "维修人员姓名", example = "李师傅")
    private String repairPerson;

    /**
     * 开始维修时间
     */
    @Schema(description = "开始维修时间", example = "2024-01-20 09:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime repairStartTime;

    /**
     * 完成维修时间
     */
    @Schema(description = "完成维修时间", example = "2024-01-20 15:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime repairEndTime;

    /**
     * 维修成本
     */
    @Schema(description = "维修成本（元）", example = "500.00", minimum = "0")
    @DecimalMin(value = "0", message = "维修成本不能为负数")
    private BigDecimal repairCost;

    /**
     * 维修备注
     */
    @Schema(description = "维修备注说明", example = "更换了充电模块，测试正常")
    private String repairNote;
}
