package com.ev.charging.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 排队记录实体
 */
@Entity
@Table(name = "queue_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 订单ID（可选，排队时可能还未创建订单）
     */
    @Column(name = "order_id")
    private Long orderId;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 充电站ID
     */
    @Column(name = "station_id", nullable = false)
    private Long stationId;

    /**
     * 充电桩ID（分配后设置）
     */
    @Column(name = "pile_id")
    private Long pileId;

    /**
     * 排队号
     */
    @Column(name = "queue_no", nullable = false, length = 20)
    private String queueNo;

    /**
     * 当前队列位置
     */
    @Column(name = "queue_position", nullable = false)
    private Integer queuePosition;

    /**
     * 预计等待时间（分钟）
     */
    @Column(name = "estimated_wait_time")
    private Integer estimatedWaitTime;

    /**
     * 状态：0-排队中 1-已叫号 2-已过号 3-已取消
     */
    @Column(name = "status", nullable = false)
    private Byte status;

    /**
     * 加入排队时间
     */
    @Column(name = "join_time", nullable = false)
    private LocalDateTime joinTime;

    /**
     * 叫号时间
     */
    @Column(name = "call_time")
    private LocalDateTime callTime;

    /**
     * 过期时间（叫号后15分钟）
     */
    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
