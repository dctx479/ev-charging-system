package com.ev.charging.service;

import com.ev.charging.dto.FaultQueryDTO;
import com.ev.charging.dto.FaultReportDTO;
import com.ev.charging.dto.RepairUpdateDTO;
import com.ev.charging.entity.ChargingPile;
import com.ev.charging.entity.ChargingStation;
import com.ev.charging.entity.FaultRecord;
import com.ev.charging.exception.BusinessException;
import com.ev.charging.repository.ChargingPileRepository;
import com.ev.charging.repository.ChargingStationRepository;
import com.ev.charging.repository.FaultRecordRepository;
import com.ev.charging.vo.FaultRecordVO;
import com.ev.charging.websocket.PileStatusHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
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
@RequiredArgsConstructor
public class FaultService {

    private final FaultRecordRepository faultRecordRepository;
    private final ChargingPileRepository chargingPileRepository;
    private final ChargingStationRepository chargingStationRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheService cacheService;

    /**
     * 上报故障
     */
    @Transactional(rollbackFor = Exception.class)
    public Long reportFault(FaultReportDTO dto) {
        log.info("上报故障: pileId={}, faultType={}", dto.getPileId(), dto.getFaultType());

        // 先验证充电桩存在性，再创建故障记录
        ChargingPile pile = chargingPileRepository.findById(dto.getPileId())
                .orElseThrow(() -> new BusinessException("充电桩不存在"));

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
        pile.setStatus((byte) 4); // 4-故障
        chargingPileRepository.save(pile);

        // 清除Redis缓存（修复缓存一致性）
        String cacheKey = cacheService.buildPileStatusKey(dto.getPileId());
        try {
            redisTemplate.delete(cacheKey);
            log.debug("已清除充电桩缓存: {}", cacheKey);
        } catch (Exception e) {
            log.warn("清除缓存失败: {}", e.getMessage());
        }

        // 发送WebSocket通知（使用ObjectMapper安全序列化，防止JSON注入）
        try {
            Map<String, Object> wsData = new HashMap<>();
            wsData.put("type", "fault_alert");
            wsData.put("pileId", dto.getPileId());
            wsData.put("pileNo", pile.getPileNo());
            wsData.put("severity", dto.getSeverity());
            wsData.put("faultDescription", dto.getFaultDescription());
            String wsMessage = new ObjectMapper().writeValueAsString(wsData);
            PileStatusHandler.broadcast(wsMessage);
        } catch (Exception e) {
            log.error("WebSocket广播失败", e);
        }

        log.info("故障记录创建成功: id={}", saved.getId());
        return saved.getId();
    }

    /**
     * 获取故障列表（分页）
     * 优化：批量查询关联数据，避免N+1查询
     */
    public Page<FaultRecordVO> getFaultList(FaultQueryDTO queryDTO) {
        Pageable pageable = PageRequest.of(
                queryDTO.getPage(),
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

        return convertToVOBatch(page);
    }

    /**
     * 根据ID获取故障详情
     * 优化：批量查询关联数据
     */
    public FaultRecordVO getFaultById(Long id) {
        FaultRecord faultRecord = faultRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("故障记录不存在"));

        // 批量查询关联数据
        ChargingPile pile = chargingPileRepository.findById(faultRecord.getPileId()).orElse(null);
        ChargingStation station = chargingStationRepository.findById(faultRecord.getStationId()).orElse(null);

        return convertToVO(faultRecord, pile, station);
    }

    /**
     * 更新维修状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRepairStatus(Long faultId, RepairUpdateDTO dto) {
        log.info("更新维修状态: faultId={}, repairStatus={}", faultId, dto.getRepairStatus());

        FaultRecord faultRecord = faultRecordRepository.findById(faultId)
                .orElseThrow(() -> new BusinessException("故障记录不存在"));

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
                    .orElseThrow(() -> new BusinessException("充电桩不存在"));
            pile.setStatus((byte) 1); // 1-空闲
            pile.setHealthScore((byte) 100); // 重置健康度
            pile.setLastMaintenanceTime(LocalDateTime.now());
            chargingPileRepository.save(pile);

            // 清除Redis缓存（修复缓存一致性）
            String cacheKey = cacheService.buildPileStatusKey(pile.getId());
            try {
                redisTemplate.delete(cacheKey);
                log.debug("已清除充电桩缓存: {}", cacheKey);
            } catch (Exception e) {
                log.warn("清除缓存失败: {}", e.getMessage());
            }

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

        // 查询紧急待维修故障（优化：批量查询关联数据）
        List<FaultRecord> urgentFaults = faultRecordRepository.findUrgentPendingFaults();
        statistics.put("urgentFaultCount", urgentFaults.size());

        if (!urgentFaults.isEmpty()) {
            // 批量查询关联数据
            java.util.Set<Long> pileIds = urgentFaults.stream()
                    .map(FaultRecord::getPileId)
                    .collect(java.util.stream.Collectors.toSet());

            java.util.Set<Long> stationIds = urgentFaults.stream()
                    .map(FaultRecord::getStationId)
                    .collect(java.util.stream.Collectors.toSet());

            java.util.Map<Long, ChargingPile> pileMap = chargingPileRepository.findAllById(pileIds).stream()
                    .collect(java.util.stream.Collectors.toMap(ChargingPile::getId, p -> p));

            java.util.Map<Long, ChargingStation> stationMap = chargingStationRepository.findAllById(stationIds).stream()
                    .collect(java.util.stream.Collectors.toMap(ChargingStation::getId, s -> s));

            statistics.put("urgentFaults", urgentFaults.stream()
                    .map(f -> convertToVO(f, pileMap.get(f.getPileId()), stationMap.get(f.getStationId())))
                    .collect(Collectors.toList()));
        } else {
            statistics.put("urgentFaults", List.of());
        }

        return statistics;
    }

    /**
     * 批量转换为VO（优化N+1查询）
     * 用于故障列表查询，一次性批量加载所有关联数据
     */
    private Page<FaultRecordVO> convertToVOBatch(Page<FaultRecord> faultPage) {
        if (faultPage.isEmpty()) {
            return Page.empty();
        }

        // 1. 收集所有pileId和stationId
        java.util.Set<Long> pileIds = faultPage.getContent().stream()
                .map(FaultRecord::getPileId)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Set<Long> stationIds = faultPage.getContent().stream()
                .map(FaultRecord::getStationId)
                .collect(java.util.stream.Collectors.toSet());

        // 2. 批量查询所有关联的pile和station（只执行2次查询，而不是N次）
        java.util.Map<Long, ChargingPile> pileMap = chargingPileRepository.findAllById(pileIds).stream()
                .collect(java.util.stream.Collectors.toMap(ChargingPile::getId, p -> p));

        java.util.Map<Long, ChargingStation> stationMap = chargingStationRepository.findAllById(stationIds).stream()
                .collect(java.util.stream.Collectors.toMap(ChargingStation::getId, s -> s));

        // 3. 转换为VO
        return faultPage.map(faultRecord -> {
            ChargingPile pile = pileMap.get(faultRecord.getPileId());
            ChargingStation station = stationMap.get(faultRecord.getStationId());
            return convertToVO(faultRecord, pile, station);
        });
    }

    /**
     * 转换为VO（带关联数据）
     * 用于单个故障查询，避免N+1问题
     */
    private FaultRecordVO convertToVO(FaultRecord faultRecord, ChargingPile pile, ChargingStation station) {
        FaultRecordVO vo = new FaultRecordVO();
        BeanUtils.copyProperties(faultRecord, vo);

        // 设置充电桩信息
        if (pile != null) {
            vo.setPileNo(pile.getPileNo());
            vo.setPileName(pile.getPileName());
        }

        // 设置站点信息
        if (station != null) {
            vo.setStationName(station.getName());
        }

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
        if (faultType == null) return "未知";
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
        if (severity == null) return "未知";
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
        if (reportSource == null) return "未知";
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
        if (repairStatus == null) return "未知";
        return switch (repairStatus) {
            case 0 -> "待维修";
            case 1 -> "维修中";
            case 2 -> "已完成";
            default -> "未知";
        };
    }
}
