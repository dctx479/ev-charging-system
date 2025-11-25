package com.ev.charging.service;

import com.ev.charging.dto.MaintenancePlanDTO;
import com.ev.charging.entity.MaintenancePlan;
import com.ev.charging.repository.MaintenancePlanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 维护计划服务
 */
@Slf4j
@Service
public class MaintenanceService {

    @Autowired
    private MaintenancePlanRepository maintenancePlanRepository;

    /**
     * 创建维护计划
     */
    @Transactional
    public Long createMaintenancePlan(MaintenancePlanDTO dto) {
        log.info("创建维护计划: pileId={}, planType={}", dto.getPileId(), dto.getPlanType());

        MaintenancePlan plan = MaintenancePlan.builder()
                .pileId(dto.getPileId())
                .stationId(dto.getStationId())
                .planType(dto.getPlanType())
                .predictedFaultProbability(dto.getPredictedFaultProbability())
                .maintenanceContent(dto.getMaintenanceContent())
                .plannedTime(dto.getPlannedTime())
                .maintenanceStatus((byte) 0) // 待执行
                .assignedPerson(dto.getAssignedPerson())
                .build();

        MaintenancePlan saved = maintenancePlanRepository.save(plan);
        log.info("维护计划创建成功: id={}", saved.getId());
        return saved.getId();
    }

    /**
     * 获取维护计划列表
     */
    public Page<MaintenancePlan> getMaintenancePlanList(Long stationId, Byte maintenanceStatus,
                                                         Byte planType, Integer page, Integer pageSize) {
        Pageable pageable = PageRequest.of(
                page - 1,
                pageSize,
                Sort.by(Sort.Direction.DESC, "plannedTime")
        );

        return maintenancePlanRepository.findByConditions(stationId, maintenanceStatus, planType, pageable);
    }

    /**
     * 更新维护计划状态
     */
    @Transactional
    public void updateMaintenanceStatus(Long planId, Byte status, String note) {
        log.info("更新维护计划状态: planId={}, status={}", planId, status);

        MaintenancePlan plan = maintenancePlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("维护计划不存在"));

        plan.setMaintenanceStatus(status);
        plan.setMaintenanceNote(note);

        if (status == 1) { // 执行中
            plan.setActualStartTime(LocalDateTime.now());
        } else if (status == 2) { // 已完成
            plan.setActualEndTime(LocalDateTime.now());
        }

        maintenancePlanRepository.save(plan);
    }

    /**
     * 获取即将到期的维护计划
     */
    public List<MaintenancePlan> getUpcomingPlans() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusHours(24);
        return maintenancePlanRepository.findUpcomingPlans(now, tomorrow);
    }

    /**
     * 根据充电桩ID获取维护历史
     */
    public List<MaintenancePlan> getMaintenanceHistoryByPileId(Long pileId) {
        return maintenancePlanRepository.findByPileIdOrderByPlannedTimeDesc(pileId);
    }
}
