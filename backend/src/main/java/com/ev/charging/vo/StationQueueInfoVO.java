package com.ev.charging.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 站点排队信息VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationQueueInfoVO {

    /**
     * 充电站ID
     */
    private Long stationId;

    /**
     * 充电站名称
     */
    private String stationName;

    /**
     * 当前排队人数
     */
    private Integer queueCount;

    /**
     * 可用充电桩数量
     */
    private Integer availablePiles;

    /**
     * 预计平均等待时间（分钟）
     */
    private Integer averageWaitTime;

    /**
     * 是否建议排队
     */
    private Boolean recommendQueue;

    /**
     * 建议信息
     */
    private String suggestion;
}
