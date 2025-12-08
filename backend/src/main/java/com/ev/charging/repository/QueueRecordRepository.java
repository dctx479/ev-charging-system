package com.ev.charging.repository;

import com.ev.charging.entity.QueueRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 排队记录数据访问层
 */
@Repository
public interface QueueRecordRepository extends JpaRepository<QueueRecord, Long> {

    /**
     * 查询用户当前正在排队的记录
     *
     * @param userId 用户ID
     * @param status 状态（0-排队中 1-已叫号）
     * @return 排队记录
     */
    Optional<QueueRecord> findByUserIdAndStatusIn(Long userId, List<Byte> status);

    /**
     * 查询站点当前排队列表（按加入时间排序）
     *
     * @param stationId 充电站ID
     * @param status    状态
     * @return 排队列表
     */
    List<QueueRecord> findByStationIdAndStatusOrderByJoinTimeAsc(Long stationId, Byte status);

    /**
     * 统计站点当前排队人数
     *
     * @param stationId 充电站ID
     * @param status    状态
     * @return 排队人数
     */
    long countByStationIdAndStatus(Long stationId, Byte status);

    /**
     * 查询指定站点和状态的排队记录
     *
     * @param stationId 充电站ID
     * @param status    状态列表
     * @return 排队记录列表
     */
    List<QueueRecord> findByStationIdAndStatusInOrderByJoinTimeAsc(Long stationId, List<Byte> status);

    /**
     * 查询已过期的叫号记录（超过过期时间且状态为已叫号）
     *
     * @param now    当前时间
     * @param status 状态（1-已叫号）
     * @return 过期记录列表
     */
    @Query("SELECT q FROM QueueRecord q WHERE q.status = :status AND q.expireTime < :now")
    List<QueueRecord> findExpiredRecords(@Param("now") LocalDateTime now, @Param("status") Byte status);

    /**
     * 查询用户在指定站点的活跃排队记录
     *
     * @param userId    用户ID
     * @param stationId 充电站ID
     * @param statusList 状态列表
     * @return 排队记录
     */
    Optional<QueueRecord> findByUserIdAndStationIdAndStatusIn(Long userId, Long stationId, List<Byte> statusList);

    /**
     * 根据排队号查询
     *
     * @param queueNo 排队号
     * @return 排队记录
     */
    Optional<QueueRecord> findByQueueNo(String queueNo);

    /**
     * 查询站点当天的排队记录数（用于生成排队号）
     *
     * @param stationId 充电站ID
     * @param startTime 开始时间
     * @return 记录数
     */
    @Query("SELECT COUNT(q) FROM QueueRecord q WHERE q.stationId = :stationId AND q.joinTime >= :startTime")
    long countTodayQueue(@Param("stationId") Long stationId, @Param("startTime") LocalDateTime startTime);
}
