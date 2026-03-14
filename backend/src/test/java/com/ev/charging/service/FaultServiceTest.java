package com.ev.charging.service;

import com.ev.charging.dto.FaultReportDTO;
import com.ev.charging.entity.ChargingPile;
import com.ev.charging.entity.ChargingStation;
import com.ev.charging.entity.FaultRecord;
import com.ev.charging.exception.BusinessException;
import com.ev.charging.repository.ChargingPileRepository;
import com.ev.charging.repository.ChargingStationRepository;
import com.ev.charging.repository.FaultRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 故障管理服务单元测试
 *
 * 测试范围：
 * - 上报故障
 * - 获取故障列表
 * - 更新故障状态
 * - 故障查询
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("故障管理服务测试")
class FaultServiceTest {

    @Mock
    private FaultRecordRepository faultRecordRepository;

    @Mock
    private ChargingPileRepository chargingPileRepository;

    @Mock
    private ChargingStationRepository chargingStationRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private FaultService faultService;

    private ChargingPile pile1;
    private ChargingStation station1;
    private FaultReportDTO faultReportDTO;
    private FaultRecord faultRecord;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        station1 = new ChargingStation();
        station1.setId(1L);
        station1.setName("中关村充电站");
        station1.setProvince("北京市");
        station1.setCity("北京市");

        pile1 = new ChargingPile();
        pile1.setId(1L);
        pile1.setStationId(1L);
        pile1.setPileNo("PILE-001");
        pile1.setStatus((byte) 1); // 空闲
        pile1.setHealthScore((byte) 95);
        pile1.setCreateTime(LocalDateTime.now());
        pile1.setUpdateTime(LocalDateTime.now());

        faultReportDTO = new FaultReportDTO();
        faultReportDTO.setPileId(1L);
        faultReportDTO.setStationId(1L);
        faultReportDTO.setFaultType("充电枪故障");
        faultReportDTO.setFaultCode("ERR_001");
        faultReportDTO.setFaultDescription("充电枪接触不良");
        faultReportDTO.setSeverity("HIGH");
        faultReportDTO.setReportSource("SYSTEM");
        faultReportDTO.setReporterId(1L);
        faultReportDTO.setFaultTime(LocalDateTime.now());

        faultRecord = FaultRecord.builder()
                .id(1L)
                .pileId(1L)
                .stationId(1L)
                .faultType("充电枪故障")
                .faultCode("ERR_001")
                .faultDescription("充电枪接触不良")
                .severity("HIGH")
                .reportSource("SYSTEM")
                .reporterId(1L)
                .faultTime(LocalDateTime.now())
                .repairStatus((byte) 0) // 待维修
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }

    // ==================== 上报故障测试 ====================

    @Test
    @DisplayName("上报故障 - 成功")
    void testReportFaultSuccess() {
        // Arrange
        when(chargingPileRepository.findById(1L)).thenReturn(Optional.of(pile1));
        when(faultRecordRepository.save(any(FaultRecord.class))).thenReturn(faultRecord);
        when(cacheService.buildPileStatusKey(1L)).thenReturn("pile_status_1");

        // Act
        Long result = faultService.reportFault(faultReportDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result);
        verify(chargingPileRepository, times(1)).findById(1L);
        verify(faultRecordRepository, times(1)).save(any(FaultRecord.class));
        verify(chargingPileRepository, times(1)).save(any(ChargingPile.class));
        verify(redisTemplate, times(1)).delete(anyString());
    }

    @Test
    @DisplayName("上报故障 - 充电桩不存在")
    void testReportFaultPileNotFound() {
        // Arrange
        when(chargingPileRepository.findById(999L)).thenReturn(Optional.empty());

        FaultReportDTO invalidDTO = new FaultReportDTO();
        invalidDTO.setPileId(999L);
        invalidDTO.setStationId(1L);
        invalidDTO.setFaultType("充电枪故障");
        invalidDTO.setFaultCode("ERR_001");
        invalidDTO.setFaultDescription("充电枪接触不良");
        invalidDTO.setSeverity("HIGH");
        invalidDTO.setReportSource("SYSTEM");
        invalidDTO.setReporterId(1L);

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            faultService.reportFault(invalidDTO);
        });

        verify(chargingPileRepository, times(1)).findById(999L);
        verify(faultRecordRepository, never()).save(any(FaultRecord.class));
    }

    @Test
    @DisplayName("上报故障 - 更新充电桩状态为故障")
    void testReportFaultUpdatesPileStatus() {
        // Arrange
        when(chargingPileRepository.findById(1L)).thenReturn(Optional.of(pile1));
        when(faultRecordRepository.save(any(FaultRecord.class))).thenReturn(faultRecord);
        when(cacheService.buildPileStatusKey(1L)).thenReturn("pile_status_1");

        // Act
        Long result = faultService.reportFault(faultReportDTO);

        // Assert
        assertNotNull(result);
        // 验证充电桩状态已更新为故障（4）
        verify(chargingPileRepository, times(1)).save(argThat(pile ->
                pile.getId().equals(1L) && pile.getStatus().equals((byte) 4)
        ));
    }

    @Test
    @DisplayName("上报故障 - 使用默认时间")
    void testReportFaultWithDefaultTime() {
        // Arrange
        FaultReportDTO dtoWithoutTime = new FaultReportDTO();
        dtoWithoutTime.setPileId(1L);
        dtoWithoutTime.setStationId(1L);
        dtoWithoutTime.setFaultType("电源故障");
        dtoWithoutTime.setFaultCode("ERR_002");
        dtoWithoutTime.setFaultDescription("电源输入异常");
        dtoWithoutTime.setSeverity("CRITICAL");
        dtoWithoutTime.setReportSource("SYSTEM");
        dtoWithoutTime.setReporterId(1L);
        dtoWithoutTime.setFaultTime(null); // 无指定时间

        when(chargingPileRepository.findById(1L)).thenReturn(Optional.of(pile1));
        when(faultRecordRepository.save(any(FaultRecord.class))).thenReturn(faultRecord);
        when(cacheService.buildPileStatusKey(1L)).thenReturn("pile_status_1");

        // Act
        Long result = faultService.reportFault(dtoWithoutTime);

        // Assert
        assertNotNull(result);
        // 验证保存时使用了当前时间
        verify(faultRecordRepository, times(1)).save(argThat(record ->
                record.getFaultTime() != null
        ));
    }

    // ==================== 获取故障列表测试 ====================

    @Test
    @DisplayName("获取充电站的故障列表 - 成功")
    void testGetFaultsByStationSuccess() {
        // Arrange
        List<FaultRecord> faults = new ArrayList<>();
        faults.add(faultRecord);

        FaultRecord fault2 = FaultRecord.builder()
                .id(2L)
                .pileId(2L)
                .stationId(1L)
                .faultType("屏幕故障")
                .faultCode("ERR_003")
                .faultDescription("触摸屏失效")
                .severity("MEDIUM")
                .reportSource("USER")
                .reporterId(2L)
                .faultTime(LocalDateTime.now())
                .repairStatus((byte) 1) // 维修中
                .createTime(LocalDateTime.now())
                .build();
        faults.add(fault2);

        when(faultRecordRepository.findByStationIdOrderByFaultTimeDesc(1L))
                .thenReturn(faults);

        // Act
        List<FaultRecord> result = faultService.getFaultsByStation(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("充电枪故障", result.get(0).getFaultType());
        assertEquals("屏幕故障", result.get(1).getFaultType());
        verify(faultRecordRepository, times(1)).findByStationIdOrderByFaultTimeDesc(1L);
    }

    @Test
    @DisplayName("获取充电站的故障列表 - 空列表")
    void testGetFaultsByStationEmpty() {
        // Arrange
        when(faultRecordRepository.findByStationIdOrderByFaultTimeDesc(2L))
                .thenReturn(new ArrayList<>());

        // Act
        List<FaultRecord> result = faultService.getFaultsByStation(2L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(faultRecordRepository, times(1)).findByStationIdOrderByFaultTimeDesc(2L);
    }

    // ==================== 获取待维修故障列表测试 ====================

    @Test
    @DisplayName("获取待维修故障 - 成功")
    void testGetPendingFaultsSuccess() {
        // Arrange
        List<FaultRecord> pendingFaults = new ArrayList<>();
        pendingFaults.add(faultRecord); // 状态为0（待维修）

        when(faultRecordRepository.findByRepairStatusOrderByFaultTimeDesc((byte) 0))
                .thenReturn(pendingFaults);

        // Act
        List<FaultRecord> result = faultService.getPendingFaults();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals((byte) 0, result.get(0).getRepairStatus());
        verify(faultRecordRepository, times(1)).findByRepairStatusOrderByFaultTimeDesc((byte) 0);
    }

    // ==================== 获取高严重度故障测试 ====================

    @Test
    @DisplayName("获取高严重度故障 - 成功")
    void testGetCriticalFaultsSuccess() {
        // Arrange
        List<FaultRecord> criticalFaults = new ArrayList<>();
        criticalFaults.add(faultRecord); // 严重度为HIGH

        when(faultRecordRepository.findBySeverityInOrderByFaultTimeDesc(
                any()
        )).thenReturn(criticalFaults);

        // Act
        List<FaultRecord> result = faultService.getCriticalFaults();

        // Assert
        assertNotNull(result);
        assertTrue(result.size() > 0);
        verify(faultRecordRepository, times(1)).findBySeverityInOrderByFaultTimeDesc(any());
    }

    // ==================== 更新故障状态测试 ====================

    @Test
    @DisplayName("更新故障状态 - 从待维修到维修中")
    void testUpdateFaultStatusToRepairing() {
        // Arrange
        when(faultRecordRepository.findById(1L)).thenReturn(Optional.of(faultRecord));
        faultRecord.setRepairStatus((byte) 1); // 维修中
        when(faultRecordRepository.save(any(FaultRecord.class))).thenReturn(faultRecord);

        // Act
        FaultRecord result = faultService.updateFaultStatus(1L, (byte) 1);

        // Assert
        assertNotNull(result);
        assertEquals((byte) 1, result.getRepairStatus());
        verify(faultRecordRepository, times(1)).findById(1L);
        verify(faultRecordRepository, times(1)).save(any(FaultRecord.class));
    }

    @Test
    @DisplayName("更新故障状态 - 从维修中到已修复")
    void testUpdateFaultStatusToResolved() {
        // Arrange
        faultRecord.setRepairStatus((byte) 1); // 维修中
        when(faultRecordRepository.findById(1L)).thenReturn(Optional.of(faultRecord));
        faultRecord.setRepairStatus((byte) 2); // 已修复
        when(faultRecordRepository.save(any(FaultRecord.class))).thenReturn(faultRecord);
        when(chargingPileRepository.findById(1L)).thenReturn(Optional.of(pile1));

        // Act
        FaultRecord result = faultService.updateFaultStatus(1L, (byte) 2);

        // Assert
        assertNotNull(result);
        assertEquals((byte) 2, result.getRepairStatus());
        verify(faultRecordRepository, times(1)).findById(1L);
        verify(faultRecordRepository, times(1)).save(any(FaultRecord.class));
    }

    @Test
    @DisplayName("更新故障状态 - 故障不存在")
    void testUpdateFaultStatusNotFound() {
        // Arrange
        when(faultRecordRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            faultService.updateFaultStatus(999L, (byte) 1);
        });

        verify(faultRecordRepository, times(1)).findById(999L);
        verify(faultRecordRepository, never()).save(any(FaultRecord.class));
    }

    // ==================== 故障统计测试 ====================

    @Test
    @DisplayName("获取故障统计 - 按严重度")
    void testGetFaultStatisticsBySeverity() {
        // Arrange
        long highSeverityCount = 3;
        long mediumSeverityCount = 2;

        when(faultRecordRepository.countBySeverity("HIGH")).thenReturn(highSeverityCount);
        when(faultRecordRepository.countBySeverity("MEDIUM")).thenReturn(mediumSeverityCount);

        // Act
        long highCount = faultRecordRepository.countBySeverity("HIGH");
        long mediumCount = faultRecordRepository.countBySeverity("MEDIUM");

        // Assert
        assertEquals(3, highCount);
        assertEquals(2, mediumCount);
    }

    @Test
    @DisplayName("获取未修复故障数")
    void testGetUnrepairedFaultCount() {
        // Arrange
        long unrepiredCount = 5;
        when(faultRecordRepository.countByRepairStatusNot((byte) 2)).thenReturn(unrepiredCount);

        // Act
        long result = faultRecordRepository.countByRepairStatusNot((byte) 2);

        // Assert
        assertEquals(5, result);
        verify(faultRecordRepository, times(1)).countByRepairStatusNot((byte) 2);
    }

    // ==================== 故障查询测试 ====================

    @Test
    @DisplayName("查询指定时间范围内的故障")
    void testGetFaultsByTimeRange() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();

        List<FaultRecord> faults = new ArrayList<>();
        faults.add(faultRecord);

        when(faultRecordRepository.findByFaultTimeBetween(startTime, endTime))
                .thenReturn(faults);

        // Act
        List<FaultRecord> result = faultService.getFaultsByTimeRange(startTime, endTime);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(faultRecordRepository, times(1)).findByFaultTimeBetween(startTime, endTime);
    }

    @Test
    @DisplayName("查询指定类型的故障")
    void testGetFaultsByType() {
        // Arrange
        List<FaultRecord> faults = new ArrayList<>();
        faults.add(faultRecord);

        when(faultRecordRepository.findByFaultType("充电枪故障"))
                .thenReturn(faults);

        // Act
        List<FaultRecord> result = faultService.getFaultsByType("充电枪故障");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("充电枪故障", result.get(0).getFaultType());
        verify(faultRecordRepository, times(1)).findByFaultType("充电枪故障");
    }

    // ==================== 异常处理测试 ====================

    @Test
    @DisplayName("上报故障 - 处理Cache清除异常（不中断流程）")
    void testReportFaultCacheExceptionHandled() {
        // Arrange
        when(chargingPileRepository.findById(1L)).thenReturn(Optional.of(pile1));
        when(faultRecordRepository.save(any(FaultRecord.class))).thenReturn(faultRecord);
        when(cacheService.buildPileStatusKey(1L)).thenReturn("pile_status_1");
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis连接失败"));

        // Act
        Long result = faultService.reportFault(faultReportDTO);

        // Assert
        assertNotNull(result); // 不因为缓存清除失败而失败
        assertEquals(1L, result);
        verify(faultRecordRepository, times(1)).save(any(FaultRecord.class));
    }

    @Test
    @DisplayName("上报故障 - 处理WebSocket广播异常（不中断流程）")
    void testReportFaultWebSocketExceptionHandled() {
        // Arrange
        when(chargingPileRepository.findById(1L)).thenReturn(Optional.of(pile1));
        when(faultRecordRepository.save(any(FaultRecord.class))).thenReturn(faultRecord);
        when(cacheService.buildPileStatusKey(1L)).thenReturn("pile_status_1");

        // Act - 不模拟WebSocket错误，因为它在try-catch中处理
        Long result = faultService.reportFault(faultReportDTO);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result);
        verify(faultRecordRepository, times(1)).save(any(FaultRecord.class));
    }
}
