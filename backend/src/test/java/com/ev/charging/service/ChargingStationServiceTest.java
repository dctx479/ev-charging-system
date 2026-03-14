package com.ev.charging.service;

import com.ev.charging.entity.ChargingStation;
import com.ev.charging.repository.ChargingPileRepository;
import com.ev.charging.repository.ChargingStationRepository;
import com.ev.charging.util.DistanceUtil;
import com.ev.charging.vo.StationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 充电站服务层单元测试
 *
 * 测试范围：
 * - 获取所有充电站
 * - 根据ID获取充电站
 * - 获取附近的充电站（距离计算）
 * - 创建充电站
 * - 更新充电站
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("充电站服务测试")
class ChargingStationServiceTest {

    @Mock
    private ChargingStationRepository stationRepository;

    @Mock
    private ChargingPileRepository pileRepository;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private ChargingStationService stationService;

    private ChargingStation station1;
    private ChargingStation station2;
    private ChargingStation station3;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        station1 = new ChargingStation();
        station1.setId(1L);
        station1.setOperatorId(100L);
        station1.setName("中关村充电站");
        station1.setProvince("北京市");
        station1.setCity("北京市");
        station1.setAddress("中关村大街1号");
        station1.setLatitude(new BigDecimal("39.9045"));
        station1.setLongitude(new BigDecimal("116.4073"));
        station1.setStatus((byte) 1); // ACTIVE
        station1.setTotalPiles(10);
        station1.setAvailablePiles(3);
        station1.setRating(4.8);
        station1.setReviewCount(100);
        station1.setCreateTime(LocalDateTime.now());
        station1.setUpdateTime(LocalDateTime.now());

        station2 = new ChargingStation();
        station2.setId(2L);
        station2.setOperatorId(101L);
        station2.setName("望京充电站");
        station2.setProvince("北京市");
        station2.setCity("北京市");
        station2.setAddress("望京街道");
        station2.setLatitude(new BigDecimal("39.9852"));
        station2.setLongitude(new BigDecimal("116.5500"));
        station2.setStatus((byte) 1); // ACTIVE
        station2.setTotalPiles(15);
        station2.setAvailablePiles(5);
        station2.setRating(4.5);
        station2.setReviewCount(85);
        station2.setCreateTime(LocalDateTime.now());
        station2.setUpdateTime(LocalDateTime.now());

        station3 = new ChargingStation();
        station3.setId(3L);
        station3.setOperatorId(102L);
        station3.setName("朝阳公园充电站");
        station3.setProvince("北京市");
        station3.setCity("北京市");
        station3.setAddress("朝阳公园内");
        station3.setLatitude(new BigDecimal("39.9250"));
        station3.setLongitude(new BigDecimal("116.6700"));
        station3.setStatus((byte) 2); // MAINTENANCE
        station3.setTotalPiles(8);
        station3.setAvailablePiles(0);
        station3.setRating(4.2);
        station3.setReviewCount(60);
        station3.setCreateTime(LocalDateTime.now());
        station3.setUpdateTime(LocalDateTime.now());
    }

    // ==================== 获取所有充电站测试 ====================

    @Test
    @DisplayName("获取所有充电站 - 返回非空列表")
    void testGetAllStationsSuccess() {
        // Arrange
        List<ChargingStation> stations = new ArrayList<>();
        stations.add(station1);
        stations.add(station2);
        stations.add(station3);
        when(stationRepository.findAll()).thenReturn(stations);

        // Act
        List<ChargingStation> result = stationService.getAllStations();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("中关村充电站", result.get(0).getName());
        verify(stationRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("获取所有充电站 - 返回空列表")
    void testGetAllStationsEmpty() {
        // Arrange
        when(stationRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<ChargingStation> result = stationService.getAllStations();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(stationRepository, times(1)).findAll();
    }

    // ==================== 根据ID获取充电站测试 ====================

    @Test
    @DisplayName("根据ID获取充电站 - 存在")
    void testGetStationByIdFound() {
        // Arrange
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station1));

        // Act
        ChargingStation result = stationService.getStationById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("中关村充电站", result.getName());
        verify(stationRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("根据ID获取充电站 - 不存在")
    void testGetStationByIdNotFound() {
        // Arrange
        when(stationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            stationService.getStationById(999L);
        });

        verify(stationRepository, times(1)).findById(999L);
    }

    // ==================== 获取附近充电站测试 ====================

    @Test
    @DisplayName("获取附近充电站 - 成功（使用距离计算）")
    void testGetNearbyStationsSuccess() {
        // Arrange
        Double latitude = 39.9045;
        Double longitude = 116.4073;
        Double radius = 10.0; // 10km

        List<ChargingStation> nearbyStations = new ArrayList<>();
        nearbyStations.add(station1);
        nearbyStations.add(station2);

        when(stationRepository.findNearbyStations(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(nearbyStations);
        when(cacheService.buildStationListKey(latitude, longitude, radius))
                .thenReturn("station_list_39.9045_116.4073_10.0");
        when(cacheService.get(anyString(), any())).thenReturn(null);

        // Act
        List<StationVO> result = stationService.getNearbyStations(latitude, longitude, radius);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(stationRepository, times(1)).findNearbyStations(anyDouble(), anyDouble(), anyDouble(), anyDouble());
        verify(cacheService, times(1)).set(anyString(), anyList(), anyLong());
    }

    @Test
    @DisplayName("获取附近充电站 - 使用缓存")
    void testGetNearbyStationsCached() {
        // Arrange
        Double latitude = 39.9045;
        Double longitude = 116.4073;
        Double radius = 10.0;

        List<StationVO> cachedResult = new ArrayList<>();
        cachedResult.add(StationVO.builder().id(1L).name("中关村充电站").build());

        when(cacheService.buildStationListKey(latitude, longitude, radius))
                .thenReturn("station_list_39.9045_116.4073_10.0");
        when(cacheService.get(anyString(), any())).thenReturn(cachedResult);

        // Act
        List<StationVO> result = stationService.getNearbyStations(latitude, longitude, radius);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(stationRepository, never()).findNearbyStations(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("获取附近充电站 - 参数验证失败（纬度范围）")
    void testGetNearbyStationsInvalidLatitude() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            stationService.getNearbyStations(91.0, 116.4073, 10.0); // 纬度超出范围
        });

        verify(stationRepository, never()).findNearbyStations(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("获取附近充电站 - 参数验证失败（经度范围）")
    void testGetNearbyStationsInvalidLongitude() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            stationService.getNearbyStations(39.9045, 181.0, 10.0); // 经度超出范围
        });

        verify(stationRepository, never()).findNearbyStations(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("获取附近充电站 - 参数验证失败（搜索半径）")
    void testGetNearbyStationsInvalidRadius() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            stationService.getNearbyStations(39.9045, 116.4073, 150.0); // 半径超出范围
        });

        verify(stationRepository, never()).findNearbyStations(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("获取附近充电站 - 空值参数")
    void testGetNearbyStationsNullParams() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            stationService.getNearbyStations(null, 116.4073, 10.0);
        });

        verify(stationRepository, never()).findNearbyStations(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    // ==================== 创建充电站测试 ====================

    @Test
    @DisplayName("创建充电站 - 成功")
    void testCreateStationSuccess() {
        // Arrange
        ChargingStation newStation = new ChargingStation();
        newStation.setOperatorId(100L);
        newStation.setName("新充电站");
        newStation.setProvince("北京市");
        newStation.setCity("北京市");
        newStation.setAddress("新地址");
        newStation.setLatitude(new BigDecimal("39.9045"));
        newStation.setLongitude(new BigDecimal("116.4073"));

        ChargingStation savedStation = new ChargingStation();
        savedStation.setId(4L);
        savedStation.setOperatorId(100L);
        savedStation.setName("新充电站");
        savedStation.setStatus((byte) 1);
        savedStation.setTotalPiles(0);
        savedStation.setAvailablePiles(0);
        savedStation.setRating(0.0);
        savedStation.setReviewCount(0);

        when(stationRepository.save(any(ChargingStation.class))).thenReturn(savedStation);

        // Act
        ChargingStation result = stationService.createStation(newStation);

        // Assert
        assertNotNull(result);
        assertEquals(4L, result.getId());
        assertEquals((byte) 1, result.getStatus()); // 默认ACTIVE
        assertEquals(0, result.getTotalPiles()); // 初始化为0
        assertEquals(0.0, result.getRating()); // 初始化为0.0
        verify(stationRepository, times(1)).save(any(ChargingStation.class));
    }

    @Test
    @DisplayName("创建充电站 - 初始化默认值")
    void testCreateStationWithDefaults() {
        // Arrange
        ChargingStation newStation = new ChargingStation();
        newStation.setOperatorId(100L);
        newStation.setName("新充电站");
        newStation.setProvince("北京市");
        newStation.setCity("北京市");
        newStation.setLatitude(new BigDecimal("39.9045"));
        newStation.setLongitude(new BigDecimal("116.4073"));

        ChargingStation saved = new ChargingStation();
        saved.setId(5L);
        saved.setStatus((byte) 1);
        saved.setTotalPiles(0);
        saved.setAvailablePiles(0);
        saved.setRating(0.0);
        saved.setReviewCount(0);

        when(stationRepository.save(any(ChargingStation.class))).thenReturn(saved);

        // Act
        ChargingStation result = stationService.createStation(newStation);

        // Assert
        assertNotNull(result);
        assertEquals((byte) 1, result.getStatus());
        assertEquals(0, result.getTotalPiles());
        assertEquals(0, result.getReviewCount());
    }

    // ==================== 更新充电站测试 ====================

    @Test
    @DisplayName("更新充电站 - 成功")
    void testUpdateStationSuccess() {
        // Arrange
        ChargingStation updateData = new ChargingStation();
        updateData.setId(1L);
        updateData.setName("更新后的充电站");
        updateData.setRating(4.9);

        when(stationRepository.findById(1L)).thenReturn(Optional.of(station1));
        when(stationRepository.save(any(ChargingStation.class))).thenReturn(station1);

        // Act
        ChargingStation result = stationService.updateStation(updateData);

        // Assert
        assertNotNull(result);
        verify(stationRepository, times(1)).findById(1L);
        verify(stationRepository, times(1)).save(any(ChargingStation.class));
        verify(cacheService, times(1)).evictStationCache(1L);
    }

    @Test
    @DisplayName("更新充电站 - 不存在")
    void testUpdateStationNotFound() {
        // Arrange
        ChargingStation updateData = new ChargingStation();
        updateData.setId(999L);

        when(stationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            stationService.updateStation(updateData);
        });

        verify(stationRepository, times(1)).findById(999L);
        verify(stationRepository, never()).save(any(ChargingStation.class));
    }

    // ==================== 更新充电桩统计测试 ====================

    @Test
    @DisplayName("更新充电站充电桩统计 - 成功")
    void testUpdateStationPileCountSuccess() {
        // Arrange
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station1));
        when(pileRepository.countByStationId(1L)).thenReturn(10L);
        when(pileRepository.countByStationIdAndStatus(1L, (byte) 1)).thenReturn(3L);
        when(stationRepository.save(any(ChargingStation.class))).thenReturn(station1);

        // Act
        stationService.updateStationPileCount(1L);

        // Assert
        verify(stationRepository, times(1)).findById(1L);
        verify(pileRepository, times(1)).countByStationId(1L);
        verify(pileRepository, times(1)).countByStationIdAndStatus(1L, (byte) 1);
        verify(stationRepository, times(1)).save(any(ChargingStation.class));
        verify(cacheService, times(1)).evictStationCache(1L);
    }

    // ==================== 删除充电站测试 ====================

    @Test
    @DisplayName("删除充电站 - 成功")
    void testDeleteStationSuccess() {
        // Act
        stationService.deleteStation(1L);

        // Assert
        verify(stationRepository, times(1)).deleteById(1L);
        verify(cacheService, times(1)).evictStationCache(1L);
    }
}
