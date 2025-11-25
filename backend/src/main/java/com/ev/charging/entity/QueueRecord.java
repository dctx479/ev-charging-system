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
 * 智能排队表实体
 * 对应数据库表: queue_record
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
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 站点ID
     */
    @Column(name = "station_id", nullable = false)
    private Long stationId;

    /**
     * 充电类型：1快充 2慢充
     */
    @Column(name = "pile_type", nullable = false)
    private Byte pileType;

    /**
     * 排队号
     */
    @Column(name = "queue_no", nullable = false, length = 20)
    private String queueNo;

    /**
     * 当前排队位置
     */
    @Column(name = "queue_position", nullable = false)
    private Integer queuePosition;

    /**
     * 预计等待时间（分钟）
     */
    @Column(name = "estimated_wait_time")
    private Integer estimatedWaitTime;

    /**
     * 排队状态：1排队中 2已分配 3已取消 4超时
     */
    @Column(name = "queue_status", nullable = false)
    @Builder.Default
    private Byte queueStatus = 1;

    /**
     * 是否已发送通知：1是 0否
     */
    @Column(name = "notify_sent")
    @Builder.Default
    private Byte notifySent = 0;

    /**
     * 关联订单ID
     */
    @Column(name = "order_id")
    private Long orderId;

    /**
     * 分配的充电桩ID
     */
    @Column(name = "assigned_pile_id")
    private Long assignedPileId;

    /**
     * 加入队列时间
     */
    @Column(name = "join_time", nullable = false, updatable = false)
    private LocalDateTime joinTime;

    /**
     * 分配时间
     */
    @Column(name = "assigned_time")
    private LocalDateTime assignedTime;

    /**
     * 取消时间
     */
    @Column(name = "cancel_time")
    private LocalDateTime cancelTime;

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
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 乐观锁版本号
     */
    @Version
    @Column(name = "version")
    private Integer version;
}
