package com.ev.charging.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 维护计划实体类（预测性维护）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "maintenance_plan")
public class MaintenancePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 充电桩ID
     */
    @Column(nullable = false)
    private Long pileId;

    /**
     * 站点ID
     */
    @Column(nullable = false)
    private Long stationId;

    /**
     * 计划类型：1预测性维护 2定期保养 3紧急维护
     */
    @Column(nullable = false)
    private Byte planType;

    /**
     * 预测故障概率(%)
     */
    private BigDecimal predictedFaultProbability;

    /**
     * 维护内容
     */
    @Column(columnDefinition = "TEXT")
    private String maintenanceContent;

    /**
     * 计划维护时间
     */
    @Column(nullable = false)
    private LocalDateTime plannedTime;

    /**
     * 状态：0待执行 1执行中 2已完成 3已取消
     */
    @Column(nullable = false)
    private Byte maintenanceStatus;

    /**
     * 负责人
     */
    @Column(length = 50)
    private String assignedPerson;

    /**
     * 实际开始时间
     */
    private LocalDateTime actualStartTime;

    /**
     * 实际完成时间
     */
    private LocalDateTime actualEndTime;

    /**
     * 维护记录
     */
    @Column(columnDefinition = "TEXT")
    private String maintenanceNote;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateTime;
}
