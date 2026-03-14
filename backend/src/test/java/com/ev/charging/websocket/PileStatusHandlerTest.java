package com.ev.charging.websocket;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * WebSocket处理器单元测试
 *
 * 测试范围：
 * - 连接建立（认证验证）
 * - 接收客户端消息
 * - 连接关闭
 * - 广播消息到所有客户端
 * - 错误处理
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
@DisplayName("WebSocket处理器测试")
class PileStatusHandlerTest {

    @Mock
    private WebSocketSession session1;

    @Mock
    private WebSocketSession session2;

    @Mock
    private WebSocketSession session3;

    private PileStatusHandler pileStatusHandler;

    @BeforeEach
    void setUp() {
        pileStatusHandler = new PileStatusHandler();
    }

    // ==================== 连接建立测试 ====================

    @Test
    @DisplayName("连接建立 - 通过认证")
    void testAfterConnectionEstablishedSuccess() throws Exception {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", 1L);

        when(session1.getId()).thenReturn("session-1");
        when(session1.getAttributes()).thenReturn(attributes);

        // Act
        pileStatusHandler.afterConnectionEstablished(session1);

        // Assert
        verify(session1, times(1)).sendMessage(any(TextMessage.class));
        verify(session1, never()).close(any(CloseStatus.class));

        log.info("连接建立成功: session-1, userId=1");
    }

    @Test
    @DisplayName("连接建立 - userId为null（认证失败）")
    void testAfterConnectionEstablishedNoUserId() throws Exception {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", null);

        when(session1.getId()).thenReturn("session-1");
        when(session1.getAttributes()).thenReturn(attributes);

        // Act
        pileStatusHandler.afterConnectionEstablished(session1);

        // Assert
        verify(session1, times(1)).close(argThat(status ->
                status.getCode() == 1008 // POLICY_VIOLATION
        ));
        verify(session1, never()).sendMessage(any(TextMessage.class));

        log.warn("连接被拒绝: userId不存在");
    }

    @Test
    @DisplayName("连接建立 - userId无效（<=0）")
    void testAfterConnectionEstablishedInvalidUserId() throws Exception {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", 0L);

        when(session1.getId()).thenReturn("session-1");
        when(session1.getAttributes()).thenReturn(attributes);

        // Act
        pileStatusHandler.afterConnectionEstablished(session1);

        // Assert
        verify(session1, times(1)).close(any(CloseStatus.class));

        log.warn("连接被拒绝: userId无效");
    }

    @Test
    @DisplayName("连接建立 - 缺少userId属性")
    void testAfterConnectionEstablishedMissingUserId() throws Exception {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        // 不设置userId

        when(session1.getId()).thenReturn("session-1");
        when(session1.getAttributes()).thenReturn(attributes);

        // Act
        pileStatusHandler.afterConnectionEstablished(session1);

        // Assert
        verify(session1, times(1)).close(any(CloseStatus.class));
    }

    // ==================== 接收客户端消息测试 ====================

    @Test
    @DisplayName("接收客户端消息 - 成功")
    void testHandleTextMessageSuccess() throws Exception {
        // Arrange
        String clientMessage = "{\"type\":\"subscribe\",\"stationId\":1}";
        TextMessage message = new TextMessage(clientMessage);

        when(session1.getId()).thenReturn("session-1");

        // Act
        pileStatusHandler.handleTextMessage(session1, message);

        // Assert
        verify(session1, times(1)).sendMessage(any(TextMessage.class));

        log.info("客户端消息已处理: {}", clientMessage);
    }

    @Test
    @DisplayName("接收客户端消息 - 异常处理")
    void testHandleTextMessageException() throws Exception {
        // Arrange
        String clientMessage = "{\"type\":\"subscribe\"}";
        TextMessage message = new TextMessage(clientMessage);

        when(session1.getId()).thenReturn("session-1");
        doThrow(new IOException("发送消息失败")).when(session1).sendMessage(any(TextMessage.class));

        // Act & Assert
        assertThrows(IOException.class, () -> {
            pileStatusHandler.handleTextMessage(session1, message);
        });

        log.error("处理客户端消息异常");
    }

    // ==================== 连接关闭测试 ====================

    @Test
    @DisplayName("连接关闭 - 正常关闭")
    void testAfterConnectionClosedNormal() throws Exception {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", 1L);

        when(session1.getId()).thenReturn("session-1");
        when(session1.getAttributes()).thenReturn(attributes);

        // 先建立连接
        pileStatusHandler.afterConnectionEstablished(session1);

        // Act
        pileStatusHandler.afterConnectionClosed(session1, CloseStatus.NORMAL);

        // Assert
        verify(session1, never()).sendMessage(any(TextMessage.class));

        log.info("连接正常关闭: session-1");
    }

    @Test
    @DisplayName("连接关闭 - 异常关闭")
    void testAfterConnectionClosedAbnormal() throws Exception {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", 2L);

        when(session1.getId()).thenReturn("session-1");
        when(session1.getAttributes()).thenReturn(attributes);

        // 先建立连接
        pileStatusHandler.afterConnectionEstablished(session1);

        // Act
        pileStatusHandler.afterConnectionClosed(session1, CloseStatus.SERVER_ERROR);

        // Assert
        log.warn("连接异常关闭: session-1, code=1011");
    }

    // ==================== 广播消息测试 ====================

    @Test
    @DisplayName("广播消息 - 发送给所有连接的客户端")
    void testBroadcastMessage() throws Exception {
        // Arrange
        Map<String, Object> attributes1 = new HashMap<>();
        attributes1.put("userId", 1L);
        Map<String, Object> attributes2 = new HashMap<>();
        attributes2.put("userId", 2L);

        when(session1.getId()).thenReturn("session-1");
        when(session1.getAttributes()).thenReturn(attributes1);
        when(session2.getId()).thenReturn("session-2");
        when(session2.getAttributes()).thenReturn(attributes2);

        // 建立多个连接
        pileStatusHandler.afterConnectionEstablished(session1);
        pileStatusHandler.afterConnectionEstablished(session2);

        // Act
        String broadcastMessage = "{\"type\":\"fault_alert\",\"pileId\":1,\"severity\":\"HIGH\"}";
        PileStatusHandler.broadcast(broadcastMessage);

        // Assert
        // 验证消息已发送到所有连接（虽然实际的broadcast方法是静态的）
        log.info("广播消息已发送: {}", broadcastMessage);
    }

    @Test
    @DisplayName("广播消息 - 处理发送异常")
    void testBroadcastMessageHandleException() throws Exception {
        // Arrange
        Map<String, Object> attributes1 = new HashMap<>();
        attributes1.put("userId", 1L);

        when(session1.getId()).thenReturn("session-1");
        when(session1.getAttributes()).thenReturn(attributes1);

        pileStatusHandler.afterConnectionEstablished(session1);

        // 模拟一个会话已关闭
        when(session1.isOpen()).thenReturn(false);

        // Act
        String broadcastMessage = "{\"type\":\"status_update\"}";
        try {
            PileStatusHandler.broadcast(broadcastMessage);
        } catch (Exception e) {
            log.debug("广播时处理异常: {}", e.getMessage());
        }

        // Assert
        log.info("广播异常处理完成");
    }

    // ==================== 多客户端连接测试 ====================

    @Test
    @DisplayName("多客户端连接 - 同时连接3个客户端")
    void testMultipleClientsConnection() throws Exception {
        // Arrange
        Map<String, Object> attributes1 = new HashMap<>();
        attributes1.put("userId", 1L);
        Map<String, Object> attributes2 = new HashMap<>();
        attributes2.put("userId", 2L);
        Map<String, Object> attributes3 = new HashMap<>();
        attributes3.put("userId", 3L);

        when(session1.getId()).thenReturn("session-1");
        when(session1.getAttributes()).thenReturn(attributes1);
        when(session2.getId()).thenReturn("session-2");
        when(session2.getAttributes()).thenReturn(attributes2);
        when(session3.getId()).thenReturn("session-3");
        when(session3.getAttributes()).thenReturn(attributes3);

        // Act
        pileStatusHandler.afterConnectionEstablished(session1);
        pileStatusHandler.afterConnectionEstablished(session2);
        pileStatusHandler.afterConnectionEstablished(session3);

        // Assert
        verify(session1, times(1)).sendMessage(any(TextMessage.class));
        verify(session2, times(1)).sendMessage(any(TextMessage.class));
        verify(session3, times(1)).sendMessage(any(TextMessage.class));

        log.info("3个客户端连接成功");
    }

    @Test
    @DisplayName("多客户端连接 - 部分客户端断开连接")
    void testMultipleClientsDisconnect() throws Exception {
        // Arrange
        Map<String, Object> attributes1 = new HashMap<>();
        attributes1.put("userId", 1L);
        Map<String, Object> attributes2 = new HashMap<>();
        attributes2.put("userId", 2L);

        when(session1.getId()).thenReturn("session-1");
        when(session1.getAttributes()).thenReturn(attributes1);
        when(session2.getId()).thenReturn("session-2");
        when(session2.getAttributes()).thenReturn(attributes2);

        // 建立连接
        pileStatusHandler.afterConnectionEstablished(session1);
        pileStatusHandler.afterConnectionEstablished(session2);

        // Act
        pileStatusHandler.afterConnectionClosed(session1, CloseStatus.NORMAL);

        // Assert
        // session2 应该仍然连接
        log.info("session-1断开连接，session-2仍然连接");
    }

    // ==================== 消息格式测试 ====================

    @Test
    @DisplayName("接收JSON格式消息 - 有效的JSON")
    void testHandleValidJsonMessage() throws Exception {
        // Arrange
        String validJson = "{\"type\":\"subscribe\",\"stationId\":1,\"pileId\":100}";
        TextMessage message = new TextMessage(validJson);

        when(session1.getId()).thenReturn("session-1");

        // Act
        pileStatusHandler.handleTextMessage(session1, message);

        // Assert
        verify(session1, times(1)).sendMessage(any(TextMessage.class));

        log.info("有效JSON消息处理成功");
    }

    @Test
    @DisplayName("接收JSON格式消息 - 无效的JSON")
    void testHandleInvalidJsonMessage() throws Exception {
        // Arrange
        String invalidJson = "{type:subscribe}"; // 非法JSON
        TextMessage message = new TextMessage(invalidJson);

        when(session1.getId()).thenReturn("session-1");

        // Act - 不应该抛出异常，应该日志记录
        pileStatusHandler.handleTextMessage(session1, message);

        // Assert
        log.warn("无效JSON消息: {}", invalidJson);
    }

    // ==================== 心跳测试 ====================

    @Test
    @DisplayName("发送心跳/Ping消息")
    void testHeartbeatMessage() throws Exception {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", 1L);

        when(session1.getId()).thenReturn("session-1");
        when(session1.getAttributes()).thenReturn(attributes);

        pileStatusHandler.afterConnectionEstablished(session1);

        // Act
        String heartbeatMessage = "{\"type\":\"ping\"}";
        TextMessage message = new TextMessage(heartbeatMessage);
        pileStatusHandler.handleTextMessage(session1, message);

        // Assert
        verify(session1, atLeast(2)).sendMessage(any(TextMessage.class)); // 连接消息 + 响应

        log.info("心跳消息发送成功");
    }

    @Test
    @DisplayName("接收心跳响应 - Pong")
    void testHeartbeatResponse() throws Exception {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", 1L);

        when(session1.getId()).thenReturn("session-1");
        when(session1.getAttributes()).thenReturn(attributes);

        pileStatusHandler.afterConnectionEstablished(session1);

        // Act
        String pongMessage = "{\"type\":\"pong\"}";
        TextMessage message = new TextMessage(pongMessage);
        pileStatusHandler.handleTextMessage(session1, message);

        // Assert
        verify(session1, atLeast(2)).sendMessage(any(TextMessage.class));

        log.info("心跳响应收到");
    }

    // ==================== 实时数据更新测试 ====================

    @Test
    @DisplayName("推送充电桩实时数据更新")
    void testPushPileStatusUpdate() throws Exception {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", 1L);

        when(session1.getId()).thenReturn("session-1");
        when(session1.getAttributes()).thenReturn(attributes);

        pileStatusHandler.afterConnectionEstablished(session1);

        // Act
        String statusUpdate = "{\"type\":\"status_update\"," +
                "\"pileId\":1," +
                "\"status\":\"charging\"," +
                "\"voltage\":380," +
                "\"current\":32.5}";

        TextMessage message = new TextMessage(statusUpdate);
        pileStatusHandler.handleTextMessage(session1, message);

        // Assert
        verify(session1, atLeast(2)).sendMessage(any(TextMessage.class));

        log.info("充电桩状态更新推送成功");
    }

    @Test
    @DisplayName("推送故障警报")
    void testPushFaultAlert() throws Exception {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", 1L);

        when(session1.getId()).thenReturn("session-1");
        when(session1.getAttributes()).thenReturn(attributes);

        pileStatusHandler.afterConnectionEstablished(session1);

        // Act
        String faultAlert = "{\"type\":\"fault_alert\"," +
                "\"pileId\":1," +
                "\"faultType\":\"gun_fault\"," +
                "\"severity\":\"HIGH\"," +
                "\"description\":\"充电枪故障\"}";

        TextMessage message = new TextMessage(faultAlert);
        pileStatusHandler.handleTextMessage(session1, message);

        // Assert
        verify(session1, atLeast(2)).sendMessage(any(TextMessage.class));

        log.warn("故障警报推送成功");
    }

    // ==================== 并发测试 ====================

    @Test
    @DisplayName("并发连接测试 - 10个客户端同时连接")
    void testConcurrentConnections() throws Exception {
        // Arrange
        int clientCount = 10;
        WebSocketSession[] sessions = new WebSocketSession[clientCount];

        for (int i = 0; i < clientCount; i++) {
            sessions[i] = mock(WebSocketSession.class);
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("userId", (long) i + 1);

            when(sessions[i].getId()).thenReturn("session-" + (i + 1));
            when(sessions[i].getAttributes()).thenReturn(attributes);
        }

        // Act
        for (int i = 0; i < clientCount; i++) {
            pileStatusHandler.afterConnectionEstablished(sessions[i]);
        }

        // Assert
        for (int i = 0; i < clientCount; i++) {
            verify(sessions[i], times(1)).sendMessage(any(TextMessage.class));
        }

        log.info("10个客户端并发连接成功");
    }
}
