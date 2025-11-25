package com.ev.charging.interceptor;

import com.alibaba.fastjson2.JSON;
import com.ev.charging.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 频率限制拦截器
 *
 * 安全特性:
 * 1. 基于IP的请求频率限制
 * 2. 不同接口不同限流策略
 * 3. Redis存储计数器
 * 4. 自动过期
 *
 * 防护场景:
 * - 暴力破解攻击(登录/注册)
 * - DOS攻击(支付/下单)
 * - 爬虫攻击(数据采集)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    /**
     * Lua脚本：原子递增并设置过期时间，解决increment+expire之间的TOCTOU竞态条件
     * 如果key不存在则创建并设置TTL，如果已存在则仅递增（不重置TTL）
     * 返回递增后的计数值
     */
    private static final String INCREMENT_WITH_EXPIRE_LUA =
            "local count = redis.call('incr', KEYS[1]) " +
            "if count == 1 then redis.call('expire', KEYS[1], ARGV[1]) end " +
            "return count";

    /**
     * 频率限制规则
     * key: 接口路径
     * value: 限制规则(最大请求数, 时间窗口秒数)
     */
    private static final Map<String, RateLimitRule> RULES = Map.ofEntries(
        // 认证接口 - 严格限制
        Map.entry("/api/auth/login", new RateLimitRule(5, 60, "登录接口")),           // 5次/分钟
        Map.entry("/api/auth/register", new RateLimitRule(3, 3600, "注册接口")),      // 3次/小时
        Map.entry("/api/auth/send-code", new RateLimitRule(1, 60, "验证码接口")),     // 1次/分钟

        // 支付接口 - 严格限制
        Map.entry("/api/orders/pay", new RateLimitRule(10, 60, "支付接口")),          // 10次/分钟
        Map.entry("/api/orders/create", new RateLimitRule(20, 60, "创建订单接口")),   // 20次/分钟

        // 查询接口 - 中等限制
        Map.entry("/api/stations/nearby", new RateLimitRule(30, 60, "附近站点查询")), // 30次/分钟
        Map.entry("/api/piles/list", new RateLimitRule(50, 60, "充电桩列表")),        // 50次/分钟

        // 操作接口 - 正常限制
        Map.entry("/api/orders/cancel", new RateLimitRule(10, 60, "取消订单")),       // 10次/分钟
        Map.entry("/api/queue/join", new RateLimitRule(20, 60, "加入排队")),          // 20次/分钟

        // AI接口 - 严格限制(计算密集)
        Map.entry("/api/ai/predict/duration", new RateLimitRule(10, 60, "AI预测")),  // 10次/分钟
        Map.entry("/api/ai/predict/fault", new RateLimitRule(10, 60, "故障预测"))     // 10次/分钟
    );

    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {
        
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // OPTIONS请求直接放行
        if ("OPTIONS".equals(method)) {
            return true;
        }
        
        // 检查是否有限流规则
        RateLimitRule rule = RULES.get(path);
        if (rule == null) {
            return true;  // 无规则，放行
        }
        
        // 获取客户端IP
        String clientIp = getClientIp(request);
        
        // 构建Redis key
        String key = "rate_limit:" + path + ":" + clientIp;

        // 原子递增+设置过期（Lua脚本保证原子性，解决increment与expire之间的TOCTOU竞态条件）
        Long count;
        try {
            count = redisTemplate.execute(
                new DefaultRedisScript<>(INCREMENT_WITH_EXPIRE_LUA, Long.class),
                Collections.singletonList(key),
                String.valueOf(rule.seconds)
            );
        } catch (Exception e) {
            // Redis异常，放行（fail-open策略：限流失败不应阻塞业务）
            log.warn("频率限制 - Redis执行失败: key={}, error={}", key, e.getMessage());
            return true;
        }

        if (count == null) {
            log.warn("频率限制 - Redis Lua返回null: key={}", key);
            return true;
        }

        if (count > rule.maxRequests) {
            // 超过限制
            log.warn("频率限制 - {}[{}] 超过限制 {}/{} 在 {}秒内",
                    clientIp, rule.description, count, rule.maxRequests, rule.seconds);

            // 返回429错误
            response.setStatus(429);  // Too Many Requests
            response.setContentType("application/json;charset=UTF-8");

            Result<Object> result = Result.error(429,
                String.format("请求过于频繁，请 %d 秒后再试", rule.seconds));
            response.getWriter().write(JSON.toJSONString(result));

            return false;
        }

        log.debug("频率限制 - {}[{}] 访问计数 {}/{} {}",
                clientIp, rule.description, count, rule.maxRequests, path);
        
        return true;
    }

    /**
     * 获取客户端真实IP
     * 安全修复: 优先使用getRemoteAddr()防止X-Forwarded-For头伪造绕过限流
     * 注意: 如果部署在反向代理(Nginx)后面，需要在代理层配置信任的X-Forwarded-For
     */
    private String getClientIp(HttpServletRequest request) {
        // 优先使用TCP连接的远程地址（不可伪造）
        String ip = request.getRemoteAddr();
        return ip != null ? ip.trim() : "unknown";
    }

    /**
     * 频率限制规则
     */
    @Data
    @AllArgsConstructor
    static class RateLimitRule {
        /**
         * 最大请求数
         */
        int maxRequests;
        
        /**
         * 时间窗口（秒）
         */
        int seconds;
        
        /**
         * 规则描述
         */
        String description;
    }
}
