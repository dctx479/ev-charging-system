package com.ev.charging.monitor;

import com.ev.charging.datasource.DataSourceContextHolder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源健康检查器
 * 定时检查所有数据源的健康状态，自动摘除不健康的从库
 * 
 * @author Architecture Team
 * @since 2026-01-23
 */
@Slf4j
@Component
@Profile("replication")
@ConditionalOnProperty(prefix = "replication.health-check", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataSourceHealthChecker {
    
    private final DataSource masterDataSource;
    private final DataSource slave1DataSource;
    private final DataSource slave2DataSource;
    
    /**
     * 数据源健康状态缓存
     * Key: 数据源名称 (master/slave1/slave2)
     * Value: 健康状态 (UP/DOWN)
     */
    private final Map<String, String> healthStatus = new ConcurrentHashMap<>();
    
    /**
     * 可用的从库列表
     */
    private final Set<String> availableSlaves = ConcurrentHashMap.newKeySet();
    
    public DataSourceHealthChecker(
            @Qualifier("masterDataSource") DataSource masterDataSource,
            @Qualifier("slave1DataSource") DataSource slave1DataSource,
            @Qualifier("slave2DataSource") DataSource slave2DataSource) {
        this.masterDataSource = masterDataSource;
        this.slave1DataSource = slave1DataSource;
        this.slave2DataSource = slave2DataSource;
        
        // 初始化：假设所有数据源都是健康的
        healthStatus.put(DataSourceContextHolder.MASTER, "UP");
        healthStatus.put(DataSourceContextHolder.SLAVE1, "UP");
        healthStatus.put(DataSourceContextHolder.SLAVE2, "UP");
        
        availableSlaves.add(DataSourceContextHolder.SLAVE1);
        availableSlaves.add(DataSourceContextHolder.SLAVE2);
    }
    
    /**
     * 定时健康检查（每5秒）
     */
    @Scheduled(fixedRateString = "${replication.health-check.interval:5000}")
    public void checkDataSourceHealth() {
        try {
            log.debug("开始数据源健康检查...");

            // 检查主库
            checkSingleDataSource(masterDataSource, DataSourceContextHolder.MASTER);

            // 检查slave1
            boolean slave1Healthy = checkSingleDataSource(slave1DataSource, DataSourceContextHolder.SLAVE1);
            updateAvailableSlaves(DataSourceContextHolder.SLAVE1, slave1Healthy);

            // 检查slave2
            boolean slave2Healthy = checkSingleDataSource(slave2DataSource, DataSourceContextHolder.SLAVE2);
            updateAvailableSlaves(DataSourceContextHolder.SLAVE2, slave2Healthy);

            log.debug("数据源健康检查完成: master={}, slave1={}, slave2={}",
                    healthStatus.get(DataSourceContextHolder.MASTER),
                    healthStatus.get(DataSourceContextHolder.SLAVE1),
                    healthStatus.get(DataSourceContextHolder.SLAVE2));
        } catch (Exception e) {
            log.error("数据源健康检查任务异常，调度器线程安全退出", e);
        }
    }
    
    /**
     * 检查单个数据源的健康状态
     * 
     * @param dataSource 数据源
     * @param dataSourceName 数据源名称
     * @return true=健康, false=不健康
     */
    private boolean checkSingleDataSource(DataSource dataSource, String dataSourceName) {
        try (Connection conn = dataSource.getConnection()) {
            // 执行ping查询
            boolean valid = conn.isValid(3);  // 3秒超时
            
            String status = valid ? "UP" : "DOWN";
            String previousStatus = healthStatus.put(dataSourceName, status);
            
            // 状态变化时记录日志
            if (!status.equals(previousStatus)) {
                if ("UP".equals(status)) {
                    log.info("✅ 数据源{}已恢复健康", dataSourceName);
                } else {
                    log.error("❌ 数据源{}健康检查失败", dataSourceName);
                }
            }
            
            return valid;
            
        } catch (SQLException e) {
            log.error("数据源{}健康检查异常: {}", dataSourceName, e.getMessage());
            healthStatus.put(dataSourceName, "DOWN");
            return false;
        }
    }
    
    /**
     * 更新可用从库列表
     * 
     * @param slaveName 从库名称
     * @param healthy 是否健康
     */
    private void updateAvailableSlaves(String slaveName, boolean healthy) {
        if (healthy) {
            boolean added = availableSlaves.add(slaveName);
            if (added) {
                log.info("✅ 从库{}已加入可用列表", slaveName);
            }
        } else {
            boolean removed = availableSlaves.remove(slaveName);
            if (removed) {
                log.warn("⚠️ 从库{}已从可用列表中移除", slaveName);
            }
        }
    }
    
    /**
     * 获取所有数据源的健康状态
     * 
     * @return 健康状态Map
     */
    public Map<String, String> getHealthStatus() {
        return new HashMap<>(healthStatus);
    }
    
    /**
     * 获取可用的从库列表
     * 
     * @return 从库名称列表
     */
    public List<String> getAvailableSlaves() {
        return new ArrayList<>(availableSlaves);
    }
    
    /**
     * 获取详细健康报告
     * 
     * @return 健康报告对象
     */
    public HealthReport getHealthReport() {
        HealthReport report = new HealthReport();
        report.setMasterStatus(healthStatus.get(DataSourceContextHolder.MASTER));
        report.setSlave1Status(healthStatus.get(DataSourceContextHolder.SLAVE1));
        report.setSlave2Status(healthStatus.get(DataSourceContextHolder.SLAVE2));
        report.setAvailableSlaves(getAvailableSlaves());
        
        // 判断整体健康状态
        boolean allHealthy = healthStatus.values().stream().allMatch(status -> "UP".equals(status));
        report.setOverallStatus(allHealthy ? "HEALTHY" : "DEGRADED");
        
        return report;
    }
    
    /**
     * 健康报告DTO
     */
    @Data
    public static class HealthReport {
        private String masterStatus;
        private String slave1Status;
        private String slave2Status;
        private List<String> availableSlaves;
        private String overallStatus;
    }
}
