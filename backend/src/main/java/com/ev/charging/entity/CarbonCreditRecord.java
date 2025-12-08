package com.ev.charging.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 碳积分记录实体类
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "carbon_credit_record")
public class CarbonCreditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 订单ID（充电获得积分时关联）
     */
    private Long orderId;

    /**
     * 积分变动（正为获得，负为消耗）
     */
    @Column(nullable = false)
    private Integer creditChange;

    /**
     * 积分类型：1-充电获得 2-签到 3-兑换消耗 4-活动奖励
     */
    @Column(nullable = false)
    private Byte creditType;

    /**
     * 描述信息
     */
    @Column(length = 200)
    private String description;

    /**
     * 变动后余额
     */
    @Column(nullable = false)
    private Integer balanceAfter;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;
}
