package com.ev.charging.dto;

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
public class RepairUpdateDTO {

    /**
     * 维修状态：0待维修 1维修中 2已完成
     */
    @NotNull(message = "维修状态不能为空")
    private Byte repairStatus;

    /**
     * 维修人员
     */
    private String repairPerson;

    /**
     * 开始维修时间
     */
    private LocalDateTime repairStartTime;

    /**
     * 完成维修时间
     */
    private LocalDateTime repairEndTime;

    /**
     * 维修成本
     */
    private BigDecimal repairCost;

    /**
     * 维修备注
     */
    private String repairNote;
}
