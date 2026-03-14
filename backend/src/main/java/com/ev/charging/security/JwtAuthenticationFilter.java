package com.ev.charging.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ev.charging.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JWT认证过滤器
 * 
 * 功能:
 * 1. 从请求头中提取JWT Token
 * 2. 验证Token有效性
 * 3. 检查Token黑名单（支持登出和强制下线）
 * 4. 解析用户信息和角色
 * 5. 设置Spring Security上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. 从请求头获取Token
            String token = extractTokenFromRequest(request);

            // 2. 如果Token存在且有效
            if (token != null && jwtUtil.validateToken(token)) {

                // 3. 检查Token是否在黑名单中（用户登出）
                if (tokenBlacklistService.isBlacklisted(token)) {
                    log.debug("Token在黑名单中，拒绝访问");
                    sendUnauthorizedResponse(response, "Token已失效，请重新登录");
                    return;
                }

                // 4. 解析Token获取用户信息
                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);
                String userType = jwtUtil.getUserTypeFromToken(token);

                // 5. 检查用户是否被强制下线
                if (tokenBlacklistService.isUserBlacklisted(userId, token)) {
                    log.debug("用户已被强制下线: userId={}", userId);
                    sendUnauthorizedResponse(response, "账号已在其他地方登录或被强制下线");
                    return;
                }

                // 6. 构建权限列表
                List<SimpleGrantedAuthority> authorities = buildAuthorities(userType);

                // 7. 创建认证对象
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                authorities
                        );

                // 8. 设置请求详情
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 9. 将用户信息存储到请求属性(兼容旧代码)
                request.setAttribute("userId", userId);
                request.setAttribute("username", username);
                request.setAttribute("userType", userType);

                // 10. 设置到Security上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT认证成功 - 用户: {}, 角色: {}", username, userType);
            }
        } catch (Exception e) {
            log.warn("JWT认证异常 - {}: {}", e.getClass().getSimpleName(), e.getMessage());
            // 安全修复：异常发生时清除任何可能存在的认证信息
            // 这确保在Token验证失败、过期或被篡改时不会保留任何身份信息
            SecurityContextHolder.clearContext();
            request.removeAttribute("userId");
            request.removeAttribute("username");
            request.removeAttribute("userType");
        }

        // 继续过滤器链
        // 注：Spring Security会根据后续配置决定是否允许访问
        // 如果SecurityContext为空，则视为未认证请求
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取Token
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 根据用户类型构建权限列表
     * 
     * 用户类型映射:
     * - USER -> ROLE_USER
     * - ADMIN -> ROLE_ADMIN
     * - OPERATOR -> ROLE_OPERATOR
     * - VALET_CHARGER -> ROLE_VALET_CHARGER
     */
    private List<SimpleGrantedAuthority> buildAuthorities(String userType) {
        if (userType == null) {
            return Collections.emptyList();
        }

        // Spring Security要求角色必须以"ROLE_"开头
        String role = "ROLE_" + userType;
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    /**
     * 发送401未授权响应
     * 安全修复: 使用JSON转义防止注入，避免String.format直接拼接
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> body = Map.of("code", 401, "message", message, "data", "");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
    }
}
