package com.ev.charging.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ev.charging.entity.ChatMessage;
import com.ev.charging.entity.User;
import com.ev.charging.repository.ChatMessageRepository;
import com.ev.charging.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ChargingChatHandler extends TextWebSocketHandler {

    // 存储每个站点的在线用户
    private static final ConcurrentHashMap<Long, Set<WebSocketSession>> stationSessions = new ConcurrentHashMap<>();

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从握手拦截器验证后的attributes获取已认证的userId
        Long userId = (Long) session.getAttributes().get("userId");

        // userId应该已经由JwtHandshakeInterceptor验证和设置，此处再次检查以防万一
        if (userId == null || userId <= 0) {
            log.warn("WebSocket连接被拒绝: userId未找到或无效，可能是握手拦截器配置问题");
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Unauthorized: invalid or missing userId"));
            return;
        }

        // 从URL参数获取站点ID（客户端通过 ws://localhost:8080/ws/chat?token=xxx&stationId=1 传入）
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        Map<String, String> params = parseQuery(query);

        String stationIdStr = params.get("stationId");
        if (stationIdStr == null) {
            log.warn("WebSocket连接被拒绝: 缺少stationId参数，userId={}", userId);
            session.close(CloseStatus.BAD_DATA.withReason("stationId is required"));
            return;
        }

        Long stationId;
        try {
            stationId = Long.parseLong(stationIdStr);
        } catch (NumberFormatException e) {
            log.warn("WebSocket连接被拒绝: stationId格式无效，userId={}, stationId={}", userId, stationIdStr);
            session.close(CloseStatus.BAD_DATA.withReason("Invalid stationId format"));
            return;
        }

        // 添加到该站点的会话集合
        stationSessions.computeIfAbsent(stationId, k -> ConcurrentHashMap.newKeySet()).add(session);

        // 保存站点信息到session属性（userId已在握手时存入）
        session.getAttributes().put("stationId", stationId);

        log.info("WebSocket连接建立: userId={}, stationId={}, sessionId={}", userId, stationId, session.getId());

        // 发送系统消息:用户加入
        sendSystemMessage(stationId, "用户 " + userId + " 加入了聊天");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long stationId = (Long) session.getAttributes().get("stationId");
        Long userId = (Long) session.getAttributes().get("userId");

        String payload = message.getPayload();

        // 防DoS：限制消息长度
        if (payload.length() > 5000) {
            log.warn("聊天消息过大，忽略: sessionId={}, length={}", session.getId(), payload.length());
            return;
        }

        JSONObject json;
        try {
            json = JSON.parseObject(payload);
        } catch (Exception e) {
            log.warn("聊天消息解析失败，忽略: sessionId={}, payload={}", session.getId(), payload);
            return;
        }
        String content = json.getString("content");
        // 忽略空内容消息，防止存入null
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        // 保存消息到数据库
        User user = userRepository.findById(userId).orElse(null);

        // XSS防护：对用户输入的内容进行HTML转义
        String sanitizedContent = content
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");

        ChatMessage chatMessage = ChatMessage.builder()
            .stationId(stationId)
            .userId(userId)
            .userNickname(user != null ? user.getNickname() : "用户" + userId)
            .userAvatar(user != null ? user.getAvatar() : null)
            .messageType((byte) 1)  // 文字消息
            .content(sanitizedContent)
            .build();

        chatMessageRepository.save(chatMessage);

        // 广播消息到该站点的所有用户
        broadcastToStation(stationId, JSON.toJSONString(Map.of(
            "type", "chat",
            "messageId", chatMessage.getId(),
            "userId", userId,
            "userNickname", chatMessage.getUserNickname(),
            "userAvatar", chatMessage.getUserAvatar() != null ? chatMessage.getUserAvatar() : "",
            "content", sanitizedContent,
            "createTime", chatMessage.getCreateTime() != null ? chatMessage.getCreateTime().toString() : LocalDateTime.now().toString()
        )));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long stationId = (Long) session.getAttributes().get("stationId");
        Long userId = (Long) session.getAttributes().get("userId");
        log.warn("WebSocket传输错误: userId={}, stationId={}, sessionId={}, error={}",
                userId, stationId, session.getId(), exception.getMessage());

        // 清理已断开的session
        if (stationId != null) {
            Set<WebSocketSession> sessions = stationSessions.get(stationId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    stationSessions.remove(stationId);
                }
            }
        }

        if (session.isOpen()) {
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (IOException e) {
                log.debug("关闭异常WebSocket会话失败: sessionId={}", session.getId());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long stationId = (Long) session.getAttributes().get("stationId");
        Long userId = (Long) session.getAttributes().get("userId");

        // 移除会话
        Set<WebSocketSession> sessions = stationSessions.get(stationId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                stationSessions.remove(stationId);
            }
        }

        // 发送系统消息:用户离开
        if (stationId != null && userId != null) {
            sendSystemMessage(stationId, "用户 " + userId + " 离开了聊天");
        }
    }

    private void broadcastToStation(Long stationId, String message) {
        Set<WebSocketSession> sessions = stationSessions.get(stationId);
        if (sessions != null) {
            // 创建快照副本，避免并发修改异常
            Set<WebSocketSession> snapshot = new HashSet<>(sessions);
            snapshot.forEach(session -> {
                try {
                    synchronized (session) {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(message));
                        }
                    }
                } catch (IOException e) {
                    log.warn("WebSocket广播失败: sessionId={}, error={}", session.getId(), e.getMessage());
                }
            });
        }
    }

    private void sendSystemMessage(Long stationId, String content) {
        // 保存系统消息
        ChatMessage chatMessage = ChatMessage.builder()
            .stationId(stationId)
            .userId(0L)
            .userNickname("系统")
            .messageType((byte) 3)
            .content(content)
            .build();

        chatMessageRepository.save(chatMessage);

        // 广播
        broadcastToStation(stationId, JSON.toJSONString(Map.of(
            "type", "system",
            "content", content,
            "createTime", chatMessage.getCreateTime() != null ? chatMessage.getCreateTime().toString() : LocalDateTime.now().toString()
        )));
    }

    private Map<String, String> parseQuery(String query) {
        if (query == null || query.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new java.util.HashMap<>();
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2 && !pair[0].isEmpty()) {
                result.putIfAbsent(pair[0], pair[1]); // 重复key取第一个，避免Collectors.toMap崩溃
            }
        }
        return result;
    }

    public static Set<WebSocketSession> getStationSessions(Long stationId) {
        return stationSessions.getOrDefault(stationId, ConcurrentHashMap.newKeySet());
    }
}
