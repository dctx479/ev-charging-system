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
public class NotificationMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String messageId;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private String relatedId;
    private String relatedType;
    private LocalDateTime eventTime;
}
