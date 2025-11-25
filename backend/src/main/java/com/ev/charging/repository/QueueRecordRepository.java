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
     * @param userId    用户ID
     * @param queueStatus 排队状态（1-排队中 2-已分配）
     * @return 排队记录
     */
    Optional<QueueRecord> findByUserIdAndQueueStatusIn(Long userId, List<Byte> queueStatus);

    /**
     * 查询站点当前排队列表（按加入时间排序，同时间按ID排序保证FIFO确定性）
     *
     * @param stationId 充电站ID
     * @param queueStatus    排队状态
     * @return 排队列表
     */
    List<QueueRecord> findByStationIdAndQueueStatusOrderByJoinTimeAscIdAsc(Long stationId, Byte queueStatus);

    /**
     * 统计站点当前排队人数
     *
     * @param stationId 充电站ID
     * @param queueStatus    排队状态
     * @return 排队人数
     */
    long countByStationIdAndQueueStatus(Long stationId, Byte queueStatus);

    /**
     * 查询已超时的分配记录（超过分配时间且状态为已分配）
     * 规则：分配超过15分钟未确认充电则视为超时
     *
     * @param now    当前时间（通常是 now() - 15 minutes）
     * @param queueStatus 状态（2-已分配）
     * @return 超时记录列表
     */
    @Query("SELECT q FROM QueueRecord q WHERE q.queueStatus = :queueStatus AND q.assignedTime < :expiryTime")
    List<QueueRecord> findExpiredRecords(@Param("expiryTime") LocalDateTime now, @Param("queueStatus") Byte queueStatus);

    /**
     * 查询用户在指定站点的活跃排队记录
     *
     * @param userId    用户ID
     * @param stationId 充电站ID
     * @param queueStatusList 排队状态列表
     * @return 排队记录
     */
    Optional<QueueRecord> findByUserIdAndStationIdAndQueueStatusIn(Long userId, Long stationId, List<Byte> queueStatusList);

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
