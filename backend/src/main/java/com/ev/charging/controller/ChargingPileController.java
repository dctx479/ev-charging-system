package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.entity.ChargingPile;
import com.ev.charging.service.ChargingPileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 充电桩控制器
 */
@Tag(name = "充电桩管理", description = "充电桩查询、状态管理、维护记录等功能")
@RestController
@RequestMapping(value = "/piles", produces = "application/json;charset=UTF-8")
@RequiredArgsConstructor
public class ChargingPileController {

    private final ChargingPileService pileService;

    /**
     * 获取所有充电桩
     *
     * @return 充电桩列表
     */
    @Operation(summary = "获取所有充电桩", description = "查询系统中所有充电桩列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
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
    @Operation(summary = "获取充电桩详情", description = "根据充电桩ID查询详细信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "充电桩不存在")
    })
    @GetMapping("/{id}")
    public Result<ChargingPile> getPileById(
            @Parameter(description = "充电桩ID", required = true, example = "1")
            @PathVariable Long id) {
        ChargingPile pile = pileService.getPileById(id);
        return Result.success(pile);
    }

    /**
     * 根据充电站ID获取充电桩列表
     *
     * @param stationId 充电站ID
     * @return 充电桩列表
     */
    @Operation(summary = "获取充电站的充电桩列表", description = "根据充电站ID查询该站所有充电桩")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "充电站不存在")
    })
    @GetMapping("/station/{stationId}")
    public Result<List<ChargingPile>> getPilesByStationId(
            @Parameter(description = "充电站ID", required = true, example = "1")
            @PathVariable Long stationId) {
        List<ChargingPile> piles = pileService.getPilesByStationId(stationId);
        return Result.success(piles);
    }

    /**
     * 获取充电站的可用充电桩
     *
     * @param stationId 充电站ID
     * @return 可用充电桩列表
     */
    @Operation(summary = "获取可用充电桩", description = "查询充电站内状态为空闲的充电桩列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/station/{stationId}/available")
    public Result<List<ChargingPile>> getAvailablePiles(
            @Parameter(description = "充电站ID", required = true, example = "1")
            @PathVariable Long stationId) {
        List<ChargingPile> piles = pileService.getAvailablePiles(stationId);
        return Result.success(piles);
    }

    /**
     * 获取所有故障充电桩（管理员功能）
     *
     * @return 故障充电桩列表
     */
    @Operation(
            summary = "获取故障充电桩列表",
            description = "查询所有状态为故障的充电桩（管理员功能）",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
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
    @Operation(
            summary = "创建充电桩",
            description = "创建新的充电桩（管理员功能）",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<ChargingPile> createPile(
            @Parameter(description = "充电桩信息", required = true)
            @RequestBody @Valid ChargingPile pile) {
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
    @Operation(
            summary = "更新充电桩信息",
            description = "更新充电桩的配置信息（管理员功能）",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "404", description = "充电桩不存在")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<ChargingPile> updatePile(
            @Parameter(description = "充电桩ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "充电桩信息", required = true)
            @RequestBody @Valid ChargingPile pile) {
        pile.setId(id);
        ChargingPile updated = pileService.updatePile(pile);
        return Result.success("更新成功", updated);
    }

    /**
     * 更新充电桩状态（管理员功能）
     *
     * @param id     充电桩ID
     * @param status 新状态：1空闲 2充电中 3预约中 4故障 5离线
     * @return 更新结果
     */
    @Operation(
            summary = "更新充电桩状态",
            description = "更新充电桩的运行状态（管理员功能）",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "状态更新成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "404", description = "充电桩不存在")
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<ChargingPile> updatePileStatus(
            @Parameter(description = "充电桩ID", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "状态：1-空闲，2-充电中，3-预约中，4-故障，5-离线", required = true, example = "1")
            @RequestParam Byte status) {
        ChargingPile updated = pileService.updatePileStatus(id, status);
        return Result.success("状态更新成功", updated);
    }

    /**
     * 记录充电桩维护（管理员功能）
     *
     * @param id 充电桩ID
     * @return 维护记录结果
     */
    @Operation(
            summary = "记录充电桩维护",
            description = "记录充电桩维护操作（管理员功能）",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "记录成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "404", description = "充电桩不存在")
    })
    @PostMapping("/{id}/maintenance")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<ChargingPile> recordMaintenance(
            @Parameter(description = "充电桩ID", required = true, example = "1")
            @PathVariable Long id) {
        ChargingPile updated = pileService.recordMaintenance(id);
        return Result.success("维护记录成功", updated);
    }

    /**
     * 删除充电桩（管理员功能）
     *
     * @param id 充电桩ID
     * @return 删除结果
     */
    @Operation(
            summary = "删除充电桩",
            description = "删除指定充电桩（管理员功能，谨慎操作）",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "404", description = "充电桩不存在")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Void> deletePile(
            @Parameter(description = "充电桩ID", required = true, example = "1")
            @PathVariable Long id) {
        pileService.deletePile(id);
        return Result.success("删除成功", null);
    }
}
