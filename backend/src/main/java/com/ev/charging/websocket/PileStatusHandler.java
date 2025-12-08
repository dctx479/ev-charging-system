package com.ev.charging.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket处理器 - 充电桩状态实时推送
 */
@Slf4j
public class PileStatusHandler extends TextWebSocketHandler {

    /**
     * 存储所有WebSocket会话
     * Key: sessionId, Value: WebSocketSession
     */
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 连接建立时触发
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("WebSocket连接建立: sessionId={}, 当前连接数={}", session.getId(), sessions.size());

        // 发送欢迎消息
        session.sendMessage(new TextMessage("{\"type\":\"connected\",\"message\":\"WebSocket连接成功\"}"));
    }

    /**
     * 接收客户端消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("收到客户端消息: sessionId={}, message={}", session.getId(), payload);

        // 这里可以根据客户端消息进行不同的处理
        // 例如：客户端订阅特定站点的充电桩状态更新

        // 回复确认消息
        session.sendMessage(new TextMessage("{\"type\":\"ack\",\"message\":\"消息已收到\"}"));
    }

    /**
     * 连接关闭时触发
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        log.info("WebSocket连接关闭: sessionId={}, status={}, 当前连接数={}",
                session.getId(), status, sessions.size());
    }

    /**
     * 传输错误时触发
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: sessionId={}", session.getId(), exception);
        if (session.isOpen()) {
            session.close();
        }
        sessions.remove(session.getId());
    }

    /**
     * 广播消息给所有连接的客户端
     *
     * @param message JSON格式的消息
     */
    public static void broadcast(String message) {
        log.info("广播消息给{}个客户端: {}", sessions.size(), message);

        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                log.error("发送消息失败: sessionId={}", session.getId(), e);
            }
        });
    }

    /**
     * 向指定会话发送消息
     *
     * @param sessionId 会话ID
     * @param message JSON格式的消息
     */
    public static void sendToSession(String sessionId, String message) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.info("发送消息给指定客户端: sessionId={}", sessionId);
            } catch (IOException e) {
                log.error("发送消息失败: sessionId={}", sessionId, e);
            }
        }
    }

    /**
     * 获取当前连接数
     */
    public static int getConnectionCount() {
        return sessions.size();
    }
}
