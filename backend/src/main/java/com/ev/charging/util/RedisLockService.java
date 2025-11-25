package com.ev.charging.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁工具类
 *
 * 用于防止并发环境下的资源竞争，特别是充电桩占用场景
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisLockService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String UNLOCK_LUA_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    /**
     * 尝试获取锁（返回锁标识用于安全释放）
     *
     * @param key           锁的键
     * @param expireSeconds 锁的过期时间（秒）
     * @return 锁标识（UUID），null表示获取失败
     */
    public String tryLockWithOwner(String key, long expireSeconds) {
        try {
            String owner = UUID.randomUUID().toString();
            Boolean success = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, owner, expireSeconds, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(success)) {
                log.debug("获取锁成功: key={}, owner={}", key, owner);
                return owner;
            }
            log.debug("获取锁失败，锁已被占用: key={}", key);
            return null;
        } catch (Exception e) {
            log.error("获取锁异常: key={}", key, e);
            return null;
        }
    }

    /**
     * 安全释放锁（仅释放自己持有的锁）
     *
     * @param key   锁的键
     * @param owner 锁标识
     */
    public void unlockSafe(String key, String owner) {
        try {
            Long result = stringRedisTemplate.execute(
                    new DefaultRedisScript<>(UNLOCK_LUA_SCRIPT, Long.class),
                    Collections.singletonList(key), owner);
            if (result != null && result == 1) {
                log.debug("安全释放锁成功: key={}", key);
            } else {
                log.debug("安全释放锁跳过（非持有者或已过期）: key={}", key);
            }
        } catch (Exception e) {
            log.error("安全释放锁异常: key={}", key, e);
        }
    }

    /**
     * 尝试获取锁
     *
     * @param key           锁的键
     * @param expireSeconds 锁的过期时间（秒）
     * @return true-获取成功，false-获取失败
     */
    public boolean tryLock(String key, long expireSeconds) {
        try {
            Boolean success = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, String.valueOf(System.currentTimeMillis()),
                            expireSeconds, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(success)) {
                log.debug("获取锁成功: key={}", key);
                return true;
            } else {
                log.debug("获取锁失败，锁已被占用: key={}", key);
                return false;
            }
        } catch (Exception e) {
            log.error("获取锁异常: key={}", key, e);
            return false;
        }
    }

    /**
     * 尝试获取锁（带重试）
     *
     * @param key           锁的键
     * @param expireSeconds 锁的过期时间（秒）
     * @param retryCount    重试次数
     * @param retryInterval 重试间隔（毫秒）
     * @return true-获取成功，false-获取失败
     */
    public boolean tryLockWithRetry(String key, long expireSeconds, int retryCount, long retryInterval) {
        for (int i = 0; i < retryCount; i++) {
            if (tryLock(key, expireSeconds)) {
                return true;
            }

            // 如果不是最后一次重试，等待一段时间后重试
            if (i < retryCount - 1) {
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("重试等待被中断", e);
                    return false;
                }
            }
        }

        log.warn("获取锁失败，已重试{}次: key={}", retryCount, key);
        return false;
    }

    /**
     * 释放锁
     *
     * @param key 锁的键
     */
    public void unlock(String key) {
        try {
            stringRedisTemplate.delete(key);
            log.debug("释放锁成功: key={}", key);
        } catch (Exception e) {
            log.error("释放锁异常: key={}", key, e);
        }
    }

    /**
     * 检查锁是否存在
     *
     * @param key 锁的键
     * @return true-锁存在，false-锁不存在
     */
    public boolean isLocked(String key) {
        try {
            Boolean hasKey = stringRedisTemplate.hasKey(key);
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception e) {
            log.error("检查锁状态异常: key={}", key, e);
            return false;
        }
    }

    /**
     * 获取锁的剩余时间
     *
     * @param key 锁的键
     * @return 剩余时间（秒），-1表示没有过期时间，-2表示key不存在
     */
    public long getLockTTL(String key) {
        try {
            Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null ? ttl : -2;
        } catch (Exception e) {
            log.error("获取锁剩余时间异常: key={}", key, e);
            return -2;
        }
    }

    /**
     * 构建充电桩锁的键
     *
     * @param pileId 充电桩ID
     * @return 锁的键
     */
    public static String buildPileLockKey(Long pileId) {
        return "lock:pile:" + pileId;
    }

    /**
     * 构建站点排队锁的键
     *
     * @param stationId 充电站ID
     * @return 锁的键
     */
    public static String buildStationQueueLockKey(Long stationId) {
        return "lock:queue:station:" + stationId;
    }

    /**
     * 构建用户排队锁的键
     *
     * @param userId 用户ID
     * @return 锁的键
     */
    public static String buildUserQueueLockKey(Long userId) {
        return "lock:queue:user:" + userId;
    }
}
