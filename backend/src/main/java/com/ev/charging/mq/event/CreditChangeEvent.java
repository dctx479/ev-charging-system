package com.ev.charging.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditChangeEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String messageId;
    private Long userId;
    private Long orderId;
    private Integer creditAmount;
    private String changeType;
    private String description;
    private LocalDateTime eventTime;
}
