package com.ev.charging.service;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 统一缓存管理服务
 * 提供缓存的增删改查、预热、失效等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 缓存Key前缀
    public static final String PILE_STATUS_PREFIX = "pile:status:";
    public static final String STATION_LIST_PREFIX = "station:list:";
    public static final String USER_INFO_PREFIX = "user:info:";
    public static final String POI_SEARCH_PREFIX = "poi:search:";
    public static final String STATION_DETAIL_PREFIX = "station:detail:";
    public static final String ORDER_LIST_PREFIX = "order:list:";
    public static final String QUEUE_STATUS_PREFIX = "queue:status:";

    // 缓存TTL（秒）
    public static final long PILE_STATUS_TTL = 10;           // 充电桩状态：10秒
    public static final long STATION_LIST_TTL = 300;         // 充电站列表：5分钟
    public static final long USER_INFO_TTL = 1800;           // 用户信息：30分钟
    public static final long POI_SEARCH_TTL = 3600;          // POI搜索：1小时
    public static final long STATION_DETAIL_TTL = 600;       // 充电站详情：10分钟
    public static final long ORDER_LIST_TTL = 60;            // 订单列表：1分钟
    public static final long QUEUE_STATUS_TTL = 5;           // 排队状态：5秒

    /**
     * 设置缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param ttl   过期时间（秒）
     */
    public void set(String key, Object value, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
            log.debug("Cache set: key={}, ttl={}s", key, ttl);
        } catch (Exception e) {
            log.error("Failed to set cache: key={}", key, e);
        }
    }

    /**
     * 获取缓存
     *
     * @param key   缓存键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                log.debug("Cache miss: key={}", key);
                return null;
            }
            log.debug("Cache hit: key={}", key);

            // 如果是String类型，尝试JSON反序列化
            if (value instanceof String && !clazz.equals(String.class)) {
                return JSON.parseObject((String) value, clazz);
            }

            return (T) value;
        } catch (Exception e) {
            log.error("Failed to get cache: key={}", key, e);
            return null;
        }
    }

    /**
     * 删除缓存
     *
     * @param key 缓存键
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Cache deleted: key={}", key);
        } catch (Exception e) {
            log.error("Failed to delete cache: key={}", key, e);
        }
    }

    /**
     * 批量删除缓存（通过模式匹配）
     * 使用SCAN迭代式扫描替代KEYS命令，避免阻塞Redis
     *
     * @param pattern 键模式（如 "pile:status:*"）
     */
    public void deleteByPattern(String pattern) {
        try {
            Set<String> keys = new HashSet<>();
            redisTemplate.execute((RedisCallback<Object>) connection -> {
                try (Cursor<byte[]> cursor = connection.scan(
                    ScanOptions.scanOptions().match(pattern).count(100).build()
                )) {
                    while (cursor.hasNext()) {
                        keys.add(new String(cursor.next()));
                    }
                } catch (Exception e) {
                    log.error("Failed to close Redis cursor: pattern={}", pattern, e);
                }
                return null;
            });
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cache deleted by pattern: pattern={}, count={}", pattern, keys.size());
            }
        } catch (Exception e) {
            log.error("Failed to delete cache by pattern: pattern={}", pattern, e);
        }
    }

    /**
     * 检查缓存是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Failed to check cache existence: key={}", key, e);
            return false;
        }
    }

    /**
     * 设置缓存过期时间
     *
     * @param key 缓存键
     * @param ttl 过期时间（秒）
     */
    public void expire(String key, long ttl) {
        try {
            redisTemplate.expire(key, Duration.ofSeconds(ttl));
            log.debug("Cache expiration set: key={}, ttl={}s", key, ttl);
        } catch (Exception e) {
            log.error("Failed to set cache expiration: key={}", key, e);
        }
    }

    /**
     * 获取缓存剩余过期时间
     *
     * @param key 缓存键
     * @return 剩余秒数，-1表示永不过期，-2表示不存在
     */
    public long getTtl(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null ? ttl : -2;
        } catch (Exception e) {
            log.error("Failed to get cache TTL: key={}", key, e);
            return -2;
        }
    }

    /**
     * 清除所有缓存（慎用）
     * 使用SCAN迭代式扫描替代KEYS命令，避免阻塞Redis
     */
    public void clearAll() {
        try {
            Set<String> keys = new HashSet<>();
            redisTemplate.execute((RedisCallback<Object>) connection -> {
                try (Cursor<byte[]> cursor = connection.scan(
                    ScanOptions.scanOptions().match("*").count(100).build()
                )) {
                    while (cursor.hasNext()) {
                        keys.add(new String(cursor.next()));
                    }
                } catch (Exception e) {
                    log.error("Failed to close Redis cursor during clearAll", e);
                }
                return null;
            });
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.warn("All cache cleared: count={}", keys.size());
            }
        } catch (Exception e) {
            log.error("Failed to clear all cache", e);
        }
    }

    /**
     * 构建充电桩状态缓存键
     *
     * @param pileId 充电桩ID
     * @return 缓存键
     */
    public String buildPileStatusKey(Long pileId) {
        return PILE_STATUS_PREFIX + pileId;
    }

    /**
     * 构建充电站列表缓存键
     *
     * @param lat    纬度
     * @param lng    经度
     * @param radius 半径
     * @return 缓存键
     */
    public String buildStationListKey(Double lat, Double lng, Double radius) {
        return String.format("%s%.4f:%.4f:%.1f", STATION_LIST_PREFIX, lat, lng, radius);
    }

    /**
     * 构建用户信息缓存键
     *
     * @param userId 用户ID
     * @return 缓存键
     */
    public String buildUserInfoKey(Long userId) {
        return USER_INFO_PREFIX + userId;
    }

    /**
     * 构建POI搜索缓存键
     *
     * @param keyword 关键词
     * @param lat     纬度
     * @param lng     经度
     * @param radius  半径
     * @return 缓存键
     */
    public String buildPoiSearchKey(String keyword, Double lat, Double lng, Double radius) {
        return String.format("%s%s:%.4f:%.4f:%.1f", POI_SEARCH_PREFIX, keyword, lat, lng, radius);
    }

    /**
     * 构建充电站详情缓存键
     *
     * @param stationId 充电站ID
     * @return 缓存键
     */
    public String buildStationDetailKey(Long stationId) {
        return STATION_DETAIL_PREFIX + stationId;
    }

    /**
     * 构建订单列表缓存键
     *
     * @param userId 用户ID
     * @param status 订单状态
     * @param page   页码
     * @return 缓存键
     */
    public String buildOrderListKey(Long userId, Byte status, int page) {
        return String.format("%s%d:%d:%d", ORDER_LIST_PREFIX, userId, status != null ? status : 0, page);
    }

    /**
     * 构建排队状态缓存键
     *
     * @param stationId 充电站ID
     * @return 缓存键
     */
    public String buildQueueStatusKey(Long stationId) {
        return QUEUE_STATUS_PREFIX + stationId;
    }

    /**
     * 清除充电桩相关缓存
     *
     * @param pileId 充电桩ID
     */
    public void evictPileCache(Long pileId) {
        delete(buildPileStatusKey(pileId));
    }

    /**
     * 清除充电站相关缓存
     *
     * @param stationId 充电站ID
     */
    public void evictStationCache(Long stationId) {
        delete(buildStationDetailKey(stationId));
        deleteByPattern(STATION_LIST_PREFIX + "*");
        deleteByPattern(QUEUE_STATUS_PREFIX + stationId);
    }

    /**
     * 清除用户相关缓存
     *
     * @param userId 用户ID
     */
    public void evictUserCache(Long userId) {
        delete(buildUserInfoKey(userId));
        deleteByPattern(ORDER_LIST_PREFIX + userId + ":*");
    }

    /**
     * 清除订单相关缓存
     *
     * @param userId 用户ID
     */
    public void evictOrderCache(Long userId) {
        deleteByPattern(ORDER_LIST_PREFIX + userId + ":*");
    }
}
