package com.ev.charging.repository;

import com.ev.charging.entity.ChargingStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    List<ChargingStation> findByStatus(Byte status);

    /**
     * 根据名称模糊查询充电站（接收完整LIKE pattern，%已由调用方添加和转义）
     *
     * @param pattern LIKE pattern，如 %keyword%
     * @return 充电站列表
     */
    List<ChargingStation> findByNameLike(String pattern);

    /**
     * 查询所有营业中的充电站
     *
     * @return 充电站列表
     */
    @Query("SELECT s FROM ChargingStation s WHERE s.status = 1")
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
            "AND s.longitude BETWEEN :minLon AND :maxLon AND s.status = 1")
    List<ChargingStation> findNearbyStations(@Param("minLat") double minLat,
                                             @Param("maxLat") double maxLat,
                                             @Param("minLon") double minLon,
                                             @Param("maxLon") double maxLon);
}
