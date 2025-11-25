package com.ev.charging.repository;

import com.ev.charging.entity.EnergyData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnergyDataRepository extends JpaRepository<EnergyData, Long> {

    // 查询时间范围内的能源数据
    @Query("SELECT e FROM EnergyData e WHERE e.stationId = :stationId AND e.recordTime BETWEEN :startTime AND :endTime ORDER BY e.recordTime ASC")
    List<EnergyData> findByStationIdAndTimeRange(
        @Param("stationId") Long stationId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    // 查询最新的能源数据
    Optional<EnergyData> findTopByStationIdOrderByRecordTimeDesc(Long stationId);

    // 查询所有站点在时间范围内的能源数据（stationId 为 null 时使用）
    @Query("SELECT e FROM EnergyData e WHERE e.recordTime BETWEEN :startTime AND :endTime ORDER BY e.recordTime ASC")
    List<EnergyData> findByRecordTimeBetween(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}
