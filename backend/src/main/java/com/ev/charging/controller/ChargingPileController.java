package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.entity.ChargingPile;
import com.ev.charging.service.ChargingPileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 充电桩控制器
 */
@RestController
@RequestMapping("/piles")
@RequiredArgsConstructor
public class ChargingPileController {

    private final ChargingPileService pileService;

    /**
     * 获取所有充电桩
     *
     * @return 充电桩列表
     */
    @GetMapping
    public Result<List<ChargingPile>> getAllPiles() {
        List<ChargingPile> piles = pileService.getAllPiles();
        return Result.success(piles);
    }

    /**
     * 根据ID获取充电桩详情
     *
     * @param id 充电桩ID
     * @return 充电桩详情
     */
    @GetMapping("/{id}")
    public Result<ChargingPile> getPileById(@PathVariable Long id) {
        ChargingPile pile = pileService.getPileById(id);
        return Result.success(pile);
    }

    /**
     * 根据充电站ID获取充电桩列表
     *
     * @param stationId 充电站ID
     * @return 充电桩列表
     */
    @GetMapping("/station/{stationId}")
    public Result<List<ChargingPile>> getPilesByStationId(@PathVariable Long stationId) {
        List<ChargingPile> piles = pileService.getPilesByStationId(stationId);
        return Result.success(piles);
    }

    /**
     * 获取充电站的可用充电桩
     *
     * @param stationId 充电站ID
     * @return 可用充电桩列表
     */
    @GetMapping("/station/{stationId}/available")
    public Result<List<ChargingPile>> getAvailablePiles(@PathVariable Long stationId) {
        List<ChargingPile> piles = pileService.getAvailablePiles(stationId);
        return Result.success(piles);
    }

    /**
     * 获取所有故障充电桩（管理员功能）
     *
     * @return 故障充电桩列表
     */
    @GetMapping("/fault")
    public Result<List<ChargingPile>> getFaultPiles() {
        List<ChargingPile> piles = pileService.getFaultPiles();
        return Result.success(piles);
    }

    /**
     * 创建充电桩（管理员功能）
     *
     * @param pile 充电桩信息
     * @return 创建结果
     */
    @PostMapping
    public Result<ChargingPile> createPile(@RequestBody ChargingPile pile) {
        ChargingPile created = pileService.createPile(pile);
        return Result.success("创建成功", created);
    }

    /**
     * 更新充电桩信息（管理员功能）
     *
     * @param id   充电桩ID
     * @param pile 充电桩信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result<ChargingPile> updatePile(@PathVariable Long id, @RequestBody ChargingPile pile) {
        pile.setId(id);
        ChargingPile updated = pileService.updatePile(pile);
        return Result.success("更新成功", updated);
    }

    /**
     * 更新充电桩状态（管理员功能）
     *
     * @param id     充电桩ID
     * @param status 新状态
     * @return 更新结果
     */
    @PatchMapping("/{id}/status")
    public Result<ChargingPile> updatePileStatus(@PathVariable Long id, @RequestParam String status) {
        ChargingPile updated = pileService.updatePileStatus(id, status);
        return Result.success("状态更新成功", updated);
    }

    /**
     * 记录充电桩维护（管理员功能）
     *
     * @param id 充电桩ID
     * @return 维护记录结果
     */
    @PostMapping("/{id}/maintenance")
    public Result<ChargingPile> recordMaintenance(@PathVariable Long id) {
        ChargingPile updated = pileService.recordMaintenance(id);
        return Result.success("维护记录成功", updated);
    }

    /**
     * 删除充电桩（管理员功能）
     *
     * @param id 充电桩ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deletePile(@PathVariable Long id) {
        pileService.deletePile(id);
        return Result.success("删除成功", null);
    }
}
