package com.ev.charging.repository;

import com.ev.charging.entity.FaultRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 故障记录数据访问层
 */
@Repository
public interface FaultRecordRepository extends JpaRepository<FaultRecord, Long> {

    /**
     * 按站点ID查询故障列表
     */
    Page<FaultRecord> findByStationId(Long stationId, Pageable pageable);

    /**
     * 按充电桩ID查询故障列表
     */
    List<FaultRecord> findByPileIdOrderByCreateTimeDesc(Long pileId);

    /**
     * 按维修状态查询
     */
    Page<FaultRecord> findByRepairStatus(Byte repairStatus, Pageable pageable);

    /**
     * 按站点和维修状态查询
     */
    Page<FaultRecord> findByStationIdAndRepairStatus(Long stationId, Byte repairStatus, Pageable pageable);

    /**
     * 按严重程度查询
     */
    Page<FaultRecord> findBySeverity(Byte severity, Pageable pageable);

    /**
     * 统计各状态故障数量
     */
    @Query("SELECT f.repairStatus, COUNT(f) FROM FaultRecord f GROUP BY f.repairStatus")
    List<Object[]> countByRepairStatus();

    /**
     * 统计站点故障数量
     */
    @Query("SELECT COUNT(f) FROM FaultRecord f WHERE f.stationId = :stationId AND f.repairStatus = :repairStatus")
    Long countByStationIdAndRepairStatus(@Param("stationId") Long stationId, @Param("repairStatus") Byte repairStatus);

    /**
     * 统计时间段内的故障数量
     */
    @Query("SELECT COUNT(f) FROM FaultRecord f WHERE f.faultTime BETWEEN :startTime AND :endTime")
    Long countByFaultTimeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 按故障类型统计
     */
    @Query("SELECT f.faultType, COUNT(f) FROM FaultRecord f GROUP BY f.faultType")
    List<Object[]> countByFaultType();

    /**
     * 查询待维修的紧急故障
     */
    @Query("SELECT f FROM FaultRecord f WHERE f.repairStatus = 0 AND f.severity = 4 ORDER BY f.faultTime ASC")
    List<FaultRecord> findUrgentPendingFaults();

    /**
     * 按多条件查询（动态查询）
     */
    @Query("SELECT f FROM FaultRecord f WHERE " +
            "(:stationId IS NULL OR f.stationId = :stationId) AND " +
            "(:repairStatus IS NULL OR f.repairStatus = :repairStatus) AND " +
            "(:severity IS NULL OR f.severity = :severity) AND " +
            "(:faultType IS NULL OR f.faultType = :faultType)")
    Page<FaultRecord> findByConditions(
            @Param("stationId") Long stationId,
            @Param("repairStatus") Byte repairStatus,
            @Param("severity") Byte severity,
            @Param("faultType") Byte faultType,
            Pageable pageable
    );
}
