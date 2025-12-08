package com.ev.charging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询条件DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaultQueryDTO {

    /**
     * 站点ID
     */
    private Long stationId;

    /**
     * 维修状态：0待维修 1维修中 2已完成
     */
    private Byte repairStatus;

    /**
     * 严重程度：1轻微 2一般 3严重 4紧急
     */
    private Byte severity;

    /**
     * 故障类型：1通信故障 2硬件故障 3软件故障 4其他
     */
    private Byte faultType;

    /**
     * 页码（从1开始）
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
