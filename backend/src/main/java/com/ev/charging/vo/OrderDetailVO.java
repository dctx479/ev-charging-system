package com.ev.charging.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单详情VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailVO {

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 充电站名称
     */
    private String stationName;

    /**
     * 充电桩编号
     */
    private String pileNo;

    /**
     * 充电桩功率
     */
    private BigDecimal pilePower;

    /**
     * 开始充电时间
     */
    private LocalDateTime startTime;

    /**
     * 结束充电时间
     */
    private LocalDateTime endTime;

    /**
     * 充电时长（分钟）
     */
    private Integer chargeDuration;

    /**
     * 充电量（kWh）
     */
    private BigDecimal chargeAmount;

    /**
     * 电费（元）
     */
    private BigDecimal electricityFee;

    /**
     * 服务费（元）
     */
    private BigDecimal serviceFee;

    /**
     * 总费用（元）
     */
    private BigDecimal totalFee;

    /**
     * 支付状态：0-未支付 1-已支付 2-已退款
     */
    private Byte paymentStatus;

    /**
     * 支付方式：1-微信 2-支付宝 3-余额
     */
    private Byte paymentMethod;

    /**
     * 支付时间
     */
    private LocalDateTime paymentTime;

    /**
     * 订单状态：0-进行中 1-已完成 2-已取消 3-异常
     */
    private Byte orderStatus;

    /**
     * 开始SOC（%）
     */
    private Integer startSoc;

    /**
     * 结束SOC（%）
     */
    private Integer endSoc;

    /**
     * 充电模式：1-充满 2-按金额 3-按电量 4-按时间
     */
    private Byte chargeMode;

    /**
     * 目标值
     */
    private BigDecimal targetValue;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
