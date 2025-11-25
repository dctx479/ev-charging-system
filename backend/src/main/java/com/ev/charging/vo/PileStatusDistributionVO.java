package com.ev.charging.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 充电桩状态分布VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PileStatusDistributionVO {

    /**
     * 空闲数量
     */
    private Integer availableCount;

    /**
     * 充电中数量
     */
    private Integer chargingCount;

    /**
     * 预约中数量
     */
    private Integer reservedCount;

    /**
     * 故障数量
     */
    private Integer faultCount;

    /**
     * 离线数量
     */
    private Integer offlineCount;
}
