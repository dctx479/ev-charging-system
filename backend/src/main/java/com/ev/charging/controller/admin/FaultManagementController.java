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
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 故障管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/faults")
@CrossOrigin(origins = "*")
public class FaultManagementController {

    @Autowired
    private FaultService faultService;

    /**
     * 上报故障
     */
    @PostMapping
    public Result<Long> reportFault(@RequestBody @Valid FaultReportDTO dto) {
        log.info("上报故障: pileId={}, faultType={}", dto.getPileId(), dto.getFaultType());
        Long faultId = faultService.reportFault(dto);
        return Result.success(faultId);
    }

    /**
     * 获取故障列表（分页）
     */
    @GetMapping
    public Result<Page<FaultRecordVO>> getFaultList(FaultQueryDTO queryDTO) {
        log.info("查询故障列表: {}", queryDTO);
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
