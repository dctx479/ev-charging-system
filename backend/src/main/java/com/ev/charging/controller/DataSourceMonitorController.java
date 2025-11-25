package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.monitor.DataSourceHealthChecker;
import com.ev.charging.monitor.ReplicationLagMonitor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据源监控API
 * 提供主从延迟、数据源健康状态查询接口
 * 
 * @author Architecture Team
 * @since 2026-01-23
 */
@Slf4j
@RestController
@RequestMapping("/monitor/datasource")
@RequiredArgsConstructor
@Profile("replication")
@Tag(name = "数据源监控", description = "数据源健康状态和主从延迟监控接口")
public class DataSourceMonitorController {
    
    private final ReplicationLagMonitor replicationLagMonitor;
    private final DataSourceHealthChecker healthChecker;
    
    /**
     * 查询主从复制延迟
     * 
     * @return 主从延迟状态
     */
    @GetMapping("/replication-lag")
    @Operation(summary = "查询主从复制延迟", description = "返回所有从库的主从复制延迟（秒）")
    public Result<ReplicationLagMonitor.HealthStatus> getReplicationLag() {
        ReplicationLagMonitor.HealthStatus status = replicationLagMonitor.getHealthStatus();
        return Result.success(status);
    }
    
    /**
     * 查询数据源健康状态
     * 
     * @return 数据源健康状态
     */
    @GetMapping("/health")
    @Operation(summary = "查询数据源健康状态", description = "返回主库和所有从库的健康状态")
    public Result<DataSourceHealthChecker.HealthReport> getDataSourceHealth() {
        DataSourceHealthChecker.HealthReport report = healthChecker.getHealthReport();
        return Result.success(report);
    }
}
