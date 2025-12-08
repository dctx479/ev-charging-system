package com.ev.charging.controller.admin;

import com.ev.charging.common.Result;
import com.ev.charging.dto.MaintenancePlanDTO;
import com.ev.charging.entity.MaintenancePlan;
import com.ev.charging.service.MaintenanceService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 维护计划控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/maintenance")
@CrossOrigin(origins = "*")
public class MaintenanceController {

    @Autowired
    private MaintenanceService maintenanceService;

    /**
     * 创建维护计划
     */
    @PostMapping
    public Result<Long> createMaintenancePlan(@RequestBody @Valid MaintenancePlanDTO dto) {
        log.info("创建维护计划: pileId={}", dto.getPileId());
        Long planId = maintenanceService.createMaintenancePlan(dto);
        return Result.success(planId);
    }

    /**
     * 获取维护计划列表
     */
    @GetMapping
    public Result<Page<MaintenancePlan>> getMaintenancePlanList(
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) Byte maintenanceStatus,
            @RequestParam(required = false) Byte planType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        log.info("查询维护计划列表: stationId={}, status={}", stationId, maintenanceStatus);
        Page<MaintenancePlan> plans = maintenanceService.getMaintenancePlanList(
                stationId, maintenanceStatus, planType, page, pageSize
        );
        return Result.success(plans);
    }

    /**
     * 更新维护计划状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateMaintenanceStatus(
            @PathVariable Long id,
            @RequestParam Byte status,
            @RequestParam(required = false) String note
    ) {
        log.info("更新维护计划状态: planId={}, status={}", id, status);
        maintenanceService.updateMaintenanceStatus(id, status, note);
        return Result.success();
    }

    /**
     * 获取即将到期的维护计划
     */
    @GetMapping("/upcoming")
    public Result<List<MaintenancePlan>> getUpcomingPlans() {
        log.info("查询即将到期的维护计划");
        List<MaintenancePlan> plans = maintenanceService.getUpcomingPlans();
        return Result.success(plans);
    }

    /**
     * 根据充电桩ID获取维护历史
     */
    @GetMapping("/pile/{pileId}")
    public Result<List<MaintenancePlan>> getMaintenanceHistoryByPileId(@PathVariable Long pileId) {
        log.info("查询充电桩维护历史: pileId={}", pileId);
        List<MaintenancePlan> history = maintenanceService.getMaintenanceHistoryByPileId(pileId);
        return Result.success(history);
    }
}
