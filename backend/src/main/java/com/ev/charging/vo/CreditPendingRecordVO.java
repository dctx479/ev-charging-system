package com.ev.charging.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分待发放记录VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditPendingRecordVO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String userNickname;

    /**
     * 充电量(kWh)
     */
    private BigDecimal chargeAmount;

    /**
     * 应发放积分数
     */
    private Integer creditsToIssue;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最后一次错误信息
     */
    private String lastError;

    /**
     * 状态：0-待重试 1-已成功 2-已放弃
     */
    private Byte status;

    /**
     * 状态文本
     */
    private String statusText;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 获取状态文本
     */
    public static String getStatusText(Byte status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待重试";
            case 1 -> "已成功";
            case 2 -> "已放弃";
            default -> "未知";
        };
    }
}
