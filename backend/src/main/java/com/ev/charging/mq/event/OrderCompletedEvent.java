package com.ev.charging.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单完成事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String messageId;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private Long stationId;
    private Long pileId;
    private BigDecimal chargeAmount;
    private BigDecimal totalFee;
    private BigDecimal electricityFee;
    private BigDecimal serviceFee;
    private Integer chargeDuration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime eventTime;
}
