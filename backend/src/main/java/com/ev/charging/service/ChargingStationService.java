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
     * 获取充电站详情VO
     *
     * @param id 充电站ID
     * @return 充电站VO
     */
    public StationVO getStationVOById(Long id) {
        ChargingStation station = getStationById(id);
        return convertToVO(station);
    }

    /**
     * 搜索充电站（根据名称）
     *
     * @param keyword 关键词
     * @return 充电站列表
     */
    public List<StationVO> searchStations(String keyword) {
        List<ChargingStation> stations = stationRepository.findByNameContaining(keyword);
        return stations.stream()
                .filter(s -> s.getStatus() != null && s.getStatus() == 1)
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 查询附近的充电站
     *
     * @param latitude  用户当前纬度
     * @param longitude 用户当前经度
     * @param radius    搜索半径（千米）
     * @return 充电站列表（按距离排序）
     */
    public List<StationVO> getNearbyStations(Double latitude, Double longitude, Double radius) {
        // 计算经纬度范围（简化算法，实际应考虑地球曲率）
        double lat = radius / 111.0; // 纬度1度约111km
        double lon = radius / (111.0 * Math.cos(Math.toRadians(latitude)));

        double minLat = latitude - lat;
        double maxLat = latitude + lat;
        double minLon = longitude - lon;
        double maxLon = longitude + lon;

        // 查询范围内的充电站
        List<ChargingStation> stations = stationRepository.findNearbyStations(minLat, maxLat, minLon, maxLon);

        // 计算距离并过滤
        List<StationVO> result = new ArrayList<>();
        for (ChargingStation station : stations) {
            double distance = DistanceUtil.calculateDistance(
                    latitude, longitude,
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
     * 更新充电站信息
     *
     * @param station 充电站信息
     * @return 更新后的充电站
     */
    @Transactional
    public ChargingStation updateStation(ChargingStation station) {
        ChargingStation existing = getStationById(station.getId());

        // 更新字段
        BeanUtils.copyProperties(station, existing, "id", "createTime", "updateTime");

        return stationRepository.save(existing);
    }

    /**
     * 更新充电站的充电桩统计信息
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
    }

    /**
     * 删除充电站
     *
     * @param id 充电站ID
     */
    @Transactional
    public void deleteStation(Long id) {
        stationRepository.deleteById(id);
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
