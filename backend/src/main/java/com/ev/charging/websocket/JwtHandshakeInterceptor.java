package com.ev.charging.websocket;

import com.ev.charging.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * WebSocket握手拦截器 - JWT认证
 *
 * 在建立WebSocket连接前进行JWT验证，防止未认证的连接。
 * 验证逻辑:
 * 1. 从query参数获取token (如 ws://localhost:8080/ws/chat?token=xxx)
 * 2. 验证token有效性
 * 3. 提取userId，存入session attributes
 * 4. 验证失败则拒绝握手
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    /**
     * 握手前处理 - 进行JWT认证
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes)
            throws Exception {
        try {
            // 提取URI和query参数
            URI uri = request.getURI();
            String query = uri.getQuery();

            if (query == null || query.isEmpty()) {
                log.warn("WebSocket握手失败: 缺少query参数");
                return false;
            }

            // 解析token参数
            String token = extractTokenFromQuery(query);
            if (token == null || token.isEmpty()) {
                log.warn("WebSocket握手失败: 缺少token参数");
                return false;
            }

            // 验证token有效性
            if (!jwtUtil.validateToken(token)) {
                log.warn("WebSocket握手失败: token无效或已过期");
                return false;
            }

            // 提取userId并验证
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null || userId <= 0) {
                log.warn("WebSocket握手失败: 无法从token中提取有效的userId");
                return false;
            }

            // 将userId存入attributes，供WebSocketHandler使用
            attributes.put("userId", userId);

            log.info("WebSocket握手成功: userId={}, remoteAddress={}",
                    userId, request.getRemoteAddress());
            return true;

        } catch (Exception e) {
            log.error("WebSocket握手异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 握手后处理
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手成功后的处理（可选）
    }

    /**
     * 从query字符串中提取token参数
     *
     * @param query query字符串，如 "token=xxx&other=yyy"
     * @return token值，如果不存在返回null
     */
    private String extractTokenFromQuery(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }

        String[] params = query.split("&");
        for (String param : params) {
            if (param.isEmpty()) {
                continue;
            }

            int eqIndex = param.indexOf('=');
            if (eqIndex > 0) {
                String key = param.substring(0, eqIndex);
                String value = param.substring(eqIndex + 1);

                if ("token".equals(key) && !value.isEmpty()) {
                    // URL decode
                    try {
                        return java.net.URLDecoder.decode(value, "UTF-8");
                    } catch (Exception e) {
                        log.warn("Token URL decode失败: {}", e.getMessage());
                        return null;
                    }
                }
            }
        }
        return null;
    }
}
