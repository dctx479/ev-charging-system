package com.ev.charging.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_id", nullable = false)
    private Long stationId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "user_nickname", length = 50)
    private String userNickname;
    @Column(name = "user_avatar", length = 255)
    private String userAvatar;

    @Builder.Default
    @Column(name = "message_type", columnDefinition = "TINYINT DEFAULT 1")
    private Byte messageType = 1;  // 1文字 2图片 3系统消息
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder.Default
    @Column(name = "is_deleted")
    private Byte isDeleted = 0;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}
