package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.entity.ChargingStation;
import com.ev.charging.service.ChargingStationService;
import com.ev.charging.vo.StationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 充电站控制器
 */
@RestController
@RequestMapping("/stations")
@RequiredArgsConstructor
public class ChargingStationController {

    private final ChargingStationService stationService;

    /**
     * 获取所有营业中的充电站
     *
     * @return 充电站列表
     */
    @GetMapping
    public Result<List<StationVO>> getActiveStations() {
        List<StationVO> stations = stationService.getActiveStations();
        return Result.success(stations);
    }

    /**
     * 根据ID获取充电站详情
     *
     * @param id 充电站ID
     * @return 充电站详情
     */
    @GetMapping("/{id}")
    public Result<StationVO> getStationById(@PathVariable Long id) {
        StationVO station = stationService.getStationVOById(id);
        return Result.success(station);
    }

    /**
     * 搜索充电站
     *
     * @param keyword 关键词
     * @return 充电站列表
     */
    @GetMapping("/search")
    public Result<List<StationVO>> searchStations(@RequestParam String keyword) {
        List<StationVO> stations = stationService.searchStations(keyword);
        return Result.success(stations);
    }

    /**
     * 查询附近的充电站
     *
     * @param latitude  当前纬度
     * @param longitude 当前经度
     * @param radius    搜索半径（千米），默认5km
     * @return 附近充电站列表（按距离排序）
     */
    @GetMapping("/nearby")
    public Result<List<StationVO>> getNearbyStations(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5") Double radius) {
        List<StationVO> stations = stationService.getNearbyStations(latitude, longitude, radius);
        return Result.success(stations);
    }

    /**
     * 创建充电站（管理员功能）
     *
     * @param station 充电站信息
     * @return 创建结果
     */
    @PostMapping
    public Result<ChargingStation> createStation(@RequestBody ChargingStation station) {
        ChargingStation created = stationService.createStation(station);
        return Result.success("创建成功", created);
    }

    /**
     * 更新充电站信息（管理员功能）
     *
     * @param id      充电站ID
     * @param station 充电站信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result<ChargingStation> updateStation(@PathVariable Long id, @RequestBody ChargingStation station) {
        station.setId(id);
        ChargingStation updated = stationService.updateStation(station);
        return Result.success("更新成功", updated);
    }

    /**
     * 删除充电站（管理员功能）
     *
     * @param id 充电站ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        return Result.success("删除成功", null);
    }
}
