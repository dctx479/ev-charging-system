package com.ev.charging.service.impl;

import com.ev.charging.service.TokenBlacklistService;
import com.ev.charging.exception.BusinessException;
import com.ev.charging.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Token黑名单服务实现
 * 
 * Redis Key设计:
 * 1. 单个token黑名单: token:blacklist:{token} -> userId (TTL: token剩余有效期)
 * 2. 用户强制下线标记: user:blacklist:{userId} -> timestamp (TTL: token最大有效期)
 * 
 * 工作原理:
 * - 用户登出: 将token加入黑名单，自动过期
 * - 强制下线: 记录下线时间戳，之前生成的所有token失效
 * - 验证: 检查token是否在黑名单 + 是否早于用户下线时间
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtil jwtUtil;

    // Redis Key前缀
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String USER_BLACKLIST_PREFIX = "user:blacklist:";

    @Override
    public void addToBlacklist(String token, long expireSeconds) {
        try {
            // 解析token获取用户ID
            Long userId = jwtUtil.getUserIdFromToken(token);
            String key = TOKEN_BLACKLIST_PREFIX + token;
            
            // 存储到Redis，值为用户ID，便于后续分析
            redisTemplate.opsForValue().set(key, userId, expireSeconds, TimeUnit.SECONDS);
            
            log.info("Token加入黑名单: userId={}, expireIn={}s", userId, expireSeconds);
        } catch (Exception e) {
            log.error("添加token到黑名单失败: token={}", token, e);
            throw new BusinessException("添加token到黑名单失败", e);
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        try {
            String key = TOKEN_BLACKLIST_PREFIX + token;
            Boolean exists = redisTemplate.hasKey(key);

            if (Boolean.TRUE.equals(exists)) {
                log.debug("Token在黑名单中: token={}", token);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("检查token黑名单失败，采用fail-close策略: token={}", token, e);
            // 安全策略: fail-close - Redis故障时拒绝所有请求
            // 原因: 比allow-all更安全，用户最多需要重新登录
            // 比起允许已注销的token继续使用造成的安全漏洞，这个开销更可控
            return true;
        }
    }

    @Override
    public void blacklistUserTokens(Long userId, long expireSeconds) {
        try {
            String key = USER_BLACKLIST_PREFIX + userId;
            // 记录强制下线的时间戳（毫秒）
            long timestamp = System.currentTimeMillis();
            
            redisTemplate.opsForValue().set(key, timestamp, expireSeconds, TimeUnit.SECONDS);
            
            log.warn("用户所有token已加入黑名单（强制下线）: userId={}, timestamp={}", userId, timestamp);
        } catch (Exception e) {
            log.error("强制用户下线失败: userId={}", userId, e);
            throw new BusinessException("强制用户下线失败", e);
        }
    }

    @Override
    public boolean isUserBlacklisted(Long userId, String currentToken) {
        try {
            String key = USER_BLACKLIST_PREFIX + userId;
            Object value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                // 用户未被强制下线
                return false;
            }

            // 获取强制下线时间戳
            long blacklistTimestamp = Long.parseLong(value.toString());

            // 获取当前token的签发时间
            long tokenIssuedAt = jwtUtil.getIssuedAtFromToken(currentToken);

            // 如果token在强制下线之前签发，则失效
            if (tokenIssuedAt < blacklistTimestamp) {
                log.debug("Token早于用户强制下线时间: userId={}, tokenTime={}, blacklistTime={}",
                         userId, tokenIssuedAt, blacklistTimestamp);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("检查用户黑名单失败，采用fail-close策略: userId={}", userId, e);
            // 安全策略: fail-close - Redis故障时拒绝请求
            // 原因: 用户强制下线是管理员操作，Redis故障时保守处理更安全
            return true;
        }
    }

    @Override
    public void removeFromBlacklist(String token) {
        try {
            String key = TOKEN_BLACKLIST_PREFIX + token;
            redisTemplate.delete(key);
            log.info("Token从黑名单移除: token={}", token);
        } catch (Exception e) {
            log.error("从黑名单移除token失败: token={}", token, e);
        }
    }

    @Override
    public long getBlacklistCount() {
        try {
            // 使用SCAN迭代式扫描替代KEYS命令，避免阻塞Redis
            Set<String> tokenKeys = new HashSet<>();
            redisTemplate.execute((RedisCallback<Object>) connection -> {
                Cursor<byte[]> cursor = connection.scan(
                    ScanOptions.scanOptions().match(TOKEN_BLACKLIST_PREFIX + "*").count(100).build()
                );
                while (cursor.hasNext()) {
                    tokenKeys.add(new String(cursor.next()));
                }
                return null;
            });
            long tokenCount = tokenKeys.size();

            // 统计强制下线用户数量
            Set<String> userKeys = new HashSet<>();
            redisTemplate.execute((RedisCallback<Object>) connection -> {
                Cursor<byte[]> cursor = connection.scan(
                    ScanOptions.scanOptions().match(USER_BLACKLIST_PREFIX + "*").count(100).build()
                );
                while (cursor.hasNext()) {
                    userKeys.add(new String(cursor.next()));
                }
                return null;
            });
            long userCount = userKeys.size();

            log.debug("黑名单统计: token数量={}, 强制下线用户数量={}", tokenCount, userCount);
            return tokenCount;
        } catch (Exception e) {
            log.error("获取黑名单数量失败", e);
            return 0;
        }
    }
}
