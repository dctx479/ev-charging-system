package com.ev.charging.service;

import com.ev.charging.common.ResultCode;
import com.ev.charging.entity.ChargingPile;
import com.ev.charging.repository.ChargingPileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 充电桩服务层
 */
@Service
@RequiredArgsConstructor
public class ChargingPileService {

    private final ChargingPileRepository pileRepository;
    private final ChargingStationService stationService;

    /**
     * 获取所有充电桩
     *
     * @return 充电桩列表
     */
    public List<ChargingPile> getAllPiles() {
        return pileRepository.findAll();
    }

    /**
     * 根据ID获取充电桩
     *
     * @param id 充电桩ID
     * @return 充电桩信息
     */
    public ChargingPile getPileById(Long id) {
        return pileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(ResultCode.PILE_NOT_FOUND.getMessage()));
    }

    /**
     * 根据编号获取充电桩
     *
     * @param pileCode 充电桩编号
     * @return 充电桩信息
     */
    public ChargingPile getPileByCode(String pileCode) {
        return pileRepository.findByPileCode(pileCode)
                .orElseThrow(() -> new IllegalArgumentException(ResultCode.PILE_NOT_FOUND.getMessage()));
    }

    /**
     * 根据充电站ID获取充电桩列表
     *
     * @param stationId 充电站ID
     * @return 充电桩列表
     */
    public List<ChargingPile> getPilesByStationId(Long stationId) {
        return pileRepository.findByStationId(stationId);
    }

    /**
     * 获取充电站的可用充电桩
     *
     * @param stationId 充电站ID
     * @return 可用充电桩列表
     */
    public List<ChargingPile> getAvailablePiles(Long stationId) {
        return pileRepository.findByStationIdAndStatus(stationId, "AVAILABLE");
    }

    /**
     * 获取所有故障充电桩
     *
     * @return 故障充电桩列表
     */
    public List<ChargingPile> getFaultPiles() {
        return pileRepository.findFaultPiles();
    }

    /**
     * 创建充电桩
     *
     * @param pile 充电桩信息
     * @return 创建后的充电桩
     */
    @Transactional
    public ChargingPile createPile(ChargingPile pile) {
        // 检查编号是否已存在
        if (pileRepository.existsByPileCode(pile.getPileCode())) {
            throw new IllegalArgumentException("充电桩编号已存在");
        }

        // 验证充电站是否存在
        stationService.getStationById(pile.getStationId());

        // 设置默认值
        if (pile.getStatus() == null) {
            pile.setStatus("AVAILABLE");
        }
        if (pile.getCurrentPower() == null) {
            pile.setCurrentPower(0.0);
        }
        if (pile.getTotalChargingTimes() == null) {
            pile.setTotalChargingTimes(0);
        }
        if (pile.getTotalChargingEnergy() == null) {
            pile.setTotalChargingEnergy(0.0);
        }

        ChargingPile savedPile = pileRepository.save(pile);

        // 更新充电站的充电桩统计
        stationService.updateStationPileCount(pile.getStationId());

        return savedPile;
    }

    /**
     * 更新充电桩信息
     *
     * @param pile 充电桩信息
     * @return 更新后的充电桩
     */
    @Transactional
    public ChargingPile updatePile(ChargingPile pile) {
        ChargingPile existing = getPileById(pile.getId());

        // 保存原始值，用于后续比较
        Long oldStationId = existing.getStationId();
        String oldStatus = existing.getStatus();

        // 更新字段
        BeanUtils.copyProperties(pile, existing, "id", "pileCode", "createTime", "updateTime");

        ChargingPile savedPile = pileRepository.save(existing);

        // 如果充电站ID或状态发生变化，更新充电站统计
        if (!oldStationId.equals(existing.getStationId()) ||
                !oldStatus.equals(existing.getStatus())) {
            // 更新原充电站统计
            stationService.updateStationPileCount(oldStationId);
            // 如果充电站ID变化，还需要更新新充电站统计
            if (!oldStationId.equals(existing.getStationId())) {
                stationService.updateStationPileCount(existing.getStationId());
            }
        }

        return savedPile;
    }

    /**
     * 更新充电桩状态
     *
     * @param id     充电桩ID
     * @param status 新状态
     * @return 更新后的充电桩
     */
    @Transactional
    public ChargingPile updatePileStatus(Long id, String status) {
        ChargingPile pile = getPileById(id);
        pile.setStatus(status);
        ChargingPile savedPile = pileRepository.save(pile);

        // 更新充电站统计
        stationService.updateStationPileCount(pile.getStationId());

        return savedPile;
    }

    /**
     * 更新充电桩实时数据
     *
     * @param id          充电桩ID
     * @param currentPower 当前功率
     * @param voltage     电压
     * @param current     电流
     * @param temperature 温度
     * @return 更新后的充电桩
     */
    @Transactional
    public ChargingPile updatePileRealTimeData(Long id, Double currentPower,
                                                 Double voltage, Double current, Double temperature) {
        ChargingPile pile = getPileById(id);
        pile.setCurrentPower(currentPower);
        pile.setVoltage(voltage);
        pile.setCurrent(current);
        pile.setTemperature(temperature);

        return pileRepository.save(pile);
    }

    /**
     * 记录充电桩维护
     *
     * @param id 充电桩ID
     * @return 更新后的充电桩
     */
    @Transactional
    public ChargingPile recordMaintenance(Long id) {
        ChargingPile pile = getPileById(id);
        pile.setLastMaintenanceTime(LocalDateTime.now());
        pile.setStatus("MAINTENANCE");

        ChargingPile savedPile = pileRepository.save(pile);

        // 更新充电站统计
        stationService.updateStationPileCount(pile.getStationId());

        return savedPile;
    }

    /**
     * 删除充电桩
     *
     * @param id 充电桩ID
     */
    @Transactional
    public void deletePile(Long id) {
        ChargingPile pile = getPileById(id);
        Long stationId = pile.getStationId();

        pileRepository.deleteById(id);

        // 更新充电站统计
        stationService.updateStationPileCount(stationId);
    }
}
