package com.ev.charging.dto;

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
public class FaultReportDTO {

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
     * 故障类型：1通信故障 2硬件故障 3软件故障 4其他
     */
    @NotNull(message = "故障类型不能为空")
    private Byte faultType;

    /**
     * 故障代码
     */
    private String faultCode;

    /**
     * 故障描述
     */
    @NotBlank(message = "故障描述不能为空")
    private String faultDescription;

    /**
     * 严重程度：1轻微 2一般 3严重 4紧急
     */
    @NotNull(message = "严重程度不能为空")
    private Byte severity;

    /**
     * 上报来源：1系统自动 2用户上报 3运维发现
     */
    @NotNull(message = "上报来源不能为空")
    private Byte reportSource;

    /**
     * 上报人ID
     */
    private Long reporterId;

    /**
     * 故障发生时间
     */
    private LocalDateTime faultTime;
}
