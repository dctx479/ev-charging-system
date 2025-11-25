package com.ev.charging.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaidEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String messageId;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
    private Byte paymentMethod;
    private String paymentNo;
    private BigDecimal chargeAmount;
    private LocalDateTime paymentTime;
    private LocalDateTime eventTime;
}
