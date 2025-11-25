package com.ev.charging.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "充电订单详情")
public class OrderDetailVO {

    /**
     * 订单ID
     */
    @Schema(description = "订单ID", example = "1")
    private Long id;

    /**
     * 订单号
     */
    @Schema(description = "订单号", example = "202401200001")
    private String orderNo;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    /**
     * 充电站名称
     */
    @Schema(description = "充电站名称", example = "国贸充电站")
    private String stationName;

    /**
     * 充电桩编号
     */
    @Schema(description = "充电桩编号", example = "CP001")
    private String pileNo;

    /**
     * 充电桩功率
     */
    @Schema(description = "充电桩功率（kW）", example = "120.0")
    private BigDecimal pilePower;

    /**
     * 开始充电时间
     */
    @Schema(description = "开始充电时间", example = "2024-01-20 14:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束充电时间
     */
    @Schema(description = "结束充电时间", example = "2024-01-20 15:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 充电时长（分钟）
     */
    @Schema(description = "充电时长（分钟）", example = "90")
    private Integer chargeDuration;

    /**
     * 充电量（kWh）
     */
    @Schema(description = "实际充电量（kWh）", example = "45.5")
    private BigDecimal chargeAmount;

    /**
     * 电费（元）
     */
    @Schema(description = "电费（元）", example = "36.40")
    private BigDecimal electricityFee;

    /**
     * 服务费（元）
     */
    @Schema(description = "服务费（元）", example = "4.55")
    private BigDecimal serviceFee;

    /**
     * 总费用（元）
     */
    @Schema(description = "总费用（元）", example = "40.95")
    private BigDecimal totalFee;

    /**
     * 支付状态：0-未支付 1-已支付 2-已退款
     */
    @Schema(description = "支付状态", example = "1", allowableValues = {"0", "1", "2"})
    private Byte paymentStatus;

    /**
     * 支付方式：1-微信 2-支付宝 3-余额
     */
    @Schema(description = "支付方式", example = "1", allowableValues = {"1", "2", "3"})
    private Byte paymentMethod;

    /**
     * 支付时间
     */
    @Schema(description = "支付时间", example = "2024-01-20 15:35:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentTime;

    /**
     * 订单状态：0-进行中 1-已完成 2-已取消 3-异常
     */
    @Schema(description = "订单状态", example = "1", allowableValues = {"0", "1", "2", "3"})
    private Byte orderStatus;

    /**
     * 开始SOC（%）
     */
    @Schema(description = "开始电量百分比（%）", example = "20")
    private Integer startSoc;

    /**
     * 结束SOC（%）
     */
    @Schema(description = "结束电量百分比（%）", example = "80")
    private Integer endSoc;

    /**
     * 充电模式：1-充满 2-按金额 3-按电量 4-按时间
     */
    @Schema(description = "充电模式", example = "1", allowableValues = {"1", "2", "3", "4"})
    private Byte chargeMode;

    /**
     * 目标值
     */
    @Schema(description = "目标值（根据充电模式）", example = "50.00")
    private BigDecimal targetValue;

    /**
     * 创建时间
     */
    @Schema(description = "订单创建时间", example = "2024-01-20 13:55:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "订单更新时间", example = "2024-01-20 15:35:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 碳积分获得数量
     */
    @Schema(description = "本次充电获得的碳积分", example = "357")
    private Integer carbonCredits;

    /**
     * 充电桩ID（用于再来一单）
     */
    @Schema(description = "充电桩ID", example = "5")
    private Long pileId;
}
