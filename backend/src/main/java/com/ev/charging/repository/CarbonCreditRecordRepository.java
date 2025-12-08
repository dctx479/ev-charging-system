package com.ev.charging.repository;

import com.ev.charging.entity.CarbonCreditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 碳积分记录Repository
 */
@Repository
public interface CarbonCreditRecordRepository extends JpaRepository<CarbonCreditRecord, Long> {

    /**
     * 查询用户的积分记录（按时间倒序）
     *
     * @param userId 用户ID
     * @return 积分记录列表
     */
    List<CarbonCreditRecord> findByUserIdOrderByCreateTimeDesc(Long userId);

    /**
     * 查询用户指定类型的积分记录
     *
     * @param userId     用户ID
     * @param creditType 积分类型
     * @return 积分记录列表
     */
    List<CarbonCreditRecord> findByUserIdAndCreditTypeOrderByCreateTimeDesc(Long userId, Byte creditType);

    /**
     * 查询用户在指定时间段内的积分记录
     *
     * @param userId    用户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 积分记录列表
     */
    List<CarbonCreditRecord> findByUserIdAndCreateTimeBetweenOrderByCreateTimeDesc(
            Long userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计用户总获得积分（creditChange > 0）
     *
     * @param userId 用户ID
     * @return 总获得积分
     */
    @Query("SELECT COALESCE(SUM(c.creditChange), 0) FROM CarbonCreditRecord c WHERE c.userId = :userId AND c.creditChange > 0")
    Integer sumTotalEarnedCredits(@Param("userId") Long userId);

    /**
     * 统计用户总消耗积分（creditChange < 0）
     *
     * @param userId 用户ID
     * @return 总消耗积分（绝对值）
     */
    @Query("SELECT COALESCE(SUM(ABS(c.creditChange)), 0) FROM CarbonCreditRecord c WHERE c.userId = :userId AND c.creditChange < 0")
    Integer sumTotalSpentCredits(@Param("userId") Long userId);

    /**
     * 统计用户在指定时间段内获得的积分
     *
     * @param userId    用户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 获得积分总数
     */
    @Query("SELECT COALESCE(SUM(c.creditChange), 0) FROM CarbonCreditRecord c WHERE c.userId = :userId AND c.creditChange > 0 AND c.createTime BETWEEN :startTime AND :endTime")
    Integer sumEarnedCreditsByPeriod(@Param("userId") Long userId,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 统计用户在指定时间段内消耗的积分
     *
     * @param userId    用户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 消耗积分总数（绝对值）
     */
    @Query("SELECT COALESCE(SUM(ABS(c.creditChange)), 0) FROM CarbonCreditRecord c WHERE c.userId = :userId AND c.creditChange < 0 AND c.createTime BETWEEN :startTime AND :endTime")
    Integer sumSpentCreditsByPeriod(@Param("userId") Long userId,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 查询用户今天是否已签到
     *
     * @param userId      用户ID
     * @param creditType  积分类型（2-签到）
     * @param startOfDay  今天开始时间
     * @param endOfDay    今天结束时间
     * @return 是否已签到
     */
    @Query("SELECT COUNT(c) > 0 FROM CarbonCreditRecord c WHERE c.userId = :userId AND c.creditType = :creditType AND c.createTime BETWEEN :startOfDay AND :endOfDay")
    boolean existsByUserIdAndCreditTypeAndCreateTimeBetween(
            @Param("userId") Long userId,
            @Param("creditType") Byte creditType,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * 查询用户连续签到天数
     *
     * @param userId     用户ID
     * @param creditType 积分类型（2-签到）
     * @return 签到记录列表
     */
    @Query("SELECT c FROM CarbonCreditRecord c WHERE c.userId = :userId AND c.creditType = :creditType ORDER BY c.createTime DESC")
    List<CarbonCreditRecord> findCheckInRecords(@Param("userId") Long userId, @Param("creditType") Byte creditType);
}
