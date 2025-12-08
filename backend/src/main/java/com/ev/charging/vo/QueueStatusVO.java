package com.ev.charging.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 排队状态展示VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueStatusVO {

    /**
     * 排队记录ID
     */
    private Long id;

    /**
     * 排队号
     */
    private String queueNo;

    /**
     * 充电站ID
     */
    private Long stationId;

    /**
     * 充电站名称
     */
    private String stationName;

    /**
     * 当前队列位置
     */
    private Integer queuePosition;

    /**
     * 前面排队人数
     */
    private Integer peopleAhead;

    /**
     * 预计等待时间（分钟）
     */
    private Integer estimatedWaitTime;

    /**
     * 状态：0-排队中 1-已叫号 2-已过号 3-已取消
     */
    private Byte status;

    /**
     * 状态文本
     */
    private String statusText;

    /**
     * 加入排队时间
     */
    private LocalDateTime joinTime;

    /**
     * 叫号时间
     */
    private LocalDateTime callTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 是否即将过号（叫号后还剩5分钟以内）
     */
    private Boolean willExpireSoon;
}
