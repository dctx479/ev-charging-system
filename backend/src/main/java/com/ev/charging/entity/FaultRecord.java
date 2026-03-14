package com.ev.charging.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 故障记录实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fault_record")
public class FaultRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 充电桩ID
     */
    @Column(name = "pile_id", nullable = false)
    private Long pileId;

    /**
     * 站点ID
     */
    @Column(name = "station_id", nullable = false)
    private Long stationId;

    /**
     * 故障类型：1通信故障 2硬件故障 3软件故障 4其他
     */
    @Column(name = "fault_type", nullable = false)
    private Byte faultType;

    /**
     * 故障代码
     */
    @Column(name = "fault_code", length = 50)
    private String faultCode;

    /**
     * 故障描述
     */
    @Column(name = "fault_description", columnDefinition = "TEXT")
    private String faultDescription;

    /**
     * 严重程度：1轻微 2一般 3严重 4紧急
     */
    @Column(name = "severity", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Byte severity;

    /**
     * 上报来源：1系统自动 2用户上报 3运维发现
     */
    @Column(name = "report_source", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Byte reportSource;

    /**
     * 上报人ID
     */
    @Column(name = "reporter_id")
    private Long reporterId;

    /**
     * 故障发生时间
     */
    @Column(name = "fault_time", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime faultTime;

    /**
     * 维修状态：0待维修 1维修中 2已完成
     */
    @Column(name = "repair_status", nullable = false, columnDefinition = "TINYINT DEFAULT 0")
    private Byte repairStatus;

    /**
     * 维修人员
     */
    @Column(name = "repair_person", length = 50)
    private String repairPerson;

    /**
     * 开始维修时间
     */
    @Column(name = "repair_start_time")
    private LocalDateTime repairStartTime;

    /**
     * 完成维修时间
     */
    @Column(name = "repair_end_time")
    private LocalDateTime repairEndTime;

    /**
     * 维修成本
     */
    @Column(name = "repair_cost", columnDefinition = "DECIMAL(8,2) DEFAULT 0.00")
    private BigDecimal repairCost;

    /**
     * 维修备注
     */
    @Column(name = "repair_note", columnDefinition = "TEXT")
    private String repairNote;

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
}
