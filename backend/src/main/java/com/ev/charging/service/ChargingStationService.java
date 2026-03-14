package com.ev.charging.service;

import com.ev.charging.common.ResultCode;
import com.ev.charging.entity.ChargingStation;
import com.ev.charging.repository.ChargingPileRepository;
import com.ev.charging.repository.ChargingStationRepository;
import com.ev.charging.util.DistanceUtil;
import com.ev.charging.vo.StationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 充电站服务层
 */
@Service
@RequiredArgsConstructor
public class ChargingStationService {

    private final ChargingStationRepository stationRepository;
    private final ChargingPileRepository pileRepository;
    private final CacheService cacheService;

    /**
     * 获取所有充电站
     *
     * @return 充电站列表
     */
    public List<ChargingStation> getAllStations() {
        return stationRepository.findAll();
    }

    /**
     * 获取所有营业中的充电站
     *
     * @return 充电站列表
     */
    public List<StationVO> getActiveStations() {
        List<ChargingStation> stations = stationRepository.findActiveStationsOrderByRating();
        return stations.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取充电站详情
     *
     * @param id 充电站ID
     * @return 充电站信息
     */
    public ChargingStation getStationById(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(ResultCode.STATION_NOT_FOUND.getMessage()));
    }

    /**
     * 获取充电站详情VO（带缓存）
     *
     * @param id 充电站ID
     * @return 充电站VO
     */
    public StationVO getStationVOById(Long id) {
        // 尝试从缓存获取
        String cacheKey = cacheService.buildStationDetailKey(id);
        StationVO cached = cacheService.get(cacheKey, StationVO.class);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，从数据库查询
        ChargingStation station = getStationById(id);
        StationVO vo = convertToVO(station);

        // 写入缓存
        cacheService.set(cacheKey, vo, CacheService.STATION_DETAIL_TTL);

        return vo;
    }

    /**
     * 搜索充电站（根据名称）
     *
     * @param keyword 关键词
     * @return 充电站列表
     */
    public List<StationVO> searchStations(String keyword) {
        // 对 % _ \ 转义，防止 LIKE 通配符注入
        String escaped = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        List<ChargingStation> stations = stationRepository.findByNameLike("%" + escaped + "%");
        return stations.stream()
                .filter(s -> s.getStatus() != null && s.getStatus() == 1)
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 查询附近的充电站（带缓存）
     *
     * @param latitude  用户当前纬度
     * @param longitude 用户当前经度
     * @param radius    搜索半径（千米）
     * @return 充电站列表（按距离排序）
     */
    public List<StationVO> getNearbyStations(Double latitude, Double longitude, Double radius) {
        // 参数验证
        if (latitude == null || longitude == null || radius == null) {
            throw new IllegalArgumentException("经纬度和搜索半径不能为空");
        }
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("纬度范围必须在 -90 到 90 之间");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("经度范围必须在 -180 到 180 之间");
        }
        if (radius <= 0 || radius > 100) {
            throw new IllegalArgumentException("搜索半径必须在 0-100 公里之间");
        }

        // 转换为BigDecimal进行计算
        BigDecimal latBD = BigDecimal.valueOf(latitude);
        BigDecimal lonBD = BigDecimal.valueOf(longitude);
        BigDecimal radiusBD = BigDecimal.valueOf(radius);

        // 尝试从缓存获取
        String cacheKey = cacheService.buildStationListKey(latitude, longitude, radius);
        List<StationVO> cached = cacheService.get(cacheKey, List.class);
        if (cached != null) {
            return cached;
        }

        // 计算经纬度范围（简化算法，实际应考虑地球曲率）
        double lat = radius / 111.0; // 纬度1度约111km
        double lon = radius / (111.0 * Math.cos(Math.toRadians(latitude)));

        BigDecimal latRange = BigDecimal.valueOf(lat);
        BigDecimal lonRange = BigDecimal.valueOf(lon);

        BigDecimal minLat = latBD.subtract(latRange);
        BigDecimal maxLat = latBD.add(latRange);
        BigDecimal minLon = lonBD.subtract(lonRange);
        BigDecimal maxLon = lonBD.add(lonRange);

        // 查询范围内的充电站
        List<ChargingStation> stations = stationRepository.findNearbyStations(
                minLat.doubleValue(), maxLat.doubleValue(),
                minLon.doubleValue(), maxLon.doubleValue()
        );

        // 计算距离并过滤
        List<StationVO> result = new ArrayList<>();
        for (ChargingStation station : stations) {
            // 跳过经纬度缺失的站点，防止自动拆箱NPE
            if (station.getLatitude() == null || station.getLongitude() == null) {
                continue;
            }
            double distance = DistanceUtil.calculateDistance(
                    latBD, lonBD,
                    station.getLatitude(), station.getLongitude()
            );

            // 只保留在指定半径内的充电站
            if (distance <= radius) {
                StationVO vo = convertToVO(station);
                vo.setDistance(distance);
                vo.setDistanceText(DistanceUtil.formatDistance(distance));
                result.add(vo);
            }
        }

        // 按距离排序
        result.sort(Comparator.comparing(StationVO::getDistance));

        // 写入缓存
        cacheService.set(cacheKey, result, CacheService.STATION_LIST_TTL);

        return result;
    }

    /**
     * 创建充电站
     *
     * @param station 充电站信息
     * @return 创建后的充电站
     */
    @Transactional
    public ChargingStation createStation(ChargingStation station) {
        // 初始化默认值
        if (station.getStatus() == null) {
            station.setStatus((byte) 1);
        }
        if (station.getTotalPiles() == null) {
            station.setTotalPiles(0);
        }
        if (station.getAvailablePiles() == null) {
            station.setAvailablePiles(0);
        }
        if (station.getRating() == null) {
            station.setRating(0.0);
        }
        if (station.getReviewCount() == null) {
            station.setReviewCount(0);
        }

        return stationRepository.save(station);
    }

    /**
     * 更新充电站信息（清除缓存）
     *
     * @param station 充电站信息
     * @return 更新后的充电站
     */
    @Transactional
    public ChargingStation updateStation(ChargingStation station) {
        ChargingStation existing = getStationById(station.getId());

        // 更新字段
        BeanUtils.copyProperties(station, existing, "id", "createTime", "updateTime");

        ChargingStation updated = stationRepository.save(existing);

        // 清除相关缓存
        cacheService.evictStationCache(station.getId());

        return updated;
    }

    /**
     * 更新充电站的充电桩统计信息（清除缓存）
     *
     * @param stationId 充电站ID
     */
    @Transactional
    public void updateStationPileCount(Long stationId) {
        ChargingStation station = getStationById(stationId);

        long totalPiles = pileRepository.countByStationId(stationId);
        long availablePiles = pileRepository.countByStationIdAndStatus(stationId, (byte) 1);

        station.setTotalPiles((int) totalPiles);
        station.setAvailablePiles((int) availablePiles);

        stationRepository.save(station);

        // 清除相关缓存
        cacheService.evictStationCache(stationId);
    }

    /**
     * 删除充电站
     *
     * @param id 充电站ID
     */
    @Transactional
    public void deleteStation(Long id) {
        stationRepository.deleteById(id);
        cacheService.evictStationCache(id);
    }

    /**
     * 将实体转换为VO
     *
     * @param station 充电站实体
     * @return 充电站VO
     */
    private StationVO convertToVO(ChargingStation station) {
        StationVO vo = new StationVO();
        BeanUtils.copyProperties(station, vo, "status");

        // 转换状态：Byte -> String
        if (station.getStatus() != null) {
            switch (station.getStatus()) {
                case 1:
                    vo.setStatus("ACTIVE");
                    break;
                case 2:
                    vo.setStatus("MAINTENANCE");
                    break;
                case 0:
                default:
                    vo.setStatus("CLOSED");
                    break;
            }
        } else {
            vo.setStatus("CLOSED");
        }

        return vo;
    }
}
