package com.ev.charging.repository;

import com.ev.charging.entity.ChargingPile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 充电桩数据访问层
 */
@Repository
public interface ChargingPileRepository extends JpaRepository<ChargingPile, Long> {

    /**
     * 根据充电桩编号查找
     *
     * @param pileNo 充电桩编号
     * @return 充电桩信息
     */
    Optional<ChargingPile> findByPileNo(String pileNo);

    /**
     * 根据充电站ID查询充电桩列表
     *
     * @param stationId 充电站ID
     * @return 充电桩列表
     */
    List<ChargingPile> findByStationId(Long stationId);

    /**
     * 根据充电站ID和状态查询充电桩
     *
     * @param stationId 充电站ID
     * @param status    状态：1空闲 2充电中 3预约中 4故障 5离线
     * @return 充电桩列表
     */
    List<ChargingPile> findByStationIdAndStatus(Long stationId, Byte status);

    /**
     * 统计充电站的充电桩数量
     *
     * @param stationId 充电站ID
     * @return 充电桩数量
     */
    long countByStationId(Long stationId);

    /**
     * 统计充电站的可用充电桩数量
     *
     * @param stationId 充电站ID
     * @param status    状态
     * @return 可用充电桩数量
     */
    long countByStationIdAndStatus(Long stationId, Byte status);

    /**
     * 查询所有故障充电桩
     *
     * @return 故障充电桩列表
     */
    @Query("SELECT p FROM ChargingPile p WHERE p.status = 4 ORDER BY p.updateTime DESC")
    List<ChargingPile> findFaultPiles();

    /**
     * 检查充电桩编号是否存在
     *
     * @param pileNo 充电桩编号
     * @return true-存在，false-不存在
     */
    boolean existsByPileNo(String pileNo);

    /**
     * 原子性更新充电桩状态（带状态前置条件）
     * 用于防止并发场景下的状态覆盖问题
     * 只有当前状态等于 expectedStatus 时才更新，实现 CAS 语义
     *
     * @param pileId         充电桩ID
     * @param status         新状态
     * @param expectedStatus 期望的当前状态（前置条件）
     * @return 更新行数（0表示桩不存在或当前状态不符合预期，1表示成功）
     */
    @Modifying
    @Transactional
    @Query("UPDATE ChargingPile p SET p.status = :status, p.updateTime = CURRENT_TIMESTAMP WHERE p.id = :pileId AND p.status = :expectedStatus")
    int updatePileStatusAtomically(@Param("pileId") Long pileId, @Param("status") Byte status, @Param("expectedStatus") Byte expectedStatus);
}
