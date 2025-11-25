package com.ev.charging.controller.admin;

import com.ev.charging.aspect.CacheStatisticsAspect;
import com.ev.charging.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 性能监控控制器
 * 提供缓存统计、性能指标查询等功能
 */
@Tag(name = "管理后台 - 性能监控", description = "缓存统计、性能指标查询等功能")
@RestController
@RequestMapping(value = "/admin/performance", produces = "application/json;charset=UTF-8")
@RequiredArgsConstructor
public class PerformanceMonitorController {

    private final CacheStatisticsAspect cacheStatisticsAspect;

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计数据
     */
    @Operation(
            summary = "获取缓存统计",
            description = "查询系统缓存命中率、命中次数、未命中次数等统计信息",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/cache-stats")
    public Result<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("hitRate", String.format("%.2f%%", cacheStatisticsAspect.getHitRate()));
        stats.put("hits", cacheStatisticsAspect.getCacheHits());
        stats.put("misses", cacheStatisticsAspect.getCacheMisses());
        stats.put("total", cacheStatisticsAspect.getCacheHits() + cacheStatisticsAspect.getCacheMisses());

        return Result.success(stats);
    }

    /**
     * 重置缓存统计
     *
     * @return 操作结果
     */
    @Operation(
            summary = "重置缓存统计",
            description = "重置缓存命中统计计数器",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "重置成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PostMapping("/cache-stats/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> resetCacheStats() {
        cacheStatisticsAspect.reset();
        return Result.success();
    }
}
