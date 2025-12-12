package com.ev.charging.repository;

import com.ev.charging.entity.NearbyService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 周边服务Repository
 */
@Repository
public interface NearbyServiceRepository extends JpaRepository<NearbyService, Long> {

    /**
     * 根据充电站ID和服务类型查询周边服务
     *
     * @param stationId   充电站ID
     * @param serviceType 服务类型
     * @return 周边服务列表
     */
    List<NearbyService> findByStationIdAndServiceTypeAndStatusOrderByRatingDescDistanceAsc(
            Long stationId, Byte serviceType, Byte status);

    /**
     * 根据充电站ID查询所有周边服务
     *
     * @param stationId 充电站ID
     * @param status    状态
     * @return 周边服务列表
     */
    List<NearbyService> findByStationIdAndStatusOrderByRatingDescDistanceAsc(
            Long stationId, Byte status);

    /**
     * 根据等待时间推荐周边服务
     * 推荐逻辑：平均消费时间 ≤ 等待时间 × 1.2（留20%缓冲）
     *
     * @param stationId 充电站ID
     * @param waitTime  等待时间（分钟）
     * @param status    状态
     * @return 推荐服务列表
     */
    @Query("SELECT ns FROM NearbyService ns " +
           "WHERE ns.stationId = :stationId " +
           "AND ns.status = :status " +
           "AND ns.avgConsumeTime <= :maxTime " +
           "AND ns.avgConsumeTime >= :minTime " +
           "ORDER BY ns.rating DESC, ns.distance ASC")
    List<NearbyService> findRecommendedServices(
            @Param("stationId") Long stationId,
            @Param("minTime") Integer minTime,
            @Param("maxTime") Integer maxTime,
            @Param("status") Byte status);

    /**
     * 查询指定距离范围内的周边服务
     *
     * @param stationId   充电站ID
     * @param maxDistance 最大距离（米）
     * @param status      状态
     * @return 周边服务列表
     */
    @Query("SELECT ns FROM NearbyService ns " +
           "WHERE ns.stationId = :stationId " +
           "AND ns.status = :status " +
           "AND ns.distance <= :maxDistance " +
           "ORDER BY ns.rating DESC, ns.distance ASC")
    List<NearbyService> findByDistance(
            @Param("stationId") Long stationId,
            @Param("maxDistance") Integer maxDistance,
            @Param("status") Byte status);
}
