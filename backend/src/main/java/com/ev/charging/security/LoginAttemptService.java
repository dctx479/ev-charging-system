package com.ev.charging.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 登录尝试限制服务
 *
 * 功能:
 * 1. 记录登录失败次数（基于Redis）
 * 2. 超过阈值后锁定账户
 * 3. 自动解锁机制（基于Redis TTL）
 *
 * 安全特性:
 * - 防止暴力破解攻击
 * - 基于IP和用户名双重限制
 * - 支持分布式部署，状态共享到Redis
 * - 使用INCR原子操作，确保计数准确
 *
 * Redis Key设计:
 * 1. 用户名登录失败: login:attempt:user:{username} -> 失败次数 (TTL: 15分钟)
 * 2. IP登录失败: login:attempt:ip:{ip} -> 失败次数 (TTL: 15分钟)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    // 最大失败次数
    private static final int MAX_ATTEMPTS = 5;

    // 锁定时间(分钟)
    private static final int LOCK_TIME_MINUTES = 15;

    // Redis Key前缀
    private static final String LOGIN_ATTEMPT_USER_PREFIX = "login:attempt:user:";
    private static final String LOGIN_ATTEMPT_IP_PREFIX = "login:attempt:ip:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Lua脚本：原子递增并设置过期时间，解决increment+expire之间的TOCTOU竞态条件
     */
    private static final String INCREMENT_WITH_EXPIRE_LUA =
            "local count = redis.call('incr', KEYS[1]) " +
            "if count == 1 then redis.call('expire', KEYS[1], ARGV[1]) end " +
            "return count";

    /**
     * 登录成功，清除失败记录
     */
    public void loginSucceeded(String username, String ip) {
        try {
            redisTemplate.delete(LOGIN_ATTEMPT_USER_PREFIX + username);
            redisTemplate.delete(LOGIN_ATTEMPT_IP_PREFIX + ip);
            log.info("登录成功，清除失败记录: username={}, ip={}", username, ip);
        } catch (Exception e) {
            log.error("清除登录记录失败: username={}, ip={}", username, ip, e);
            // 不抛异常，避免影响登录流程
        }
    }

    /**
     * 登录失败，记录失败次数（原子递增）
     */
    public void loginFailed(String username, String ip) {
        try {
            long ttlSeconds = LOCK_TIME_MINUTES * 60L;

            // 用户名计数递增（原子操作 + 原子设置过期时间，解决TOCTOU竞态条件）
            String userKey = LOGIN_ATTEMPT_USER_PREFIX + username;
            Long userAttempts = redisTemplate.execute(
                new DefaultRedisScript<>(INCREMENT_WITH_EXPIRE_LUA, Long.class),
                Collections.singletonList(userKey),
                String.valueOf(ttlSeconds)
            );

            // IP计数递增（原子操作 + 原子设置过期时间）
            String ipKey = LOGIN_ATTEMPT_IP_PREFIX + ip;
            Long ipAttempts = redisTemplate.execute(
                new DefaultRedisScript<>(INCREMENT_WITH_EXPIRE_LUA, Long.class),
                Collections.singletonList(ipKey),
                String.valueOf(ttlSeconds)
            );

            log.warn("登录失败: username={}, ip={}, userAttempts={}, ipAttempts={}",
                username, ip, userAttempts, ipAttempts);
        } catch (Exception e) {
            log.error("记录登录失败信息失败: username={}, ip={}", username, ip, e);
            // 不抛异常，避免影响登录流程
        }
    }

    /**
     * 检查用户是否被锁定
     *
     * 采用fail-open策略: 如果Redis不可用，允许登录尝试
     * 原因: 比起DoS拒绝合法用户，暂时放过更好
     */
    public boolean isBlocked(String username, String ip) {
        try {
            // 检查用户名是否被锁定
            String userKey = LOGIN_ATTEMPT_USER_PREFIX + username;
            String userAttemptStr = redisTemplate.opsForValue().get(userKey);
            if (userAttemptStr != null) {
                int attempts = Integer.parseInt(userAttemptStr);
                if (attempts >= MAX_ATTEMPTS) {
                    log.warn("用户被锁定: username={}, attempts={}", username, attempts);
                    return true;
                }
            }

            // 检查IP是否被锁定
            String ipKey = LOGIN_ATTEMPT_IP_PREFIX + ip;
            String ipAttemptStr = redisTemplate.opsForValue().get(ipKey);
            if (ipAttemptStr != null) {
                int attempts = Integer.parseInt(ipAttemptStr);
                if (attempts >= MAX_ATTEMPTS) {
                    log.warn("IP被锁定: ip={}, attempts={}", ip, attempts);
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            log.error("检查登录锁定状态失败，采用fail-open策略: username={}, ip={}", username, ip, e);
            // Redis故障时允许登录尝试，避免拒绝所有用户
            return false;
        }
    }

    /**
     * 获取剩余尝试次数
     */
    public int getRemainingAttempts(String username) {
        try {
            String userKey = LOGIN_ATTEMPT_USER_PREFIX + username;
            String attemptsStr = redisTemplate.opsForValue().get(userKey);
            if (attemptsStr == null) {
                return MAX_ATTEMPTS;
            }
            return Math.max(0, MAX_ATTEMPTS - Integer.parseInt(attemptsStr));
        } catch (Exception e) {
            log.error("获取剩余尝试次数失败: username={}", username, e);
            return MAX_ATTEMPTS;
        }
    }
}
