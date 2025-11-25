package com.ev.charging.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "排队状态信息")
public class QueueStatusVO {

    /**
     * 排队记录ID
     */
    @Schema(description = "排队记录ID", example = "1")
    private Long id;

    /**
     * 排队号
     */
    @Schema(description = "排队号", example = "A001")
    private String queueNo;

    /**
     * 充电站ID
     */
    @Schema(description = "充电站ID", example = "1")
    private Long stationId;

    /**
     * 充电站名称
     */
    @Schema(description = "充电站名称", example = "国贸充电站")
    private String stationName;

    /**
     * 当前队列位置
     */
    @Schema(description = "当前在队列中的位置", example = "3")
    private Integer queuePosition;

    /**
     * 前面排队人数
     */
    @Schema(description = "前面还有多少人", example = "2")
    private Integer peopleAhead;

    /**
     * 预计等待时间（分钟）
     */
    @Schema(description = "预计等待时间（分钟）", example = "15")
    private Integer estimatedWaitTime;

    /**
     * 状态：0-排队中 1-已叫号 2-已过号 3-已取消
     */
    @Schema(description = "排队状态", example = "0", allowableValues = {"0", "1", "2", "3"})
    private Byte status;

    /**
     * 状态文本
     */
    @Schema(description = "状态文本描述", example = "排队中")
    private String statusText;

    /**
     * 加入排队时间
     */
    @Schema(description = "加入排队时间", example = "2024-01-20 14:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinTime;

    /**
     * 叫号时间
     */
    @Schema(description = "叫号时间", example = "2024-01-20 14:15:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime callTime;

    /**
     * 过期时间
     */
    @Schema(description = "叫号后的过期时间", example = "2024-01-20 14:25:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    /**
     * 已分配的充电桩ID（叫号后有值）
     */
    @Schema(description = "已分配的充电桩ID（叫号后有值）", example = "5")
    private Long pileId;

    /**
     * 是否即将过号（叫号后还剩5分钟以内）
     */
    @Schema(description = "是否即将过号（5分钟内）", example = "false")
    private Boolean willExpireSoon;
}
