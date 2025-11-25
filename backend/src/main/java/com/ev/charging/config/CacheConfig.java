package com.ev.charging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存配置类
 * 提供分层缓存TTL配置和高级缓存策略
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cache")
public class CacheConfig {

    /**
     * TTL配置（秒）- 分层缓存策略
     */
    private TtlConfig ttl = new TtlConfig();

    /**
     * 防护机制配置
     */
    private ProtectionConfig protection = new ProtectionConfig();

    /**
     * 预热策略配置
     */
    private WarmupConfig warmup = new WarmupConfig();

    /**
     * 监控配置
     */
    private MonitorConfig monitor = new MonitorConfig();

    @Data
    public static class TtlConfig {
        // ========== 实时数据层（高更新频率，5-30秒）==========
        private long pileStatus = 10;              // 充电桩状态
        private long queueStatus = 15;             // 排队状态
        private long orderStatus = 20;             // 订单状态
        private long v2gStatus = 20;               // V2G状态
        private long energyRealtime = 30;          // 能源实时数据

        // ========== 准实时数据层（中等更新频率，1-10分钟）==========
        private long stationList = 300;            // 充电站列表（5分钟）
        private long pileHealth = 600;             // 充电桩健康度（10分钟）
        private long userCurrentOrder = 300;       // 用户当前订单（5分钟）
        private long queueList = 180;              // 排队列表（3分钟）
        private long nearbySearch = 420;           // 周边搜索（7分钟）

        // ========== 稳定数据层（低更新频率，30分钟-2小时）==========
        private long userInfo = 1800;              // 用户信息（30分钟）
        private long stationDetail = 3600;         // 充电站详情（1小时）
        private long creditProducts = 7200;        // 积分商品（2小时）
        private long orderList = 1800;             // 订单列表（30分钟）
        private long creditRecords = 3600;         // 积分记录（1小时）
        private long valetChargers = 3600;         // 代充师傅列表（1小时）

        // ========== 静态数据层（极低更新频率，4-24小时）==========
        private long poiSearch = 14400;            // POI搜索（4小时）
        private long systemConfig = 86400;         // 系统配置（24小时）
        private long dictionary = 86400;           // 字典数据（24小时）
        private long operatorInfo = 43200;         // 运营商信息（12小时）
        private long faultTypes = 86400;           // 故障类型（24小时）

        // ========== 特殊配置 ==========
        private long nullValue = 60;               // 空值缓存TTL（防穿透）
        private long lockExpire = 10;              // 分布式锁过期时间
        private long hotKeyExtension = 300;        // 热点key延长时间（5分钟）

        /**
         * 获取所有TTL配置（用于监控和文档生成）
         */
        public Map<String, Long> getAllTtl() {
            Map<String, Long> ttlMap = new HashMap<>();
            // 实时层
            ttlMap.put("pile-status", pileStatus);
            ttlMap.put("queue-status", queueStatus);
            ttlMap.put("order-status", orderStatus);
            ttlMap.put("v2g-status", v2gStatus);
            ttlMap.put("energy-realtime", energyRealtime);

            // 准实时层
            ttlMap.put("station-list", stationList);
            ttlMap.put("pile-health", pileHealth);
            ttlMap.put("user-current-order", userCurrentOrder);
            ttlMap.put("queue-list", queueList);
            ttlMap.put("nearby-search", nearbySearch);

            // 稳定层
            ttlMap.put("user-info", userInfo);
            ttlMap.put("station-detail", stationDetail);
            ttlMap.put("credit-products", creditProducts);
            ttlMap.put("order-list", orderList);
            ttlMap.put("credit-records", creditRecords);
            ttlMap.put("valet-chargers", valetChargers);

            // 静态层
            ttlMap.put("poi-search", poiSearch);
            ttlMap.put("system-config", systemConfig);
            ttlMap.put("dictionary", dictionary);
            ttlMap.put("operator-info", operatorInfo);
            ttlMap.put("fault-types", faultTypes);

            return ttlMap;
        }

        /**
         * 获取分层信息（用于文档和监控）
         */
        public Map<String, String> getLayerInfo(String key) {
            Map<String, String> info = new HashMap<>();

            if (key.contains("pile:status") || key.contains("queue:status") ||
                    key.contains("order:status") || key.contains("v2g:status") ||
                    key.contains("energy:realtime")) {
                info.put("layer", "realtime");
                info.put("description", "实时数据层 (5-30秒)");
            } else if (key.contains("station:list") || key.contains("pile:health") ||
                    key.contains("user:current") || key.contains("queue:list") ||
                    key.contains("nearby:search")) {
                info.put("layer", "near-realtime");
                info.put("description", "准实时数据层 (1-10分钟)");
            } else if (key.contains("user:info") || key.contains("station:detail") ||
                    key.contains("credit:products") || key.contains("order:list") ||
                    key.contains("credit:records") || key.contains("valet:chargers")) {
                info.put("layer", "stable");
                info.put("description", "稳定数据层 (30分钟-2小时)");
            } else if (key.contains("poi:search") || key.contains("system:config") ||
                    key.contains("dictionary") || key.contains("operator:info") ||
                    key.contains("fault:types")) {
                info.put("layer", "static");
                info.put("description", "静态数据层 (4-24小时)");
            } else {
                info.put("layer", "unknown");
                info.put("description", "未分类");
            }

            return info;
        }
    }

    @Data
    public static class ProtectionConfig {
        /**
         * 启用缓存击穿防护（分布式锁）
         */
        private boolean enableBreakdownProtection = true;

        /**
         * 启用缓存穿透防护（空值缓存）
         */
        private boolean enablePenetrationProtection = true;

        /**
         * 启用缓存雪崩防护（TTL随机化）
         */
        private boolean enableAvalancheProtection = true;

        /**
         * 启用热点key保护
         */
        private boolean enableHotKeyProtection = true;

        /**
         * TTL随机化范围（百分比）
         */
        private double ttlRandomRange = 0.2;  // ±20%

        /**
         * 热点key访问阈值（每分钟访问次数）
         */
        private int hotKeyThreshold = 100;

        /**
         * 热点key检测窗口（秒）
         */
        private int hotKeyWindow = 60;
    }

    @Data
    public static class WarmupConfig {
        /**
         * 启用启动时预热
         */
        private boolean enableStartupWarmup = true;

        /**
         * 启用定时刷新
         */
        private boolean enableScheduledRefresh = true;

        /**
         * 定时刷新间隔（分钟）
         */
        private int refreshInterval = 30;

        /**
         * 预热数据项
         */
        private WarmupItems items = new WarmupItems();

        @Data
        public static class WarmupItems {
            private boolean activeStations = true;         // 营业中的充电站
            private boolean stationDetails = true;         // 充电站详情
            private boolean systemConfigs = true;          // 系统配置
            private boolean creditProducts = true;         // 积分商品
            private boolean operatorInfo = false;          // 运营商信息（可选）
            private boolean popularPoi = true;             // 热门POI
        }
    }

    @Data
    public static class MonitorConfig {
        /**
         * 启用缓存监控
         */
        private boolean enabled = true;

        /**
         * 启用命中率统计
         */
        private boolean enableHitRateStats = true;

        /**
         * 启用热点key追踪
         */
        private boolean enableHotKeyTracking = true;

        /**
         * 统计窗口大小（秒）
         */
        private int statsWindow = 300;  // 5分钟

        /**
         * 慢查询阈值（毫秒）
         */
        private long slowQueryThreshold = 100;

        /**
         * 启用性能日志
         */
        private boolean enablePerformanceLog = false;  // 默认关闭，避免日志过多
    }

    /**
     * 计算带随机偏移的TTL
     *
     * @param baseTtl 基础TTL
     * @return 随机化后的TTL
     */
    public long getRandomizedTtl(long baseTtl) {
        if (!protection.enableAvalancheProtection) {
            return baseTtl;
        }

        double randomRange = protection.ttlRandomRange;
        long randomOffset = (long) (baseTtl * randomRange * (Math.random() * 2 - 1));
        return Math.max(1, baseTtl + randomOffset);
    }

    /**
     * 判断是否为热点key
     *
     * @param accessCount 访问次数
     * @return 是否为热点
     */
    public boolean isHotKey(int accessCount) {
        return protection.enableHotKeyProtection &&
                accessCount >= protection.hotKeyThreshold;
    }
}
