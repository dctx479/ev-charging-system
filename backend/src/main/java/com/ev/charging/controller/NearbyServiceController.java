package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.service.NearbyServiceService;
import com.ev.charging.vo.NearbyServiceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 周边服务Controller
 */
@RestController
@RequestMapping("/api/nearby")
@RequiredArgsConstructor
@Slf4j
public class NearbyServiceController {

    private final NearbyServiceService nearbyServiceService;

    /**
     * 根据等待时间推荐周边服务
     *
     * @param stationId 充电站ID
     * @param waitTime  预估等待时间（分钟）
     * @return 推荐服务列表
     */
    @GetMapping("/recommend")
    public Result<List<NearbyServiceVO>> getRecommendedServices(
            @RequestParam Long stationId,
            @RequestParam Integer waitTime) {
        log.info("获取周边服务推荐: stationId={}, waitTime={}分钟", stationId, waitTime);

        try {
            List<NearbyServiceVO> services = nearbyServiceService.getRecommendedServices(stationId, waitTime);
            return Result.success("推荐成功", services);
        } catch (Exception e) {
            log.error("获取周边服务推荐失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取站点所有周边服务
     *
     * @param stationId 充电站ID
     * @return 周边服务列表
     */
    @GetMapping("/station/{stationId}")
    public Result<List<NearbyServiceVO>> getAllServices(@PathVariable Long stationId) {
        log.info("获取站点周边服务: stationId={}", stationId);

        try {
            List<NearbyServiceVO> services = nearbyServiceService.getAllServices(stationId);
            return Result.success(services);
        } catch (Exception e) {
            log.error("获取站点周边服务失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据服务类型获取周边服务
     *
     * @param stationId   充电站ID
     * @param serviceType 服务类型：1-餐饮 2-咖啡厅 3-购物 4-娱乐 5-其他
     * @return 周边服务列表
     */
    @GetMapping("/station/{stationId}/type/{serviceType}")
    public Result<List<NearbyServiceVO>> getServicesByType(
            @PathVariable Long stationId,
            @PathVariable Byte serviceType) {
        log.info("获取站点周边服务: stationId={}, serviceType={}", stationId, serviceType);

        try {
            List<NearbyServiceVO> services = nearbyServiceService.getServicesByType(stationId, serviceType);
            return Result.success(services);
        } catch (Exception e) {
            log.error("获取站点周边服务失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据距离获取周边服务
     *
     * @param stationId   充电站ID
     * @param maxDistance 最大距离（米），默认1000米
     * @return 周边服务列表
     */
    @GetMapping("/station/{stationId}/distance")
    public Result<List<NearbyServiceVO>> getServicesByDistance(
            @PathVariable Long stationId,
            @RequestParam(defaultValue = "1000") Integer maxDistance) {
        log.info("获取站点周边服务: stationId={}, maxDistance={}米", stationId, maxDistance);

        try {
            List<NearbyServiceVO> services = nearbyServiceService.getServicesByDistance(stationId, maxDistance);
            return Result.success(services);
        } catch (Exception e) {
            log.error("获取站点周边服务失败", e);
            return Result.error(e.getMessage());
        }
    }
}
