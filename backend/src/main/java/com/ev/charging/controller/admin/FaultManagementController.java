package com.ev.charging.controller.admin;

import com.ev.charging.common.Result;
import com.ev.charging.dto.FaultQueryDTO;
import com.ev.charging.dto.FaultReportDTO;
import com.ev.charging.dto.RepairUpdateDTO;
import com.ev.charging.service.FaultService;
import com.ev.charging.vo.FaultRecordVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 故障管理控制器
 */
@Tag(name = "故障管理", description = "充电桩故障上报、维修跟踪、故障统计")
@Slf4j
@RestController
@RequestMapping(value = "/admin/faults", produces = "application/json;charset=UTF-8")
public class FaultManagementController {

    @Autowired
    private FaultService faultService;

    /**
     * 上报故障
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Long> reportFault(@RequestBody @Valid FaultReportDTO dto) {
        log.info("上报故障: pileId={}, faultType={}", dto.getPileId(), dto.getFaultType());
        Long faultId = faultService.reportFault(dto);
        return Result.success(faultId);
    }

    /**
     * 获取故障列表（分页）
     */
    @GetMapping
    public Result<Page<FaultRecordVO>> getFaultList(@Valid FaultQueryDTO queryDTO) {
        log.info("查询故障列表: stationId={}, repairStatus={}, severity={}, faultType={}, page={}, pageSize={}",
                 queryDTO.getStationId(), queryDTO.getRepairStatus(), queryDTO.getSeverity(),
                 queryDTO.getFaultType(), queryDTO.getPage(), queryDTO.getPageSize());
        Page<FaultRecordVO> page = faultService.getFaultList(queryDTO);
        return Result.success(page);
    }

    /**
     * 根据ID获取故障详情
     */
    @GetMapping("/{id}")
    public Result<FaultRecordVO> getFaultById(@PathVariable Long id) {
        log.info("查询故障详情: id={}", id);
        FaultRecordVO vo = faultService.getFaultById(id);
        return Result.success(vo);
    }

    /**
     * 更新维修状态
     */
    @PutMapping("/{id}/repair")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Void> updateRepairStatus(
            @PathVariable Long id,
            @RequestBody @Valid RepairUpdateDTO dto
    ) {
        log.info("更新维修状态: faultId={}, repairStatus={}", id, dto.getRepairStatus());
        faultService.updateRepairStatus(id, dto);
        return Result.success();
    }

    /**
     * 获取故障统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getFaultStatistics() {
        log.info("查询故障统计");
        Map<String, Object> statistics = faultService.getFaultStatistics();
        return Result.success(statistics);
    }
}
