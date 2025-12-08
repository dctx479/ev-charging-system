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
    @Column(nullable = false)
    private Long pileId;

    /**
     * 站点ID
     */
    @Column(nullable = false)
    private Long stationId;

    /**
     * 故障类型：1通信故障 2硬件故障 3软件故障 4其他
     */
    @Column(nullable = false)
    private Byte faultType;

    /**
     * 故障代码
     */
    @Column(length = 50)
    private String faultCode;

    /**
     * 故障描述
     */
    @Column(columnDefinition = "TEXT")
    private String faultDescription;

    /**
     * 严重程度：1轻微 2一般 3严重 4紧急
     */
    @Column(nullable = false)
    private Byte severity;

    /**
     * 上报来源：1系统自动 2用户上报 3运维发现
     */
    @Column(nullable = false)
    private Byte reportSource;

    /**
     * 上报人ID
     */
    private Long reporterId;

    /**
     * 故障发生时间
     */
    @Column(nullable = false)
    private LocalDateTime faultTime;

    /**
     * 维修状态：0待维修 1维修中 2已完成
     */
    @Column(nullable = false)
    private Byte repairStatus;

    /**
     * 维修人员
     */
    @Column(length = 50)
    private String repairPerson;

    /**
     * 开始维修时间
     */
    private LocalDateTime repairStartTime;

    /**
     * 完成维修时间
     */
    private LocalDateTime repairEndTime;

    /**
     * 维修成本
     */
    private BigDecimal repairCost;

    /**
     * 维修备注
     */
    @Column(columnDefinition = "TEXT")
    private String repairNote;

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
