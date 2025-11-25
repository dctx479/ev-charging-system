package com.ev.charging.repository;

import com.ev.charging.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志Repository
 */
@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    /**
     * 根据用户ID分页查询操作日志
     */
    Page<OperationLog> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据模块查询操作日志
     */
    Page<OperationLog> findByModule(String module, Pageable pageable);

    /**
     * 根据操作类型查询操作日志
     */
    Page<OperationLog> findByOperationType(String operationType, Pageable pageable);

    /**
     * 根据状态查询操作日志
     */
    Page<OperationLog> findByStatus(String status, Pageable pageable);

    /**
     * 根据IP地址查询操作日志
     */
    Page<OperationLog> findByIp(String ip, Pageable pageable);

    /**
     * 根据时间范围查询操作日志
     */
    Page<OperationLog> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 综合查询：根据多个条件查询操作日志
     */
    @Query("SELECT o FROM OperationLog o WHERE " +
           "(:userId IS NULL OR o.userId = :userId) AND " +
           "(:username IS NULL OR o.username LIKE CONCAT('%', :username, '%')) AND " +
           "(:module IS NULL OR o.module = :module) AND " +
           "(:operationType IS NULL OR o.operationType = :operationType) AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:ip IS NULL OR o.ip = :ip) AND " +
           "(:startTime IS NULL OR o.createTime >= :startTime) AND " +
           "(:endTime IS NULL OR o.createTime <= :endTime)")
    Page<OperationLog> findByConditions(
            @Param("userId") Long userId,
            @Param("username") String username,
            @Param("module") String module,
            @Param("operationType") String operationType,
            @Param("status") String status,
            @Param("ip") String ip,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * 统计用户操作次数
     */
    @Query("SELECT COUNT(o) FROM OperationLog o WHERE o.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);

    /**
     * 统计模块操作次数
     */
    @Query("SELECT COUNT(o) FROM OperationLog o WHERE o.module = :module")
    Long countByModule(@Param("module") String module);

    /**
     * 统计失败操作次数
     */
    @Query("SELECT COUNT(o) FROM OperationLog o WHERE o.status = 'FAILURE'")
    Long countFailedOperations();

    /**
     * 获取最近的操作日志
     */
    List<OperationLog> findTop10ByOrderByCreateTimeDesc();

    /**
     * 删除指定时间之前的日志（用于日志归档）
     */
    void deleteByCreateTimeBefore(LocalDateTime time);
}
