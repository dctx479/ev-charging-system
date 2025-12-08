package com.ev.charging.interceptor;

import com.ev.charging.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT Token 验证拦截器
 * 用于验证请求中的JWT Token是否有效
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    /**
     * 在请求处理前进行Token验证
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param handler  处理器
     * @return true-继续处理请求，false-拦截请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取请求头中的Token
        String token = request.getHeader("Authorization");

        // 如果Token为空或格式不正确
        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("请求未携带有效Token: {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":401,\"message\":\"未授权，请先登录\",\"data\":null}");
            } catch (Exception e) {
                log.error("写入响应失败", e);
            }
            return false;
        }

        // 提取Token（去掉"Bearer "前缀）
        token = token.substring(7);

        // 验证Token是否有效
        if (!jwtUtil.validateToken(token)) {
            log.warn("Token验证失败: {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":401,\"message\":\"Token已过期或无效，请重新登录\",\"data\":null}");
            } catch (Exception e) {
                log.error("写入响应失败", e);
            }
            return false;
        }

        // Token验证成功，将用户信息存储到请求属性中
        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            log.debug("Token验证成功 - 用户: {}, ID: {}", username, userId);
        } catch (Exception e) {
            log.error("解析Token失败", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        return true;
    }
}
