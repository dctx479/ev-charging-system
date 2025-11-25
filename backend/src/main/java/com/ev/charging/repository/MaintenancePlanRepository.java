package com.ev.charging.repository;

import com.ev.charging.entity.MaintenancePlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 维护计划数据访问层
 */
@Repository
public interface MaintenancePlanRepository extends JpaRepository<MaintenancePlan, Long> {

    /**
     * 按充电桩ID查询维护计划
     */
    List<MaintenancePlan> findByPileIdOrderByPlannedTimeDesc(Long pileId);

    /**
     * 按站点ID查询维护计划
     */
    Page<MaintenancePlan> findByStationId(Long stationId, Pageable pageable);

    /**
     * 按状态查询
     */
    Page<MaintenancePlan> findByMaintenanceStatus(Byte maintenanceStatus, Pageable pageable);

    /**
     * 按站点和状态查询
     */
    Page<MaintenancePlan> findByStationIdAndMaintenanceStatus(Long stationId, Byte maintenanceStatus, Pageable pageable);

    /**
     * 按计划类型查询
     */
    Page<MaintenancePlan> findByPlanType(Byte planType, Pageable pageable);

    /**
     * 查询指定时间范围内的维护计划
     */
    @Query("SELECT m FROM MaintenancePlan m WHERE m.plannedTime BETWEEN :startTime AND :endTime")
    List<MaintenancePlan> findByPlannedTimeBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 查询待执行的维护计划
     */
    @Query("SELECT m FROM MaintenancePlan m WHERE m.maintenanceStatus = 0 ORDER BY m.plannedTime ASC")
    List<MaintenancePlan> findPendingPlans();

    /**
     * 查询即将到期的维护计划（未来24小时内）
     */
    @Query("SELECT m FROM MaintenancePlan m WHERE m.maintenanceStatus = 0 AND m.plannedTime BETWEEN :now AND :tomorrow")
    List<MaintenancePlan> findUpcomingPlans(
            @Param("now") LocalDateTime now,
            @Param("tomorrow") LocalDateTime tomorrow
    );

    /**
     * 统计各状态维护计划数量
     */
    @Query("SELECT m.maintenanceStatus, COUNT(m) FROM MaintenancePlan m GROUP BY m.maintenanceStatus")
    List<Object[]> countByMaintenanceStatus();

    /**
     * 按多条件查询
     */
    @Query("SELECT m FROM MaintenancePlan m WHERE " +
            "(:stationId IS NULL OR m.stationId = :stationId) AND " +
            "(:maintenanceStatus IS NULL OR m.maintenanceStatus = :maintenanceStatus) AND " +
            "(:planType IS NULL OR m.planType = :planType)")
    Page<MaintenancePlan> findByConditions(
            @Param("stationId") Long stationId,
            @Param("maintenanceStatus") Byte maintenanceStatus,
            @Param("planType") Byte planType,
            Pageable pageable
    );
}
