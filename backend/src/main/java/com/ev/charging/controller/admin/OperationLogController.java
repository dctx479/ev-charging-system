package com.ev.charging.controller.admin;

import com.ev.charging.annotation.OperationLog;
import com.ev.charging.common.Result;
import com.ev.charging.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 管理后台 - 操作日志控制器
 */
@Tag(name = "管理后台 - 操作日志管理", description = "操作日志查询、统计、归档等功能")
@RestController
@RequestMapping("/admin/operation-logs")
@RequiredArgsConstructor
@Slf4j
public class OperationLogController {

    private final OperationLogService operationLogService;

    /**
     * 分页查询操作日志
     *
     * @param userId        用户ID
     * @param username      用户名（模糊查询）
     * @param module        操作模块
     * @param operationType 操作类型
     * @param status        操作状态
     * @param ip            IP地址
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @param page          页码（从0开始）
     * @param size          每页数量
     * @return 操作日志分页数据
     */
    @Operation(summary = "分页查询操作日志", description = "支持多条件组合查询操作日志")
    @GetMapping
    public Result<Page<com.ev.charging.entity.OperationLog>> queryLogs(
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "用户名（模糊查询）") @RequestParam(required = false) String username,
            @Parameter(description = "操作模块") @RequestParam(required = false) String module,
            @Parameter(description = "操作类型") @RequestParam(required = false) String operationType,
            @Parameter(description = "操作状态") @RequestParam(required = false) String status,
            @Parameter(description = "IP地址") @RequestParam(required = false) String ip,
            @Parameter(description = "开始时间")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {

        log.info("查询操作日志: userId={}, username={}, module={}, operationType={}, status={}, ip={}, startTime={}, endTime={}, page={}, size={}",
                userId, username, module, operationType, status, ip, startTime, endTime, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createTime").descending());
        Page<com.ev.charging.entity.OperationLog> logs = operationLogService.queryLogs(
                userId, username, module, operationType, status, ip, startTime, endTime, pageable
        );
        return Result.success(logs);
    }

    /**
     * 根据ID查询操作日志详情
     *
     * @param id 日志ID
     * @return 操作日志详情
     */
    @Operation(summary = "查询操作日志详情", description = "根据ID查询操作日志的详细信息")
    @GetMapping("/{id}")
    public Result<com.ev.charging.entity.OperationLog> getLogById(@PathVariable Long id) {
        log.info("查询操作日志详情: id={}", id);

        com.ev.charging.entity.OperationLog logEntity = operationLogService.getLogById(id);
        return Result.success(logEntity);
    }

    /**
     * 获取操作日志统计信息
     *
     * @return 统计信息
     */
    @Operation(summary = "获取操作日志统计", description = "获取操作日志的统计信息（总数、成功率、各模块统计等）")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getLogStatistics() {
        log.info("获取操作日志统计信息");

        Map<String, Object> statistics = operationLogService.getLogStatistics();
        return Result.success(statistics);
    }

    /**
     * 获取指定用户的操作统计
     *
     * @param userId 用户ID
     * @return 用户操作统计
     */
    @Operation(summary = "获取用户操作统计", description = "获取指定用户的操作统计信息")
    @GetMapping("/user/{userId}/statistics")
    public Result<Map<String, Object>> getUserLogStatistics(@PathVariable Long userId) {
        log.info("获取用户操作统计: userId={}", userId);

        Map<String, Object> statistics = operationLogService.getUserLogStatistics(userId);
        return Result.success(statistics);
    }

    /**
     * 归档操作日志（删除指定时间之前的日志）
     *
     * @param beforeDays 保留最近多少天的日志（默认90天）
     * @return 归档结果
     */
    @Operation(summary = "归档操作日志", description = "删除指定时间之前的操作日志，用于日志归档")
    @OperationLog(module = "操作日志管理", operation = "DELETE", description = "归档操作日志")
    @PostMapping("/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> archiveLogs(
            @Parameter(description = "保留最近多少天的日志") @RequestParam(defaultValue = "90") int beforeDays) {

        log.info("归档操作日志: beforeDays={}", beforeDays);

        LocalDateTime beforeTime = LocalDateTime.now().minusDays(beforeDays);
        long count = operationLogService.archiveLogs(beforeTime);

        Map<String, Object> result = Map.of(
                "archivedCount", count,
                "beforeTime", beforeTime,
                "message", String.format("成功归档 %d 条操作日志", count)
        );

        return Result.success(result);
    }

    /**
     * 导出操作日志（可选功能）
     *
     * @param userId        用户ID
     * @param module        操作模块
     * @param operationType 操作类型
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @return 导出结果
     */
    @Operation(summary = "导出操作日志", description = "导出操作日志为Excel文件（待实现）")
    @OperationLog(module = "操作日志管理", operation = "EXPORT", description = "导出操作日志")
    @GetMapping("/export")
    public Result<String> exportLogs(
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "操作模块") @RequestParam(required = false) String module,
            @Parameter(description = "操作类型") @RequestParam(required = false) String operationType,
            @Parameter(description = "开始时间")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("导出操作日志: userId={}, module={}, operationType={}, startTime={}, endTime={}",
                userId, module, operationType, startTime, endTime);

        return Result.error(501, "导出功能暂未实现");
    }
}
