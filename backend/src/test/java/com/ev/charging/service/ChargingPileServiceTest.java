package com.ev.charging.service;

import com.ev.charging.entity.ChargingPile;
import com.ev.charging.entity.ChargingStation;
import com.ev.charging.repository.ChargingPileRepository;
import com.ev.charging.repository.ChargingStationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 充电桐服务层单元测试
 *
 * 测试范围：
 * - 根据ID获取充电桩（含缓存和负缓存）
 * - 根据充电站ID获取充电桩列表
 * - 获取可用充电桩
 * - 创建充电桩
 * - 更新充电桩状态
 * - 删除充电桩
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("充电桩服务测试")
class ChargingPileServiceTest {

    @Mock
    private ChargingPileRepository pileRepository;

    @Mock
    private ChargingStationService stationService;

    @Mock
    private ChargingStationRepository stationRepository;

    @Mock
    private CacheService cacheService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private ChargingPileService pileService;

    private ChargingPile pile1;
    private ChargingPile pile2;
    private ChargingPile pile3;
    private ChargingStation station;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        station = new ChargingStation();
        station.setId(1L);
        station.setName("中关村充电站");

        pile1 = new ChargingPile();
        pile1.setId(1L);
        pile1.setStationId(1L);
        pile1.setPileNo("PILE-001");
        pile1.setStatus((byte) 1); // 空闲
        pile1.setTotalChargeCount(100);
        pile1.setTotalChargeAmount(new BigDecimal("5000.00"));
        pile1.setHealthScore((byte) 95);
        pile1.setVoltage(380);
        pile1.setCurrent(new BigDecimal("32.00"));
        pile1.setCreateTime(LocalDateTime.now());
        pile1.setUpdateTime(LocalDateTime.now());

        pile2 = new ChargingPile();
        pile2.setId(2L);
        pile2.setStationId(1L);
        pile2.setPileNo("PILE-002");
        pile2.setStatus((byte) 2); // 充电中
        pile2.setTotalChargeCount(150);
        pile2.setTotalChargeAmount(new BigDecimal("8000.00"));
        pile2.setHealthScore((byte) 90);
        pile2.setVoltage(380);
        pile2.setCurrent(new BigDecimal("32.00"));
        pile2.setCreateTime(LocalDateTime.now());
        pile2.setUpdateTime(LocalDateTime.now());

        pile3 = new ChargingPile();
        pile3.setId(3L);
        pile3.setStationId(1L);
        pile3.setPileNo("PILE-003");
        pile3.setStatus((byte) 4); // 故障
        pile3.setTotalChargeCount(80);
        pile3.setTotalChargeAmount(new BigDecimal("4000.00"));
        pile3.setHealthScore((byte) 30);
        pile3.setCreateTime(LocalDateTime.now());
        pile3.setUpdateTime(LocalDateTime.now());
    }

    // ==================== 获取所有充电桐测试 ====================

    @Test
    @DisplayName("获取所有充电桩 - 返回非空列表")
    void testGetAllPilesSuccess() {
        // Arrange
        List<ChargingPile> piles = new ArrayList<>();
        piles.add(pile1);
        piles.add(pile2);
        piles.add(pile3);
        when(pileRepository.findAll()).thenReturn(piles);
        when(stationRepository.findAllById(any())).thenReturn(new ArrayList<>(List.of(station)));

        // Act
        List<ChargingPile> result = pileService.getAllPiles();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(pileRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("获取所有充电桩 - 返回空列表")
    void testGetAllPilesEmpty() {
        // Arrange
        when(pileRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<ChargingPile> result = pileService.getAllPiles();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(pileRepository, times(1)).findAll();
    }

    // ==================== 根据ID获取充电桐测试 ====================

    @Test
    @DisplayName("根据ID获取充电桩 - 缓存命中")
    void testGetPileByIdCachHit() {
        // Arrange
        when(cacheService.buildPileStatusKey(1L)).thenReturn("pile_status_1");
        ValueOperations<String, Object> opsForValue = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(opsForValue);
        when(opsForValue.get("pile_status_1")).thenReturn(pile1);

        // Act
        ChargingPile result = pileService.getPileById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(pileRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("根据ID获取充电桩 - 缓存未命中，从数据库查询")
    void testGetPileByIdCacheMiss() {
        // Arrange
        when(cacheService.buildPileStatusKey(1L)).thenReturn("pile_status_1");
        ValueOperations<String, Object> opsForValue = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(opsForValue);
        when(opsForValue.get("pile_status_1")).thenReturn(null);
        when(pileRepository.findById(1L)).thenReturn(Optional.of(pile1));

        // Act
        ChargingPile result = pileService.getPileById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(pileRepository, times(1)).findById(1L);
        verify(cacheService, times(1)).set(anyString(), eq(pile1), anyLong());
    }

    @Test
    @DisplayName("根据ID获取充电桩 - 不存在（负缓存）")
    void testGetPileByIdNotFoundNegativeCache() {
        // Arrange
        when(cacheService.buildPileStatusKey(999L)).thenReturn("pile_status_999");
        ValueOperations<String, Object> opsForValue = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(opsForValue);
        when(opsForValue.get("pile_status_999")).thenReturn(null);
        when(pileRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        ChargingPile result = pileService.getPileById(999L);

        // Assert
        assertNull(result);
        verify(pileRepository, times(1)).findById(999L);
        // 验证负缓存设置
        verify(opsForValue, times(1)).set(eq("pile_status_999"), eq("NULL"), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("根据ID获取充电桩 - 缓存中的负缓存标记")
    void testGetPileByIdNegativeCacheHit() {
        // Arrange
        when(cacheService.buildPileStatusKey(999L)).thenReturn("pile_status_999");
        ValueOperations<String, Object> opsForValue = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(opsForValue);
        when(opsForValue.get("pile_status_999")).thenReturn("NULL");

        // Act
        ChargingPile result = pileService.getPileById(999L);

        // Assert
        assertNull(result);
        verify(pileRepository, never()).findById(anyLong());
    }

    // ==================== 根据编号获取充电桐测试 ====================

    @Test
    @DisplayName("根据编号获取充电桩 - 成功")
    void testGetPileByNoSuccess() {
        // Arrange
        when(pileRepository.findByPileNo("PILE-001")).thenReturn(Optional.of(pile1));

        // Act
        ChargingPile result = pileService.getPileByNo("PILE-001");

        // Assert
        assertNotNull(result);
        assertEquals("PILE-001", result.getPileNo());
        verify(pileRepository, times(1)).findByPileNo("PILE-001");
    }

    @Test
    @DisplayName("根据编号获取充电桩 - 不存在")
    void testGetPileByNoNotFound() {
        // Arrange
        when(pileRepository.findByPileNo("PILE-999")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            pileService.getPileByNo("PILE-999");
        });

        verify(pileRepository, times(1)).findByPileNo("PILE-999");
    }

    // ==================== 根据充电站ID获取充电桐列表测试 ====================

    @Test
    @DisplayName("根据充电站ID获取充电桩列表 - 成功")
    void testGetPilesByStationIdSuccess() {
        // Arrange
        List<ChargingPile> piles = new ArrayList<>();
        piles.add(pile1);
        piles.add(pile2);
        piles.add(pile3);
        when(pileRepository.findByStationId(1L)).thenReturn(piles);
        when(stationRepository.findAllById(any())).thenReturn(new ArrayList<>(List.of(station)));

        // Act
        List<ChargingPile> result = pileService.getPilesByStationId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(pileRepository, times(1)).findByStationId(1L);
    }

    @Test
    @DisplayName("根据充电站ID获取充电桩列表 - 空列表")
    void testGetPilesByStationIdEmpty() {
        // Arrange
        when(pileRepository.findByStationId(2L)).thenReturn(new ArrayList<>());

        // Act
        List<ChargingPile> result = pileService.getPilesByStationId(2L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(pileRepository, times(1)).findByStationId(2L);
    }

    // ==================== 获取可用充电桩测试 ====================

    @Test
    @DisplayName("获取可用充电桩 - 成功")
    void testGetAvailablePilesSuccess() {
        // Arrange
        List<ChargingPile> availablePiles = new ArrayList<>();
        availablePiles.add(pile1); // 状态为1（空闲）
        when(pileRepository.findByStationIdAndStatus(1L, (byte) 1)).thenReturn(availablePiles);
        when(stationRepository.findAllById(any())).thenReturn(new ArrayList<>(List.of(station)));

        // Act
        List<ChargingPile> result = pileService.getAvailablePiles(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals((byte) 1, result.get(0).getStatus());
        verify(pileRepository, times(1)).findByStationIdAndStatus(1L, (byte) 1);
    }

    // ==================== 获取故障充电桩测试 ====================

    @Test
    @DisplayName("获取故障充电桩 - 成功")
    void testGetFaultPilesSuccess() {
        // Arrange
        List<ChargingPile> faultPiles = new ArrayList<>();
        faultPiles.add(pile3); // 状态为4（故障）
        when(pileRepository.findFaultPiles()).thenReturn(faultPiles);
        when(stationRepository.findAllById(any())).thenReturn(new ArrayList<>(List.of(station)));

        // Act
        List<ChargingPile> result = pileService.getFaultPiles();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals((byte) 4, result.get(0).getStatus());
        verify(pileRepository, times(1)).findFaultPiles();
    }

    // ==================== 创建充电桩测试 ====================

    @Test
    @DisplayName("创建充电桩 - 成功")
    void testCreatePileSuccess() {
        // Arrange
        ChargingPile newPile = new ChargingPile();
        newPile.setStationId(1L);
        newPile.setPileNo("PILE-004");
        newPile.setStatus((byte) 1);

        ChargingPile savedPile = new ChargingPile();
        savedPile.setId(4L);
        savedPile.setStationId(1L);
        savedPile.setPileNo("PILE-004");
        savedPile.setStatus((byte) 1);
        savedPile.setTotalChargeCount(0);
        savedPile.setTotalChargeAmount(new BigDecimal("0.00"));
        savedPile.setHealthScore((byte) 100);

        when(pileRepository.existsByPileNo("PILE-004")).thenReturn(false);
        when(stationService.getStationById(1L)).thenReturn(station);
        when(pileRepository.save(any(ChargingPile.class))).thenReturn(savedPile);

        // Act
        ChargingPile result = pileService.createPile(newPile);

        // Assert
        assertNotNull(result);
        assertEquals(4L, result.getId());
        assertEquals((byte) 1, result.getStatus());
        assertEquals(0, result.getTotalChargeCount());
        assertEquals(new BigDecimal("0.00"), result.getTotalChargeAmount());
        verify(pileRepository, times(1)).existsByPileNo("PILE-004");
        verify(stationService, times(1)).getStationById(1L);
        verify(pileRepository, times(1)).save(any(ChargingPile.class));
        verify(stationService, times(1)).updateStationPileCount(1L);
    }

    @Test
    @DisplayName("创建充电桩 - 编号已存在")
    void testCreatePileDuplicatePileNo() {
        // Arrange
        ChargingPile newPile = new ChargingPile();
        newPile.setStationId(1L);
        newPile.setPileNo("PILE-001"); // 已存在

        when(pileRepository.existsByPileNo("PILE-001")).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            pileService.createPile(newPile);
        });

        verify(pileRepository, times(1)).existsByPileNo("PILE-001");
        verify(pileRepository, never()).save(any(ChargingPile.class));
    }

    @Test
    @DisplayName("创建充电桩 - 充电站不存在")
    void testCreatePileStationNotFound() {
        // Arrange
        ChargingPile newPile = new ChargingPile();
        newPile.setStationId(999L);
        newPile.setPileNo("PILE-999");

        when(pileRepository.existsByPileNo("PILE-999")).thenReturn(false);
        when(stationService.getStationById(999L))
                .thenThrow(new IllegalArgumentException("充电站不存在"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            pileService.createPile(newPile);
        });

        verify(pileRepository, never()).save(any(ChargingPile.class));
    }

    // ==================== 更新充电桩状态测试 ====================

    @Test
    @DisplayName("更新充电桩状态 - 成功")
    void testUpdatePileStatusSuccess() {
        // Arrange
        when(cacheService.buildPileStatusKey(1L)).thenReturn("pile_status_1");
        ValueOperations<String, Object> opsForValue = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(opsForValue);
        when(opsForValue.get("pile_status_1")).thenReturn(pile1);
        when(pileRepository.save(any(ChargingPile.class))).thenReturn(pile1);

        // Act
        ChargingPile result = pileService.updatePileStatus(1L, (byte) 2); // 更新为充电中

        // Assert
        assertNotNull(result);
        verify(pileRepository, times(1)).save(any(ChargingPile.class));
        verify(cacheService, times(1)).evictPileCache(1L);
        verify(stationService, times(1)).updateStationPileCount(1L);
    }

    @Test
    @DisplayName("更新充电桩状态 - 不存在")
    void testUpdatePileStatusNotFound() {
        // Arrange
        when(cacheService.buildPileStatusKey(999L)).thenReturn("pile_status_999");
        ValueOperations<String, Object> opsForValue = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(opsForValue);
        when(opsForValue.get("pile_status_999")).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            pileService.updatePileStatus(999L, (byte) 2);
        });

        verify(pileRepository, never()).save(any(ChargingPile.class));
    }

    // ==================== 更新充电桩实时数据测试 ====================

    @Test
    @DisplayName("更新充电桩实时数据 - 成功")
    void testUpdatePileRealTimeDataSuccess() {
        // Arrange
        when(cacheService.buildPileStatusKey(1L)).thenReturn("pile_status_1");
        ValueOperations<String, Object> opsForValue = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(opsForValue);
        when(opsForValue.get("pile_status_1")).thenReturn(pile1);
        when(pileRepository.save(any(ChargingPile.class))).thenReturn(pile1);

        // Act
        ChargingPile result = pileService.updatePileRealTimeData(1L, 400, new BigDecimal("40.00"));

        // Assert
        assertNotNull(result);
        verify(pileRepository, times(1)).save(any(ChargingPile.class));
        verify(cacheService, times(1)).evictPileCache(1L);
    }

    // ==================== 记录充电桩维护测试 ====================

    @Test
    @DisplayName("记录充电桩维护 - 成功")
    void testRecordMaintenanceSuccess() {
        // Arrange
        when(cacheService.buildPileStatusKey(1L)).thenReturn("pile_status_1");
        ValueOperations<String, Object> opsForValue = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(opsForValue);
        when(opsForValue.get("pile_status_1")).thenReturn(pile1);
        when(pileRepository.save(any(ChargingPile.class))).thenReturn(pile1);

        // Act
        ChargingPile result = pileService.recordMaintenance(1L);

        // Assert
        assertNotNull(result);
        verify(pileRepository, times(1)).save(any(ChargingPile.class));
        verify(cacheService, times(1)).evictPileCache(1L);
        verify(stationService, times(1)).updateStationPileCount(1L);
    }

    // ==================== 删除充电桩测试 ====================

    @Test
    @DisplayName("删除充电桩 - 成功")
    void testDeletePileSuccess() {
        // Arrange
        when(cacheService.buildPileStatusKey(1L)).thenReturn("pile_status_1");
        ValueOperations<String, Object> opsForValue = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(opsForValue);
        when(opsForValue.get("pile_status_1")).thenReturn(pile1);

        // Act
        pileService.deletePile(1L);

        // Assert
        verify(pileRepository, times(1)).deleteById(1L);
        verify(cacheService, times(1)).evictPileCache(1L);
        verify(stationService, times(1)).updateStationPileCount(1L);
    }

    @Test
    @DisplayName("删除充电桩 - 不存在")
    void testDeletePileNotFound() {
        // Arrange
        when(cacheService.buildPileStatusKey(999L)).thenReturn("pile_status_999");
        ValueOperations<String, Object> opsForValue = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(opsForValue);
        when(opsForValue.get("pile_status_999")).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            pileService.deletePile(999L);
        });

        verify(pileRepository, never()).deleteById(anyLong());
    }
}
