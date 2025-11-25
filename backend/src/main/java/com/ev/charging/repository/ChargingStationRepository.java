package com.ev.charging.repository;

import com.ev.charging.entity.ChargingStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 充电站数据访问层
 */
@Repository
public interface ChargingStationRepository extends JpaRepository<ChargingStation, Long> {

    /**
     * 根据状态查询充电站
     *
     * @param status 状态
     * @return 充电站列表
     */
    List<ChargingStation> findByStatus(String status);

    /**
     * 根据名称模糊查询充电站
     *
     * @param name 名称关键字
     * @return 充电站列表
     */
    List<ChargingStation> findByNameContaining(String name);

    /**
     * 查询所有营业中的充电站
     *
     * @return 充电站列表
     */
    @Query("SELECT s FROM ChargingStation s WHERE s.status = 'ACTIVE' ORDER BY s.rating DESC")
    List<ChargingStation> findActiveStationsOrderByRating();

    /**
     * 根据经纬度范围查询附近的充电站
     * 简单的矩形范围查询（实际项目中应使用空间索引）
     *
     * @param minLat 最小纬度
     * @param maxLat 最大纬度
     * @param minLon 最小经度
     * @param maxLon 最大经度
     * @return 充电站列表
     */
    @Query("SELECT s FROM ChargingStation s WHERE s.latitude BETWEEN :minLat AND :maxLat " +
            "AND s.longitude BETWEEN :minLon AND :maxLon AND s.status = 'ACTIVE'")
    List<ChargingStation> findNearbyStations(Double minLat, Double maxLat, Double minLon, Double maxLon);
}
