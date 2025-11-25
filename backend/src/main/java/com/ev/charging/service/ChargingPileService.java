package com.ev.charging.service;

import com.ev.charging.common.ResultCode;
import com.ev.charging.entity.ChargingPile;
import com.ev.charging.entity.ChargingStation;
import com.ev.charging.repository.ChargingPileRepository;
import com.ev.charging.repository.ChargingStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 充电桩服务层
 */
@Service
@RequiredArgsConstructor
public class ChargingPileService {

    private final ChargingPileRepository pileRepository;
    private final ChargingStationService stationService;
    private final ChargingStationRepository stationRepository;
    private final CacheService cacheService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取所有充电桩（含所属站点名称）
     *
     * @return 充电桩列表
     */
    public List<ChargingPile> getAllPiles() {
        List<ChargingPile> piles = pileRepository.findAll();
        populateStationNames(piles);
        return piles;
    }

    /**
     * 根据ID获取充电桩（带缓存）
     * 修复：添加负缓存（缓存穿透保护）
     *
     * @param id 充电桩ID
     * @return 充电桩信息
     */
    public ChargingPile getPileById(Long id) {
        // 尝试从缓存获取
        String cacheKey = cacheService.buildPileStatusKey(id);
        Object cached = redisTemplate.opsForValue().get(cacheKey);

        // 如果缓存中是"NULL"（负缓存标记），返回null
        if ("NULL".equals(cached)) {
            return null;
        }

        if (cached != null && cached instanceof ChargingPile) {
            return (ChargingPile) cached;
        }

        // 缓存未命中，从数据库查询
        ChargingPile pile = pileRepository.findById(id).orElse(null);

        // 缓存处理
        if (pile == null) {
            // 对于不存在的充电桩，使用负缓存（60秒TTL）
            redisTemplate.opsForValue().set(cacheKey, "NULL", 60, TimeUnit.SECONDS);
        } else {
            // 正常缓存充电桩信息
            cacheService.set(cacheKey, pile, CacheService.PILE_STATUS_TTL);
        }

        return pile;
    }

    /**
     * 根据编号获取充电桩
     *
     * @param pileNo 充电桩编号
     * @return 充电桩信息
     */
    public ChargingPile getPileByNo(String pileNo) {
        return pileRepository.findByPileNo(pileNo)
                .orElseThrow(() -> new IllegalArgumentException(ResultCode.PILE_NOT_FOUND.getMessage()));
    }

    /**
     * 根据充电站ID获取充电桩列表（含所属站点名称）
     *
     * @param stationId 充电站ID
     * @return 充电桩列表
     */
    public List<ChargingPile> getPilesByStationId(Long stationId) {
        List<ChargingPile> piles = pileRepository.findByStationId(stationId);
        populateStationNames(piles);
        return piles;
    }

    /**
     * 获取充电站的可用充电桩（含所属站点名称）
     *
     * @param stationId 充电站ID
     * @return 可用充电桩列表
     */
    public List<ChargingPile> getAvailablePiles(Long stationId) {
        List<ChargingPile> piles = pileRepository.findByStationIdAndStatus(stationId, (byte) 1); // 1-空闲
        populateStationNames(piles);
        return piles;
    }

    /**
     * 获取所有故障充电桩（含所属站点名称）
     *
     * @return 故障充电桩列表
     */
    public List<ChargingPile> getFaultPiles() {
        List<ChargingPile> piles = pileRepository.findFaultPiles();
        populateStationNames(piles);
        return piles;
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
        if (pileRepository.existsByPileNo(pile.getPileNo())) {
            throw new IllegalArgumentException("充电桩编号已存在");
        }

        // 验证充电站是否存在
        stationService.getStationById(pile.getStationId());

        // 设置默认值
        if (pile.getStatus() == null) {
            pile.setStatus((byte) 1); // 1-空闲
        }
        if (pile.getTotalChargeCount() == null) {
            pile.setTotalChargeCount(0);
        }
        if (pile.getTotalChargeAmount() == null) {
            pile.setTotalChargeAmount(new BigDecimal("0.00"));
        }
        if (pile.getHealthScore() == null) {
            pile.setHealthScore((byte) 100);
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
        if (existing == null) {
            throw new IllegalArgumentException("充电桩不存在: " + pile.getId());
        }

        // 保存原始值，用于后续比较
        Long oldStationId = existing.getStationId();
        Byte oldStatus = existing.getStatus();

        // 更新字段
        BeanUtils.copyProperties(pile, existing, "id", "pileNo", "createTime", "updateTime");

        ChargingPile savedPile = pileRepository.save(existing);

        // 清除缓存（更新后旧缓存已失效）
        cacheService.evictPileCache(existing.getId());

        // 如果充电站ID或状态发生变化，更新充电站统计
        if (!Objects.equals(oldStationId, existing.getStationId()) ||
                !Objects.equals(oldStatus, existing.getStatus())) {
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
     * 更新充电桩状态（清除缓存）
     *
     * @param id     充电桩ID
     * @param status 新状态：1空闲 2充电中 3预约中 4故障 5离线
     * @return 更新后的充电桩
     */
    @Transactional
    public ChargingPile updatePileStatus(Long id, Byte status) {
        ChargingPile pile = getPileById(id);
        if (pile == null) {
            throw new IllegalArgumentException("充电桩不存在: " + id);
        }
        pile.setStatus(status);
        ChargingPile savedPile = pileRepository.save(pile);

        // 清除缓存
        cacheService.evictPileCache(id);

        // 更新充电站统计
        stationService.updateStationPileCount(pile.getStationId());

        return savedPile;
    }

    /**
     * 更新充电桩实时数据
     *
     * @param id          充电桩ID
     * @param voltage     电压
     * @param current     电流
     * @return 更新后的充电桩
     */
    @Transactional
    public ChargingPile updatePileRealTimeData(Long id, Integer voltage, BigDecimal current) {
        ChargingPile pile = getPileById(id);
        if (pile == null) {
            throw new IllegalArgumentException("充电桩不存在: " + id);
        }
        pile.setVoltage(voltage);
        pile.setCurrent(current);

        ChargingPile saved = pileRepository.save(pile);
        cacheService.evictPileCache(id);
        return saved;
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
        if (pile == null) {
            throw new IllegalArgumentException("充电桩不存在: " + id);
        }
        pile.setLastMaintenanceTime(LocalDateTime.now());
        pile.setStatus((byte) 5); // 5-离线（维护中）
        pile.setHealthScore((byte) 100); // 维护后重置健康度

        ChargingPile savedPile = pileRepository.save(pile);

        // 清除缓存（维护后状态变化）
        cacheService.evictPileCache(id);

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
        if (pile == null) {
            throw new IllegalArgumentException("充电桩不存在: " + id);
        }
        Long stationId = pile.getStationId();

        pileRepository.deleteById(id);

        // 清除缓存
        cacheService.evictPileCache(id);

        // 更新充电站统计
        stationService.updateStationPileCount(stationId);
    }

    /**
     * 批量填充充电桩的所属站点名称（避免N+1查询）
     *
     * @param piles 充电桩列表
     */
    private void populateStationNames(List<ChargingPile> piles) {
        if (piles == null || piles.isEmpty()) {
            return;
        }

        // 收集所有stationId
        Set<Long> stationIds = piles.stream()
                .map(ChargingPile::getStationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (stationIds.isEmpty()) {
            return;
        }

        // 批量查询站点信息
        Map<Long, String> stationNameMap = stationRepository.findAllById(stationIds).stream()
                .collect(Collectors.toMap(ChargingStation::getId, ChargingStation::getName));

        // 填充stationName
        for (ChargingPile pile : piles) {
            if (pile.getStationId() != null) {
                pile.setStationName(stationNameMap.getOrDefault(pile.getStationId(), "未知站点"));
            }
        }
    }
}
