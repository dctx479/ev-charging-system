package com.ev.charging.service;

import com.ev.charging.entity.NearbyService;
import com.ev.charging.repository.NearbyServiceRepository;
import com.ev.charging.vo.NearbyServiceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 周边服务Service
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NearbyServiceService {

    private final NearbyServiceRepository nearbyServiceRepository;

    /**
     * 根据等待时间推荐周边服务
     *
     * @param stationId 充电站ID
     * @param waitTime  预估等待时间（分钟）
     * @return 推荐服务列表（Top 10）
     */
    public List<NearbyServiceVO> getRecommendedServices(Long stationId, Integer waitTime) {
        log.info("推荐周边服务: stationId={}, waitTime={}分钟", stationId, waitTime);

        // 计算推荐时间范围
        // 最小时间：等待时间的50%（留出充足时间）
        // 最大时间：等待时间的120%（留20%缓冲）
        int minTime = (int) (waitTime * 0.5);
        int maxTime = (int) (waitTime * 1.2);

        List<NearbyService> services = nearbyServiceRepository.findRecommendedServices(
                stationId, minTime, maxTime, (byte) 1);

        // 转换为VO并添加推荐理由
        List<NearbyServiceVO> voList = services.stream()
                .limit(10)  // 只返回Top 10
                .map(service -> {
                    NearbyServiceVO vo = convertToVO(service);
                    vo.setRecommendReason(generateRecommendReason(service, waitTime));
                    return vo;
                })
                .collect(Collectors.toList());

        log.info("推荐周边服务: 共找到{}条匹配服务", voList.size());

        return voList;
    }

    /**
     * 获取站点所有周边服务
     *
     * @param stationId 充电站ID
     * @return 周边服务列表
     */
    public List<NearbyServiceVO> getAllServices(Long stationId) {
        log.info("获取站点周边服务: stationId={}", stationId);

        List<NearbyService> services = nearbyServiceRepository
                .findByStationIdAndStatusOrderByRatingDescDistanceAsc(stationId, (byte) 1);

        return services.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 根据服务类型获取周边服务
     *
     * @param stationId   充电站ID
     * @param serviceType 服务类型
     * @return 周边服务列表
     */
    public List<NearbyServiceVO> getServicesByType(Long stationId, Byte serviceType) {
        log.info("获取站点周边服务: stationId={}, serviceType={}", stationId, serviceType);

        List<NearbyService> services = nearbyServiceRepository
                .findByStationIdAndServiceTypeAndStatusOrderByRatingDescDistanceAsc(
                        stationId, serviceType, (byte) 1);

        return services.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 根据距离获取周边服务
     *
     * @param stationId   充电站ID
     * @param maxDistance 最大距离（米）
     * @return 周边服务列表
     */
    public List<NearbyServiceVO> getServicesByDistance(Long stationId, Integer maxDistance) {
        log.info("获取站点周边服务: stationId={}, maxDistance={}米", stationId, maxDistance);

        List<NearbyService> services = nearbyServiceRepository.findByDistance(
                stationId, maxDistance, (byte) 1);

        return services.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为VO
     *
     * @param service 周边服务实体
     * @return NearbyServiceVO
     */
    private NearbyServiceVO convertToVO(NearbyService service) {
        return NearbyServiceVO.builder()
                .id(service.getId())
                .serviceName(service.getServiceName())
                .serviceType(service.getServiceType())
                .serviceTypeText(NearbyServiceVO.getServiceTypeText(service.getServiceType()))
                .description(service.getDescription())
                .distance(service.getDistance())
                .distanceText(NearbyServiceVO.formatDistance(service.getDistance()))
                .avgConsumeTime(service.getAvgConsumeTime())
                .rating(service.getRating())
                .address(service.getAddress())
                .phone(service.getPhone())
                .businessHours(service.getBusinessHours())
                .couponInfo(service.getCouponInfo())
                .longitude(service.getLongitude())
                .latitude(service.getLatitude())
                .build();
    }

    /**
     * 生成推荐理由
     *
     * @param service  周边服务
     * @param waitTime 等待时间
     * @return 推荐理由
     */
    private String generateRecommendReason(NearbyService service, Integer waitTime) {
        int consumeTime = service.getAvgConsumeTime();
        String serviceTypeName = NearbyServiceVO.getServiceTypeText(service.getServiceType());

        if (consumeTime <= 20) {
            return String.format("等待%d分钟，推荐您去%s休息", waitTime, serviceTypeName);
        } else if (consumeTime <= 60) {
            return String.format("等待%d分钟，刚好可以享用%s", waitTime, serviceTypeName);
        } else {
            return String.format("等待%d分钟，有充足时间体验%s", waitTime, serviceTypeName);
        }
    }
}
