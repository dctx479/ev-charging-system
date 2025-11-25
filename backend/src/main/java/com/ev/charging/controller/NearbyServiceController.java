package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.service.AmapPoiService;
import com.ev.charging.service.NearbyServiceService;
import com.ev.charging.vo.NearbyServiceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 周边服务Controller
 */
@Tag(name = "周边服务", description = "充电等待期间周边服务推荐、POI搜索等相关接口")
@RestController
@RequestMapping(value = "/nearby", produces = "application/json;charset=UTF-8")
@RequiredArgsConstructor
@Slf4j
public class NearbyServiceController {

    private final NearbyServiceService nearbyServiceService;
    private final AmapPoiService amapPoiService;

    /**
     * 根据等待时间推荐周边服务
     *
     * @param stationId 充电站ID
     * @param waitTime  预估等待时间（分钟）
     * @return 推荐服务列表
     */
    @Operation(
        summary = "智能推荐周边服务",
        description = "根据充电站位置和预估等待时间，智能推荐合适的周边服务（餐饮、咖啡厅、购物、娱乐等），优先推荐距离适中且能在等待时间内完成的服务",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "推荐成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/recommend")
    public Result<List<NearbyServiceVO>> getRecommendedServices(
            @Parameter(description = "充电站ID", required = true, example = "1")
            @RequestParam Long stationId,
            @Parameter(description = "预估等待时间（分钟）", required = true, example = "30")
            @RequestParam Integer waitTime) {
        log.info("获取周边服务推荐: stationId={}, waitTime={}分钟", stationId, waitTime);

        try {
            List<NearbyServiceVO> services = nearbyServiceService.getRecommendedServices(stationId, waitTime);
            return Result.success("推荐成功", services);
        } catch (Exception e) {
            log.error("获取周边服务推荐失败", e);
            return Result.error("获取推荐失败，请稍后重试");
        }
    }

    /**
     * 获取站点所有周边服务
     *
     * @param stationId 充电站ID
     * @return 周边服务列表
     */
    @Operation(
        summary = "获取站点所有周边服务",
        description = "获取指定充电站周边的所有服务点，包含餐饮、购物、娱乐等各类服务",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/station/{stationId}")
    public Result<List<NearbyServiceVO>> getAllServices(
            @Parameter(description = "充电站ID", required = true, example = "1")
            @PathVariable Long stationId) {
        log.info("获取站点周边服务: stationId={}", stationId);

        try {
            List<NearbyServiceVO> services = nearbyServiceService.getAllServices(stationId);
            return Result.success(services);
        } catch (Exception e) {
            log.error("获取站点周边服务失败", e);
            return Result.error("获取周边服务失败，请稍后重试");
        }
    }

    /**
     * 根据服务类型获取周边服务
     *
     * @param stationId   充电站ID
     * @param serviceType 服务类型：1-餐饮 2-咖啡厅 3-购物 4-娱乐 5-其他
     * @return 周边服务列表
     */
    @Operation(
        summary = "按类型查询周边服务",
        description = "根据服务类型筛选充电站周边的服务点，服务类型：1-餐饮 2-咖啡厅 3-购物 4-娱乐 5-其他",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "服务类型参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/station/{stationId}/type/{serviceType}")
    public Result<List<NearbyServiceVO>> getServicesByType(
            @Parameter(description = "充电站ID", required = true, example = "1")
            @PathVariable Long stationId,
            @Parameter(description = "服务类型：1-餐饮 2-咖啡厅 3-购物 4-娱乐 5-其他", required = true, example = "1")
            @PathVariable Byte serviceType) {
        log.info("获取站点周边服务: stationId={}, serviceType={}", stationId, serviceType);

        try {
            List<NearbyServiceVO> services = nearbyServiceService.getServicesByType(stationId, serviceType);
            return Result.success(services);
        } catch (Exception e) {
            log.error("获取站点周边服务失败", e);
            return Result.error("获取周边服务失败，请稍后重试");
        }
    }

    /**
     * 根据距离获取周边服务
     *
     * @param stationId   充电站ID
     * @param maxDistance 最大距离（米），默认1000米
     * @return 周边服务列表
     */
    @Operation(
        summary = "按距离查询周边服务",
        description = "根据最大距离筛选充电站周边的服务点，返回指定距离范围内的所有服务",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/station/{stationId}/distance")
    public Result<List<NearbyServiceVO>> getServicesByDistance(
            @Parameter(description = "充电站ID", required = true, example = "1")
            @PathVariable Long stationId,
            @Parameter(description = "最大距离（米）", example = "1000")
            @RequestParam(defaultValue = "1000") Integer maxDistance) {
        log.info("获取站点周边服务: stationId={}, maxDistance={}米", stationId, maxDistance);

        try {
            List<NearbyServiceVO> services = nearbyServiceService.getServicesByDistance(stationId, maxDistance);
            return Result.success(services);
        } catch (Exception e) {
            log.error("获取站点周边服务失败", e);
            return Result.error("获取周边服务失败，请稍后重试");
        }
    }

    // ==================== 高德地图POI搜索 ====================

    /**
     * 搜索周边POI（使用高德地图API）
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @param types     POI类型（可选）
     * @param radius    搜索半径（米），默认1000
     * @param limit     返回数量，默认20
     * @return POI列表
     */
    @Operation(
        summary = "搜索周边POI",
        description = "使用高德地图API搜索指定位置周边的POI（兴趣点），支持自定义POI类型、搜索半径和返回数量",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功"),
        @ApiResponse(responseCode = "400", description = "经纬度参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误或高德API调用失败")
    })
    @GetMapping("/poi/search")
    public Result<List<Map<String, Object>>> searchNearbyPoi(
            @Parameter(description = "经度", required = true, example = "116.397428")
            @RequestParam BigDecimal longitude,
            @Parameter(description = "纬度", required = true, example = "39.90923")
            @RequestParam BigDecimal latitude,
            @Parameter(description = "POI类型代码（高德地图类型编码，如050000表示餐饮服务）", example = "050000")
            @RequestParam(required = false) String types,
            @Parameter(description = "搜索半径（米）", example = "1000")
            @RequestParam(required = false, defaultValue = "1000") Integer radius,
            @Parameter(description = "返回数量", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer limit) {

        log.info("搜索周边POI: lng={}, lat={}, types={}, radius={}米", longitude, latitude, types, radius);

        List<Map<String, Object>> pois = amapPoiService.searchNearbyPoi(
                longitude, latitude, types, radius, limit);

        return Result.success(pois);
    }

    /**
     * 搜索周边餐饮服务（使用高德地图API）
     */
    @Operation(
        summary = "搜索周边餐饮",
        description = "使用高德地图API搜索指定位置周边的餐饮服务，包括中餐、西餐、快餐等",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误或高德API调用失败")
    })
    @GetMapping("/poi/restaurants")
    public Result<List<Map<String, Object>>> searchNearbyRestaurants(
            @Parameter(description = "经度", required = true, example = "116.397428")
            @RequestParam BigDecimal longitude,
            @Parameter(description = "纬度", required = true, example = "39.90923")
            @RequestParam BigDecimal latitude,
            @Parameter(description = "搜索半径（米）", example = "1000")
            @RequestParam(required = false, defaultValue = "1000") Integer radius) {

        log.info("搜索周边餐饮: lng={}, lat={}, radius={}米", longitude, latitude, radius);

        List<Map<String, Object>> restaurants = amapPoiService.searchNearbyRestaurants(
                longitude, latitude, radius);

        return Result.success(restaurants);
    }

    /**
     * 搜索周边购物服务（使用高德地图API）
     */
    @Operation(
        summary = "搜索周边购物",
        description = "使用高德地图API搜索指定位置周边的购物场所，包括商场、超市、便利店等",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误或高德API调用失败")
    })
    @GetMapping("/poi/shopping")
    public Result<List<Map<String, Object>>> searchNearbyShopping(
            @Parameter(description = "经度", required = true, example = "116.397428")
            @RequestParam BigDecimal longitude,
            @Parameter(description = "纬度", required = true, example = "39.90923")
            @RequestParam BigDecimal latitude,
            @Parameter(description = "搜索半径（米）", example = "1000")
            @RequestParam(required = false, defaultValue = "1000") Integer radius) {

        log.info("搜索周边购物: lng={}, lat={}, radius={}米", longitude, latitude, radius);

        List<Map<String, Object>> shopping = amapPoiService.searchNearbyShopping(
                longitude, latitude, radius);

        return Result.success(shopping);
    }

    /**
     * 搜索周边生活服务（使用高德地图API）
     */
    @Operation(
        summary = "搜索周边生活服务",
        description = "使用高德地图API搜索指定位置周边的生活服务设施，包括银行、ATM、邮局等",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误或高德API调用失败")
    })
    @GetMapping("/poi/life-service")
    public Result<List<Map<String, Object>>> searchNearbyLifeService(
            @Parameter(description = "经度", required = true, example = "116.397428")
            @RequestParam BigDecimal longitude,
            @Parameter(description = "纬度", required = true, example = "39.90923")
            @RequestParam BigDecimal latitude,
            @Parameter(description = "搜索半径（米）", example = "1000")
            @RequestParam(required = false, defaultValue = "1000") Integer radius) {

        log.info("搜索周边生活服务: lng={}, lat={}, radius={}米", longitude, latitude, radius);

        List<Map<String, Object>> lifeService = amapPoiService.searchNearbyLifeService(
                longitude, latitude, radius);

        return Result.success(lifeService);
    }

    /**
     * 搜索周边休闲娱乐（使用高德地图API）
     */
    @Operation(
        summary = "搜索周边休闲娱乐",
        description = "使用高德地图API搜索指定位置周边的休闲娱乐场所，包括电影院、KTV、游戏厅等",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误或高德API调用失败")
    })
    @GetMapping("/poi/entertainment")
    public Result<List<Map<String, Object>>> searchNearbyEntertainment(
            @Parameter(description = "经度", required = true, example = "116.397428")
            @RequestParam BigDecimal longitude,
            @Parameter(description = "纬度", required = true, example = "39.90923")
            @RequestParam BigDecimal latitude,
            @Parameter(description = "搜索半径（米）", example = "1000")
            @RequestParam(required = false, defaultValue = "1000") Integer radius) {

        log.info("搜索周边休闲娱乐: lng={}, lat={}, radius={}米", longitude, latitude, radius);

        List<Map<String, Object>> entertainment = amapPoiService.searchNearbyEntertainment(
                longitude, latitude, radius);

        return Result.success(entertainment);
    }

    /**
     * 搜索周边酒店住宿（使用高德地图API）
     */
    @Operation(
        summary = "搜索周边酒店住宿",
        description = "使用高德地图API搜索指定位置周边的酒店和住宿设施",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误或高德API调用失败")
    })
    @GetMapping("/poi/hotels")
    public Result<List<Map<String, Object>>> searchNearbyHotels(
            @Parameter(description = "经度", required = true, example = "116.397428")
            @RequestParam BigDecimal longitude,
            @Parameter(description = "纬度", required = true, example = "39.90923")
            @RequestParam BigDecimal latitude,
            @Parameter(description = "搜索半径（米）", example = "1000")
            @RequestParam(required = false, defaultValue = "1000") Integer radius) {

        log.info("搜索周边酒店: lng={}, lat={}, radius={}米", longitude, latitude, radius);

        List<Map<String, Object>> hotels = amapPoiService.searchNearbyHotels(
                longitude, latitude, radius);

        return Result.success(hotels);
    }
}
