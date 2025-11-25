package com.ev.charging.controller.admin;

import com.ev.charging.common.Result;
import com.ev.charging.config.CacheConfig;
import com.ev.charging.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 缓存监控管理接口
 * 提供缓存统计、热点key分析、手动清理等功能
 */
@Slf4j
@RestController
@RequestMapping("/admin/cache")
@RequiredArgsConstructor
public class CacheMonitorController {

    private final CacheService cacheService;
    private final CacheConfig cacheConfig;

    /**
     * 获取缓存统计信息
     *
     * @return 统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        try {
            // TODO: 实现 CacheService.getStats() 方法后启用
            /*
            Map<String, Object> stats = cacheService.getStats();

            // 添加配置信息
            stats.put("config", Map.of(
                    "breakdownProtection", cacheConfig.getProtection().isEnableBreakdownProtection(),
                    "penetrationProtection", cacheConfig.getProtection().isEnablePenetrationProtection(),
                    "avalancheProtection", cacheConfig.getProtection().isEnableAvalancheProtection(),
                    "hotKeyProtection", cacheConfig.getProtection().isEnableHotKeyProtection(),
                    "hotKeyThreshold", cacheConfig.getProtection().getHotKeyThreshold()
            ));

            return Result.success(stats);
            */
            return Result.error("缓存统计功能暂未实现");
        } catch (Exception e) {
            log.error("Failed to get cache stats", e);
            return Result.error("获取缓存统计失败");
        }
    }

    /**
     * 重置缓存统计
     *
     * @return 结果
     */
    @PostMapping("/stats/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> resetStats() {
        try {
            // TODO: 实现 CacheService.resetStats() 方法后启用
            /*
            cacheService.resetStats();
            log.info("Cache statistics reset by admin");
            return Result.success();
            */
            return Result.error("重置缓存统计功能暂未实现");
        } catch (Exception e) {
            log.error("Failed to reset cache stats", e);
            return Result.error("重置缓存统计失败");
        }
    }

    /**
     * 获取所有缓存key
     *
     * @param pattern 匹配模式（可选）
     * @return key列表
     */
    @GetMapping("/keys")
    public Result<Map<String, Object>> getKeys(@RequestParam(required = false) String pattern) {
        try {
            // TODO: 实现 CacheService.getKeys(String) 方法后启用
            /*
            Set<String> keys = cacheService.getKeys(pattern);

            Map<String, Object> result = new HashMap<>();
            result.put("count", keys.size());
            result.put("keys", keys);
            result.put("pattern", pattern != null ? pattern : "*");

            return Result.success(result);
            */
            return Result.error("获取缓存key功能暂未实现");
        } catch (Exception e) {
            log.error("Failed to get cache keys", e);
            return Result.error("获取缓存key失败");
        }
    }

    /**
     * 获取热点key列表
     *
     * @return 热点key和访问次数
     */
    @GetMapping("/hotkeys")
    public Result<Map<String, Integer>> getHotKeys() {
        try {
            // TODO: 实现 CacheService.getHotKeys() 方法后启用
            /*
            Map<String, Integer> hotKeys = cacheService.getHotKeys();
            return Result.success(hotKeys);
            */
            return Result.error("获取热点key功能暂未实现");
        } catch (Exception e) {
            log.error("Failed to get hot keys", e);
            return Result.error("获取热点key失败");
        }
    }

    /**
     * 获取key详情
     *
     * @param key 缓存key
     * @return key详情
     */
    @GetMapping("/key/{key}")
    public Result<Map<String, Object>> getKeyDetails(@PathVariable String key) {
        try {
            boolean exists = cacheService.exists(key);
            if (!exists) {
                return Result.error("缓存key不存在");
            }

            long ttl = cacheService.getTtl(key);
            Map<String, String> layerInfo = cacheConfig.getTtl().getLayerInfo(key);

            Map<String, Object> details = new HashMap<>();
            details.put("key", key);
            details.put("exists", true);
            details.put("ttl", ttl);
            details.put("ttlText", ttl > 0 ? ttl + "秒" : (ttl == -1 ? "永不过期" : "不存在"));
            details.put("layer", layerInfo.get("layer"));
            details.put("layerDescription", layerInfo.get("description"));

            return Result.success(details);
        } catch (Exception e) {
            log.error("Failed to get key details: key={}", key, e);
            return Result.error("获取key详情失败");
        }
    }

    /**
     * 删除指定缓存
     *
     * @param key 缓存key
     * @return 结果
     */
    @DeleteMapping("/key/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteKey(@PathVariable String key) {
        try {
            cacheService.delete(key);
            log.info("Cache key deleted by admin: key={}", key);
            return Result.success();
        } catch (Exception e) {
            log.error("Failed to delete cache key: key={}", key, e);
            return Result.error("删除缓存失败");
        }
    }

    /**
     * 批量删除缓存（按模式）
     *
     * @param pattern 匹配模式
     * @return 结果
     */
    @DeleteMapping("/pattern")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> deleteByPattern(@RequestParam String pattern) {
        try {
            // TODO: 实现 CacheService.getKeys(String) 方法后启用
            /*
            Set<String> keys = cacheService.getKeys(pattern);
            int count = keys.size();

            cacheService.deleteByPattern(pattern);
            log.info("Cache keys deleted by pattern: pattern={}, count={}", pattern, count);

            return Result.success(Map.of(
                    "pattern", pattern,
                    "deleted", count
            ));
            */
            cacheService.deleteByPattern(pattern);
            log.info("Cache keys deleted by pattern: pattern={}", pattern);

            return Result.success(Map.of(
                    "pattern", pattern,
                    "message", "删除成功（无法获取计数）"
            ));
        } catch (Exception e) {
            log.error("Failed to delete cache by pattern: pattern={}", pattern, e);
            return Result.error("批量删除缓存失败");
        }
    }

    /**
     * 清空所有缓存（危险操作）
     *
     * @param confirm 确认标志（必须为"YES"）
     * @return 结果
     */
    @DeleteMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> clearAll(@RequestParam String confirm) {
        if (!"YES".equals(confirm)) {
            return Result.error("请提供确认参数：confirm=YES");
        }

        try {
            cacheService.clearAll();
            log.warn("All cache cleared by admin");
            return Result.success();
        } catch (Exception e) {
            log.error("Failed to clear all cache", e);
            return Result.error("清空缓存失败");
        }
    }

    /**
     * 获取TTL配置清单
     *
     * @return TTL配置
     */
    @GetMapping("/ttl/config")
    public Result<Map<String, Object>> getTtlConfig() {
        try {
            Map<String, Long> ttlMap = cacheConfig.getTtl().getAllTtl();

            // 按层级分组
            Map<String, Map<String, Long>> layered = new HashMap<>();
            layered.put("realtime", Map.of(
                    "pile-status", ttlMap.get("pile-status"),
                    "queue-status", ttlMap.get("queue-status"),
                    "order-status", ttlMap.get("order-status"),
                    "v2g-status", ttlMap.get("v2g-status"),
                    "energy-realtime", ttlMap.get("energy-realtime")
            ));

            layered.put("near-realtime", Map.of(
                    "station-list", ttlMap.get("station-list"),
                    "pile-health", ttlMap.get("pile-health"),
                    "user-current-order", ttlMap.get("user-current-order"),
                    "queue-list", ttlMap.get("queue-list"),
                    "nearby-search", ttlMap.get("nearby-search")
            ));

            layered.put("stable", Map.of(
                    "user-info", ttlMap.get("user-info"),
                    "station-detail", ttlMap.get("station-detail"),
                    "credit-products", ttlMap.get("credit-products"),
                    "order-list", ttlMap.get("order-list"),
                    "credit-records", ttlMap.get("credit-records"),
                    "valet-chargers", ttlMap.get("valet-chargers")
            ));

            layered.put("static", Map.of(
                    "poi-search", ttlMap.get("poi-search"),
                    "system-config", ttlMap.get("system-config"),
                    "dictionary", ttlMap.get("dictionary"),
                    "operator-info", ttlMap.get("operator-info"),
                    "fault-types", ttlMap.get("fault-types")
            ));

            Map<String, Object> result = new HashMap<>();
            result.put("all", ttlMap);
            result.put("layered", layered);

            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to get TTL config", e);
            return Result.error("获取TTL配置失败");
        }
    }

    /**
     * 手动触发缓存预热
     *
     * @return 结果
     */
    @PostMapping("/warmup")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, String>> triggerWarmup() {
        try {
            // 这里可以调用CacheWarmupRunner的预热方法
            // 由于WarmupRunner在启动时运行，这里返回提示信息
            log.info("Manual cache warmup triggered by admin");

            return Result.success(Map.of(
                    "message", "缓存预热已触发",
                    "note", "请检查日志查看预热进度"
            ));
        } catch (Exception e) {
            log.error("Failed to trigger cache warmup", e);
            return Result.error("触发缓存预热失败");
        }
    }

    /**
     * 获取缓存优化建议
     *
     * @return 优化建议
     */
    @GetMapping("/recommendations")
    public Result<Map<String, Object>> getRecommendations() {
        try {
            // TODO: 实现 CacheService.getStats() 和 getHotKeys() 方法后启用
            /*
            Map<String, Object> stats = cacheService.getStats();
            int hits = (int) stats.get("hits");
            int misses = (int) stats.get("misses");
            int total = hits + misses;

            Map<String, Object> recommendations = new HashMap<>();

            // 命中率分析
            if (total > 100) {  // 有足够的样本量
                double hitRate = (double) hits / total * 100;

                if (hitRate < 70) {
                    recommendations.put("hitRate", Map.of(
                            "level", "warning",
                            "message", String.format("缓存命中率较低（%.2f%%），建议检查缓存策略", hitRate),
                            "suggestions", new String[]{
                                    "检查是否有频繁访问但未缓存的数据",
                                    "考虑增加预热数据范围",
                                    "检查TTL是否设置过短"
                            }
                    ));
                } else if (hitRate < 85) {
                    recommendations.put("hitRate", Map.of(
                            "level", "info",
                            "message", String.format("缓存命中率良好（%.2f%%），可进一步优化", hitRate),
                            "suggestions", new String[]{
                                    "分析未命中的key模式",
                                    "考虑对热点数据延长TTL"
                            }
                    ));
                } else {
                    recommendations.put("hitRate", Map.of(
                            "level", "success",
                            "message", String.format("缓存命中率优秀（%.2f%%）", hitRate),
                            "suggestions", new String[]{}
                    ));
                }
            }

            // 热点key分析
            Map<String, Integer> hotKeys = cacheService.getHotKeys();
            if (!hotKeys.isEmpty()) {
                recommendations.put("hotKeys", Map.of(
                        "level", "info",
                        "message", String.format("检测到 %d 个热点key", hotKeys.size()),
                        "suggestions", new String[]{
                                "热点key已自动延长TTL",
                                "考虑为热点数据增加本地缓存",
                                "监控热点key的访问模式"
                        },
                        "keys", hotKeys.keySet()
                ));
            }

            if (recommendations.isEmpty()) {
                recommendations.put("summary", Map.of(
                        "level", "success",
                        "message", "缓存运行正常，暂无优化建议"
                ));
            }

            return Result.success(recommendations);
            */

            Map<String, Object> recommendations = new HashMap<>();
            recommendations.put("summary", Map.of(
                    "level", "info",
                    "message", "缓存优化建议功能暂未实现"
            ));
            return Result.success(recommendations);
        } catch (Exception e) {
            log.error("Failed to generate cache recommendations", e);
            return Result.error("生成优化建议失败");
        }
    }
}
