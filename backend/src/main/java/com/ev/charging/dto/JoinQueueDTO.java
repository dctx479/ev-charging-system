package com.ev.charging.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 加入排队请求DTO
 */
@Data
public class JoinQueueDTO {

    /**
     * 充电站ID
     */
    @NotNull(message = "充电站ID不能为空")
    private Long stationId;

    /**
     * 备注信息（可选）
     */
    private String remark;
}
