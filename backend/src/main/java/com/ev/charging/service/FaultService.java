package com.ev.charging.service;

import com.ev.charging.dto.FaultQueryDTO;
import com.ev.charging.dto.FaultReportDTO;
import com.ev.charging.dto.RepairUpdateDTO;
import com.ev.charging.entity.ChargingPile;
import com.ev.charging.entity.ChargingStation;
import com.ev.charging.entity.FaultRecord;
import com.ev.charging.repository.ChargingPileRepository;
import com.ev.charging.repository.ChargingStationRepository;
import com.ev.charging.repository.FaultRecordRepository;
import com.ev.charging.vo.FaultRecordVO;
import com.ev.charging.websocket.PileStatusHandler;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 故障管理服务
 */
@Slf4j
@Service
public class FaultService {

    @Autowired
    private FaultRecordRepository faultRecordRepository;

    @Autowired
    private ChargingPileRepository chargingPileRepository;

    @Autowired
    private ChargingStationRepository chargingStationRepository;

    /**
     * 上报故障
     */
    @Transactional
    public Long reportFault(FaultReportDTO dto) {
        log.info("上报故障: pileId={}, faultType={}", dto.getPileId(), dto.getFaultType());

        // 创建故障记录
        FaultRecord faultRecord = FaultRecord.builder()
                .pileId(dto.getPileId())
                .stationId(dto.getStationId())
                .faultType(dto.getFaultType())
                .faultCode(dto.getFaultCode())
                .faultDescription(dto.getFaultDescription())
                .severity(dto.getSeverity())
                .reportSource(dto.getReportSource())
                .reporterId(dto.getReporterId())
                .faultTime(dto.getFaultTime() != null ? dto.getFaultTime() : LocalDateTime.now())
                .repairStatus((byte) 0) // 待维修
                .build();

        FaultRecord saved = faultRecordRepository.save(faultRecord);

        // 更新充电桩状态为故障
        ChargingPile pile = chargingPileRepository.findById(dto.getPileId())
                .orElseThrow(() -> new RuntimeException("充电桩不存在"));
        pile.setStatus((byte) 4); // 4-故障
        chargingPileRepository.save(pile);

        // 发送WebSocket通知
        try {
            String wsMessage = String.format(
                    "{\"type\":\"fault_alert\",\"pileId\":%d,\"pileNo\":\"%s\",\"severity\":%d,\"faultDescription\":\"%s\"}",
                    dto.getPileId(), pile.getPileNo(), dto.getSeverity(), dto.getFaultDescription()
            );
            PileStatusHandler.broadcast(wsMessage);
        } catch (Exception e) {
            log.error("WebSocket广播失败", e);
        }

        log.info("故障记录创建成功: id={}", saved.getId());
        return saved.getId();
    }

    /**
     * 获取故障列表（分页）
     */
    public Page<FaultRecordVO> getFaultList(FaultQueryDTO queryDTO) {
        Pageable pageable = PageRequest.of(
                queryDTO.getPage() - 1,
                queryDTO.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        Page<FaultRecord> page = faultRecordRepository.findByConditions(
                queryDTO.getStationId(),
                queryDTO.getRepairStatus(),
                queryDTO.getSeverity(),
                queryDTO.getFaultType(),
                pageable
        );

        return page.map(this::convertToVO);
    }

    /**
     * 根据ID获取故障详情
     */
    public FaultRecordVO getFaultById(Long id) {
        FaultRecord faultRecord = faultRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("故障记录不存在"));
        return convertToVO(faultRecord);
    }

    /**
     * 更新维修状态
     */
    @Transactional
    public void updateRepairStatus(Long faultId, RepairUpdateDTO dto) {
        log.info("更新维修状态: faultId={}, repairStatus={}", faultId, dto.getRepairStatus());

        FaultRecord faultRecord = faultRecordRepository.findById(faultId)
                .orElseThrow(() -> new RuntimeException("故障记录不存在"));

        faultRecord.setRepairStatus(dto.getRepairStatus());
        faultRecord.setRepairPerson(dto.getRepairPerson());
        faultRecord.setRepairStartTime(dto.getRepairStartTime());
        faultRecord.setRepairEndTime(dto.getRepairEndTime());
        faultRecord.setRepairCost(dto.getRepairCost());
        faultRecord.setRepairNote(dto.getRepairNote());

        faultRecordRepository.save(faultRecord);

        // 如果维修完成，更新充电桩状态为空闲
        if (dto.getRepairStatus() == 2) {
            ChargingPile pile = chargingPileRepository.findById(faultRecord.getPileId())
                    .orElseThrow(() -> new RuntimeException("充电桩不存在"));
            pile.setStatus((byte) 1); // 1-空闲
            pile.setHealthScore((byte) 100); // 重置健康度
            pile.setLastMaintenanceTime(LocalDateTime.now());
            chargingPileRepository.save(pile);
            log.info("充电桩状态已更新为空闲: pileId={}", pile.getId());
        }
    }

    /**
     * 获取故障统计
     */
    public Map<String, Object> getFaultStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        // 统计各状态故障数量
        List<Object[]> statusCount = faultRecordRepository.countByRepairStatus();
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : statusCount) {
            Byte status = (Byte) row[0];
            Long count = (Long) row[1];
            String statusText = getRepairStatusText(status);
            statusMap.put(statusText, count);
        }
        statistics.put("statusDistribution", statusMap);

        // 统计各类型故障数量
        List<Object[]> typeCount = faultRecordRepository.countByFaultType();
        Map<String, Long> typeMap = new HashMap<>();
        for (Object[] row : typeCount) {
            Byte type = (Byte) row[0];
            Long count = (Long) row[1];
            String typeText = getFaultTypeText(type);
            typeMap.put(typeText, count);
        }
        statistics.put("typeDistribution", typeMap);

        // 查询紧急待维修故障
        List<FaultRecord> urgentFaults = faultRecordRepository.findUrgentPendingFaults();
        statistics.put("urgentFaultCount", urgentFaults.size());
        statistics.put("urgentFaults", urgentFaults.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));

        return statistics;
    }

    /**
     * 转换为VO
     */
    private FaultRecordVO convertToVO(FaultRecord faultRecord) {
        FaultRecordVO vo = new FaultRecordVO();
        BeanUtils.copyProperties(faultRecord, vo);

        // 获取充电桩信息
        chargingPileRepository.findById(faultRecord.getPileId()).ifPresent(pile -> {
            vo.setPileNo(pile.getPileNo());
            vo.setPileName(pile.getPileName());
        });

        // 获取站点信息
        chargingStationRepository.findById(faultRecord.getStationId()).ifPresent(station -> {
            vo.setStationName(station.getName());
        });

        // 设置文本描述
        vo.setFaultTypeText(getFaultTypeText(faultRecord.getFaultType()));
        vo.setSeverityText(getSeverityText(faultRecord.getSeverity()));
        vo.setReportSourceText(getReportSourceText(faultRecord.getReportSource()));
        vo.setRepairStatusText(getRepairStatusText(faultRecord.getRepairStatus()));

        return vo;
    }

    /**
     * 获取故障类型文本
     */
    private String getFaultTypeText(Byte faultType) {
        return switch (faultType) {
            case 1 -> "通信故障";
            case 2 -> "硬件故障";
            case 3 -> "软件故障";
            case 4 -> "其他";
            default -> "未知";
        };
    }

    /**
     * 获取严重程度文本
     */
    private String getSeverityText(Byte severity) {
        return switch (severity) {
            case 1 -> "轻微";
            case 2 -> "一般";
            case 3 -> "严重";
            case 4 -> "紧急";
            default -> "未知";
        };
    }

    /**
     * 获取上报来源文本
     */
    private String getReportSourceText(Byte reportSource) {
        return switch (reportSource) {
            case 1 -> "系统自动";
            case 2 -> "用户上报";
            case 3 -> "运维发现";
            default -> "未知";
        };
    }

    /**
     * 获取维修状态文本
     */
    private String getRepairStatusText(Byte repairStatus) {
        return switch (repairStatus) {
            case 0 -> "待维修";
            case 1 -> "维修中";
            case 2 -> "已完成";
            default -> "未知";
        };
    }
}
