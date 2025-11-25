package com.ev.charging.repository;

import com.ev.charging.entity.V2GRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface V2GRecordRepository extends JpaRepository<V2GRecord, Long> {

    // 查询用户的V2G记录
    List<V2GRecord> findByUserIdOrderByCreateTimeDesc(Long userId);

    // 查询全平台V2G记录（管理端）
    List<V2GRecord> findAllByOrderByCreateTimeDesc();

    // 统计用户总放电收益
    @Query("SELECT COALESCE(SUM(r.profit), 0) FROM V2GRecord r WHERE r.userId = :userId AND r.recordType = 2")
    BigDecimal sumProfitByUser(@Param("userId") Long userId);

    // 统计用户总放电量
    @Query("SELECT COALESCE(SUM(r.energyAmount), 0) FROM V2GRecord r WHERE r.userId = :userId AND r.recordType = 2")
    BigDecimal sumDischargeAmountByUser(@Param("userId") Long userId);

    // 全平台：统计指定时间段内的总放电量
    @Query("SELECT COALESCE(SUM(r.energyAmount), 0) FROM V2GRecord r WHERE r.recordType = 2 AND r.endTime >= :startTime")
    BigDecimal sumDischargeAmountSince(@Param("startTime") LocalDateTime startTime);

    // 全平台：统计指定时间段内的总收益
    @Query("SELECT COALESCE(SUM(r.profit), 0) FROM V2GRecord r WHERE r.recordType = 2 AND r.endTime >= :startTime")
    BigDecimal sumProfitSince(@Param("startTime") LocalDateTime startTime);

    // 全平台：统计已完成的放电记录总数
    @Query("SELECT COUNT(r) FROM V2GRecord r WHERE r.recordType = 2 AND r.endTime IS NOT NULL")
    Long countCompletedDischarge();

    // 全平台：统计参与过V2G放电的用户数
    @Query("SELECT COUNT(DISTINCT r.userId) FROM V2GRecord r WHERE r.recordType = 2")
    Long countDistinctUsers();

    /**
     * 原子操作：停止放电
     * 确保只有未完成的放电记录 (endTime IS NULL) 才能被更新为已完成
     * 防止double-stop race condition
     *
     * @return 更新的记录数 (0 或 1)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE V2GRecord r SET r.endTime = :endTime WHERE r.id = :recordId AND r.endTime IS NULL")
    int atomicStopDischarge(@Param("recordId") Long recordId, @Param("endTime") LocalDateTime endTime);
}
