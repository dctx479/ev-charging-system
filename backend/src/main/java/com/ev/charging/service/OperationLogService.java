package com.ev.charging.service;

import com.ev.charging.entity.OperationLog;
import com.ev.charging.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    /**
     * 综合查询操作日志
     *
     * @param userId        用户ID
     * @param username      用户名（模糊查询）
     * @param module        操作模块
     * @param operationType 操作类型
     * @param status        操作状态
     * @param ip            IP地址
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @param pageable      分页参数
     * @return 操作日志分页数据
     */
    public Page<OperationLog> queryLogs(
            Long userId,
            String username,
            String module,
            String operationType,
            String status,
            String ip,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable) {

        // Sanitize username parameter to prevent SQL injection via LIKE queries
        if (username != null) {
            username = sanitizeLikeInput(username);
        }

        return operationLogRepository.findByConditions(
                userId, username, module, operationType, status, ip, startTime, endTime, pageable
        );
    }

    /**
     * Sanitize input for LIKE queries by escaping special characters
     *
     * @param input The input string to sanitize
     * @return Sanitized string with escaped special characters
     */
    private String sanitizeLikeInput(String input) {
        if (input == null) {
            return null;
        }
        // Escape backslash first, then % and _
        return input.replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
    }

    /**
     * 根据ID查询操作日志详情
     *
     * @param id 日志ID
     * @return 操作日志
     */
    public OperationLog getLogById(Long id) {
        return operationLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("操作日志不存在"));
    }

    /**
     * 根据用户ID查询操作日志
     *
     * @param userId   用户ID
     * @param pageable 分页参数
     * @return 操作日志分页数据
     */
    public Page<OperationLog> getLogsByUserId(Long userId, Pageable pageable) {
        return operationLogRepository.findByUserId(userId, pageable);
    }

    /**
     * 根据模块查询操作日志
     *
     * @param module   模块名称
     * @param pageable 分页参数
     * @return 操作日志分页数据
     */
    public Page<OperationLog> getLogsByModule(String module, Pageable pageable) {
        return operationLogRepository.findByModule(module, pageable);
    }

    /**
     * 根据操作类型查询操作日志
     *
     * @param operationType 操作类型
     * @param pageable      分页参数
     * @return 操作日志分页数据
     */
    public Page<OperationLog> getLogsByOperationType(String operationType, Pageable pageable) {
        return operationLogRepository.findByOperationType(operationType, pageable);
    }

    /**
     * 根据状态查询操作日志
     *
     * @param status   状态
     * @param pageable 分页参数
     * @return 操作日志分页数据
     */
    public Page<OperationLog> getLogsByStatus(String status, Pageable pageable) {
        return operationLogRepository.findByStatus(status, pageable);
    }

    /**
     * 根据IP地址查询操作日志
     *
     * @param ip       IP地址
     * @param pageable 分页参数
     * @return 操作日志分页数据
     */
    public Page<OperationLog> getLogsByIp(String ip, Pageable pageable) {
        return operationLogRepository.findByIp(ip, pageable);
    }

    /**
     * 根据时间范围查询操作日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 操作日志分页数据
     */
    public Page<OperationLog> getLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return operationLogRepository.findByCreateTimeBetween(startTime, endTime, pageable);
    }

    /**
     * 获取最近的操作日志
     *
     * @return 最近10条操作日志
     */
    public List<OperationLog> getRecentLogs() {
        return operationLogRepository.findTop10ByOrderByCreateTimeDesc();
    }

    /**
     * 获取操作日志统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getLogStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        // 总记录数
        long totalCount = operationLogRepository.count();
        statistics.put("totalCount", totalCount);

        // 失败操作次数
        long failedCount = operationLogRepository.countFailedOperations();
        statistics.put("failedCount", failedCount);

        // 成功率
        double successRate = totalCount > 0 ? (double) (totalCount - failedCount) / totalCount * 100 : 100.0;
        statistics.put("successRate", String.format("%.2f%%", successRate));

        // 各模块操作次数统计
        Map<String, Long> moduleStats = new HashMap<>();
        String[] modules = {"用户管理", "充电站管理", "充电桩管理", "订单管理", "运营商管理", "故障管理", "维护管理", "V2G管理", "积分管理", "系统管理"};
        for (String module : modules) {
            Long count = operationLogRepository.countByModule(module);
            if (count > 0) {
                moduleStats.put(module, count);
            }
        }
        statistics.put("moduleStats", moduleStats);

        // 最近操作
        List<OperationLog> recentLogs = getRecentLogs();
        statistics.put("recentLogs", recentLogs);

        return statistics;
    }

    /**
     * 获取指定用户的操作统计
     *
     * @param userId 用户ID
     * @return 统计信息
     */
    public Map<String, Object> getUserLogStatistics(Long userId) {
        Map<String, Object> statistics = new HashMap<>();

        // 用户总操作次数
        long totalCount = operationLogRepository.countByUserId(userId);
        statistics.put("totalCount", totalCount);

        // 用户最近操作
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10,
                org.springframework.data.domain.Sort.by("createTime").descending());
        Page<OperationLog> recentLogs = operationLogRepository.findByUserId(userId, pageable);
        statistics.put("recentLogs", recentLogs.getContent());

        return statistics;
    }

    /**
     * 删除指定时间之前的日志（日志归档）
     *
     * @param beforeTime 时间点
     * @return 删除的记录数
     */
    @Transactional
    public long archiveLogs(LocalDateTime beforeTime) {
        try {
            // 先统计要删除的记录数
            long count = operationLogRepository.findByCreateTimeBetween(
                    LocalDateTime.of(2000, 1, 1, 0, 0),
                    beforeTime,
                    Pageable.unpaged()
            ).getTotalElements();

            // 执行删除
            operationLogRepository.deleteByCreateTimeBefore(beforeTime);

            log.info("成功归档操作日志，删除了 {} 条记录（{}之前）", count, beforeTime);
            return count;
        } catch (Exception e) {
            log.error("归档操作日志失败", e);
            throw new RuntimeException("归档操作日志失败: " + e.getMessage());
        }
    }

    /**
     * 手动保存操作日志（通常由AOP自动调用，此方法用于特殊场景）
     *
     * @param operationLog 操作日志
     * @return 保存后的操作日志
     */
    @Transactional
    public OperationLog saveLog(OperationLog operationLog) {
        return operationLogRepository.save(operationLog);
    }
}
