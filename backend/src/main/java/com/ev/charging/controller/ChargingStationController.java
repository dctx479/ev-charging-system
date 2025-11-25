package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.entity.ChargingStation;
import com.ev.charging.service.ChargingStationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ev.charging.vo.StationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 充电站控制器
 */
@Tag(name = "充电站管理", description = "充电站查询、搜索、附近站点等接口")
@RestController
@RequestMapping(value = "/stations", produces = "application/json;charset=UTF-8")
@RequiredArgsConstructor
public class ChargingStationController {

    private final ChargingStationService stationService;

    /**
     * 获取所有营业中的充电站
     *
     * @return 充电站列表
     */
    @Operation(summary = "获取所有营业中的充电站", description = "返回所有状态为营业中的充电站列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
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
    @Operation(summary = "获取充电站详情", description = "根据充电站ID获取详细信息，包括充电桩数量、可用状态等")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "充电站不存在")
    })
    @GetMapping("/{id}")
    public Result<StationVO> getStationById(
            @Parameter(description = "充电站ID", required = true) @PathVariable Long id) {
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
    @Operation(summary = "查询附近充电站", description = "基于用户当前位置，使用Haversine公式计算附近的充电站，按距离排序")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/nearby")
    public Result<List<StationVO>> getNearbyStations(
            @Parameter(description = "当前纬度", required = true, example = "39.9042") @RequestParam Double latitude,
            @Parameter(description = "当前经度", required = true, example = "116.4074") @RequestParam Double longitude,
            @Parameter(description = "搜索半径（千米）", example = "5") @RequestParam(defaultValue = "5") Double radius) {
        List<StationVO> stations = stationService.getNearbyStations(latitude, longitude, radius);
        return Result.success(stations);
    }

    /**
     * 创建充电站（管理员功能）
     *
     * @param station 充电站信息
     * @return 创建结果
     */
    @Operation(
            summary = "创建充电站",
            description = "管理员创建新的充电站（需要管理员权限）",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "401", description = "未授权"),
            @ApiResponse(responseCode = "403", description = "无权限")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<ChargingStation> createStation(
            @Parameter(description = "充电站信息", required = true) @RequestBody @Valid ChargingStation station) {
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
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<ChargingStation> updateStation(@PathVariable Long id, @RequestBody @Valid ChargingStation station) {
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
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Void> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        return Result.success("删除成功", null);
    }
}
