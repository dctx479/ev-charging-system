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
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 订单ID（充电获得积分时关联）
     */
    @Column(name = "order_id")
    private Long orderId;

    /**
     * 变动类型：1充电获得 2签到 3兑换消耗 4活动奖励
     */
    @Column(name = "credit_type", nullable = false)
    private Byte changeType;

    /**
     * 变动数量（正为增加，负为减少）
     */
    @Column(name = "credit_change", nullable = false)
    private Integer changeAmount;

    /**
     * 变动后余额
     */
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    /**
     * 对应碳减排量(kg)
     */
    @Column(name = "carbon_reduction", precision = 6, scale = 2)
    private BigDecimal carbonReduction;

    /**
     * 说明/描述信息
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}
