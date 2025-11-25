package com.ev.charging.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存统计切面
 * 记录缓存命中率
 */
@Slf4j
@Aspect
@Component
public class CacheStatisticsAspect {

    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    /**
     * 定义切点：CacheService的get方法
     */
    @Pointcut("execution(* com.ev.charging.service.CacheService.get(..))")
    public void cacheGetMethods() {
    }

    /**
     * 环绕通知：统计缓存命中率
     */
    @Around("cacheGetMethods()")
    public Object monitorCacheHit(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        // 统计命中/未命中
        if (result != null) {
            cacheHits.incrementAndGet();
        } else {
            cacheMisses.incrementAndGet();
        }

        // 每100次访问输出一次统计
        long total = cacheHits.get() + cacheMisses.get();
        if (total % 100 == 0) {
            double hitRate = (double) cacheHits.get() / total * 100;
            log.info("[CACHE STATS] Hit rate: {}% (hits: {}, misses: {}, total: {})",
                    String.format("%.2f", hitRate), cacheHits.get(), cacheMisses.get(), total);
        }

        return result;
    }

    /**
     * 获取缓存命中率
     *
     * @return 命中率（0-100）
     */
    public double getHitRate() {
        long total = cacheHits.get() + cacheMisses.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) cacheHits.get() / total * 100;
    }

    /**
     * 获取缓存命中次数
     *
     * @return 命中次数
     */
    public long getCacheHits() {
        return cacheHits.get();
    }

    /**
     * 获取缓存未命中次数
     *
     * @return 未命中次数
     */
    public long getCacheMisses() {
        return cacheMisses.get();
    }

    /**
     * 重置统计数据
     */
    public void reset() {
        cacheHits.set(0);
        cacheMisses.set(0);
        log.info("[CACHE STATS] Statistics reset");
    }
}
