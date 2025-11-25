package com.ev.charging.monitor;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 主从复制延迟监控
 * 定时检查从库的主从复制延迟，延迟过高时记录告警
 * 
 * @author Architecture Team
 * @since 2026-01-23
 */
@Slf4j
@Component
@Profile("replication")
@ConditionalOnProperty(prefix = "replication.monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReplicationLagMonitor {
    
    private final DataSource slave1DataSource;
    private final DataSource slave2DataSource;
    
    /**
     * 延迟告警阈值（秒）
     */
    private static final long LAG_THRESHOLD = 5;
    
    /**
     * 最近一次检查的延迟状态
     */
    private final Map<String, Long> latestLagStatus = new ConcurrentHashMap<>();
    
    public ReplicationLagMonitor(
            @Qualifier("slave1DataSource") DataSource slave1DataSource,
            @Qualifier("slave2DataSource") DataSource slave2DataSource) {
        this.slave1DataSource = slave1DataSource;
        this.slave2DataSource = slave2DataSource;
    }
    
    /**
     * 定时检查主从延迟（每10秒）
     */
    @Scheduled(fixedRateString = "${replication.monitor.interval:10000}")
    public void checkReplicationLag() {
        try {
            log.debug("开始检查主从复制延迟...");

            // 检查slave1
            Long slave1Lag = checkSingleSlaveLag(slave1DataSource, "slave1");
            if (slave1Lag != null) {
                latestLagStatus.put("slave1", slave1Lag);
                if (slave1Lag > LAG_THRESHOLD) {
                    log.warn("⚠️ Slave1主从延迟过高: {}秒 (阈值: {}秒)", slave1Lag, LAG_THRESHOLD);
                }
            }

            // 检查slave2
            Long slave2Lag = checkSingleSlaveLag(slave2DataSource, "slave2");
            if (slave2Lag != null) {
                latestLagStatus.put("slave2", slave2Lag);
                if (slave2Lag > LAG_THRESHOLD) {
                    log.warn("⚠️ Slave2主从延迟过高: {}秒 (阈值: {}秒)", slave2Lag, LAG_THRESHOLD);
                }
            }

            log.debug("主从延迟检查完成: slave1={}秒, slave2={}秒", slave1Lag, slave2Lag);
        } catch (Exception e) {
            log.error("主从延迟检查任务异常，调度器线程安全退出", e);
        }
    }
    
    /**
     * 检查单个从库的延迟
     * 
     * @param dataSource 从库数据源
     * @param slaveName 从库名称
     * @return 延迟秒数，查询失败返回null
     */
    private Long checkSingleSlaveLag(DataSource dataSource, String slaveName) {
        String sql = "SHOW SLAVE STATUS";
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                // Seconds_Behind_Master: 从库落后主库的秒数
                Long secondsBehind = rs.getLong("Seconds_Behind_Master");
                
                // 检查复制状态
                String ioRunning = rs.getString("Slave_IO_Running");
                String sqlRunning = rs.getString("Slave_SQL_Running");
                
                if (!"Yes".equalsIgnoreCase(ioRunning) || !"Yes".equalsIgnoreCase(sqlRunning)) {
                    log.error("❌ {}主从复制已停止！IO_Running={}, SQL_Running={}", 
                            slaveName, ioRunning, sqlRunning);
                    return null;
                }
                
                return secondsBehind;
            } else {
                log.warn("{}未配置主从复制或非从库", slaveName);
                return null;
            }
            
        } catch (Exception e) {
            log.error("检查{}主从延迟失败: {}", slaveName, e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取最新的延迟状态
     * 
     * @return 延迟状态Map (slave1 -> 延迟秒数, slave2 -> 延迟秒数)
     */
    public Map<String, Long> getLatestLagStatus() {
        return new ConcurrentHashMap<>(latestLagStatus);
    }
    
    /**
     * 获取健康状态
     * 
     * @return 健康状态对象
     */
    public HealthStatus getHealthStatus() {
        HealthStatus status = new HealthStatus();
        
        Long slave1Lag = latestLagStatus.get("slave1");
        Long slave2Lag = latestLagStatus.get("slave2");
        
        status.setSlave1Lag(slave1Lag);
        status.setSlave2Lag(slave2Lag);
        
        // 判断整体健康状态
        boolean healthy = true;
        if (slave1Lag != null && slave1Lag > LAG_THRESHOLD) {
            healthy = false;
        }
        if (slave2Lag != null && slave2Lag > LAG_THRESHOLD) {
            healthy = false;
        }
        
        status.setStatus(healthy ? "healthy" : "unhealthy");
        return status;
    }
    
    /**
     * 健康状态DTO
     */
    @Data
    public static class HealthStatus {
        private Long slave1Lag;
        private Long slave2Lag;
        private String status;
    }
}
