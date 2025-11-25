package com.ev.charging.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 *
 * 安全改进:
 * 1. 添加JWT握手拦截器进行认证
 * 2. 限制CORS为具体的前端地址（非通配符）
 * 3. 防止未认证的WebSocket连接
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private ChargingChatHandler chargingChatHandler;

    @Autowired
    private PileStatusHandler pileStatusHandler;

    @Autowired
    private JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 允许的前端地址
        String[] allowedOrigins = {
                "http://localhost:5173",      // 用户移动端
                "http://localhost:5174"       // 管理后台
        };

        // 注册充电桩状态WebSocket处理器（使用Spring容器管理的单例）
        // 需要在query参数中传入token: ws://localhost:8080/ws/pile-status?token=xxx
        registry.addHandler(pileStatusHandler, "/ws/pile-status")
                .addInterceptors(jwtHandshakeInterceptor)  // 添加JWT认证拦截器
                .setAllowedOrigins(allowedOrigins);         // 限制CORS为具体地址

        // 注册充电搭子聊天WebSocket处理器
        // 需要在query参数中传入token: ws://localhost:8080/ws/chat?token=xxx&stationId=1
        registry.addHandler(chargingChatHandler, "/ws/chat")
                .addInterceptors(jwtHandshakeInterceptor)  // 添加JWT认证拦截器
                .setAllowedOrigins(allowedOrigins);         // 限制CORS为具体地址
    }
}
