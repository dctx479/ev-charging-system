package com.ev.charging.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台订单列表VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderListVO {

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
     * 用户手机号
     */
    private String userPhone;

    /**
     * 用户昵称
     */
    private String userNickname;

    /**
     * 用户姓名(显示用，优先使用昵称)
     */
    private String userName;

    /**
     * 充电站ID
     */
    private Long stationId;

    /**
     * 充电站名称
     */
    private String stationName;

    /**
     * 充电桩ID
     */
    private Long pileId;

    /**
     * 充电桩编号
     */
    private String pileNo;

    /**
     * 开始充电时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束充电时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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
     * 总费用（元）
     */
    private BigDecimal totalFee;

    /**
     * 电费（元）
     */
    private BigDecimal electricityFee;

    /**
     * 服务费（元）
     */
    private BigDecimal serviceFee;

    /**
     * 支付状态：0-未支付 1-已支付 2-已退款
     */
    private Byte paymentStatus;

    /**
     * 支付状态文本
     */
    private String paymentStatusText;

    /**
     * 支付方式：1-微信 2-支付宝 3-余额
     */
    private Byte paymentMethod;

    /**
     * 支付方式文本
     */
    private String paymentMethodText;

    /**
     * 支付时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentTime;

    /**
     * 订单状态：1-待支付 2-充电中 3-已完成 4-已取消 5-异常
     */
    private Byte orderStatus;

    /**
     * 订单状态文本
     */
    private String orderStatusText;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
