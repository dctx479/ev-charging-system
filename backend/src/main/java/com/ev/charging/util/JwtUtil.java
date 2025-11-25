package com.ev.charging.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 * 
 * 安全特性:
 * 1. 强制从环境变量读取JWT密钥
 * 2. 启动时验证密钥长度(≥32字符)
 * 3. 使用HMAC-SHA256签名
 * 4. Token自动过期
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    /**
     * 启动时验证JWT配置
     * 确保密钥安全性
     */
    @PostConstruct
    public void init() {
        // 验证密钥是否配置
        if (secret == null || secret.trim().isEmpty()) {
            String errorMsg = "【安全错误】JWT密钥未配置！\n" +
                            "请设置环境变量: JWT_SECRET\n" +
                            "生成强密钥: openssl rand -base64 32";
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 验证密钥长度
        if (secret.length() < 32) {
            String errorMsg = String.format(
                "【安全错误】JWT密钥长度不足！\n" +
                "当前长度: %d 字符\n" +
                "最小要求: 32 字符\n" +
                "建议长度: 64+ 字符\n" +
                "生成强密钥: openssl rand -base64 64",
                secret.length()
            );
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 验证过期时间合理性
        if (expiration < 3600000) { // 小于1小时
            log.warn("【安全警告】JWT过期时间过短: {} 毫秒，建议至少1小时", expiration);
        }
        if (expiration > 604800000) { // 大于7天
            log.warn("【安全警告】JWT过期时间过长: {} 毫秒，建议不超过7天", expiration);
        }

        log.info("【安全初始化】JWT配置验证通过 - 密钥长度: {} 字符, 过期时间: {} 小时", 
                secret.length(), expiration / 3600000);
    }

    /**
     * 获取密钥
     */
    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成Token
     *
     * @param claims 自定义声明
     * @return Token字符串
     */
    public String generateToken(Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSignKey())
                .compact();
    }

    /**
     * 从Token中获取声明
     *
     * @param token Token字符串
     * @return 声明信息
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token Token字符串
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        // 使用 Number 中间转换，避免 JJWT 将小整数序列化为 Integer 导致 ClassCastException
        Object userIdObj = claims.get("userId");
        if (userIdObj == null) {
            return null;
        }
        return ((Number) userIdObj).longValue();
    }

    /**
     * 从Token中获取用户名
     *
     * @param token Token字符串
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 从Token中获取用户类型
     *
     * @param token Token字符串
     * @return 用户类型
     */
    public String getUserTypeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userType", String.class);
    }

    /**
     * 从Token中获取签发时间（毫秒时间戳）
     *
     * @param token Token字符串
     * @return 签发时间戳（毫秒）
     */
    public long getIssuedAtFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Date issuedAt = claims.getIssuedAt();
        return issuedAt != null ? issuedAt.getTime() : 0;
    }

    /**
     * 从Token中获取过期时间
     *
     * @param token Token字符串
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * 获取Token剩余有效期（秒）
     *
     * @param token Token字符串
     * @return 剩余有效期（秒），如果已过期返回0
     */
    public long getExpireSeconds(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            long now = System.currentTimeMillis();
            long expire = expiration.getTime();
            long remainingSeconds = (expire - now) / 1000;
            return Math.max(remainingSeconds, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 验证Token是否过期
     *
     * @param token Token字符串
     * @return true-已过期，false-未过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 验证Token是否有效
     *
     * @param token Token字符串
     * @return true-有效，false-无效
     */
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从HTTP请求中提取Token
     *
     * @param request HTTP请求对象
     * @return Token字符串，如果不存在返回null
     */
    public String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
