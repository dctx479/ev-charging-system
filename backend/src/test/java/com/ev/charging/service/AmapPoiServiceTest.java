package com.ev.charging.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AmapPoiService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("高德地图POI搜索服务测试")
class AmapPoiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AmapPoiService amapPoiService;

    private BigDecimal testLongitude = new BigDecimal("116.397428");
    private BigDecimal testLatitude = new BigDecimal("39.90923");

    @BeforeEach
    void setUp() {
        // 设置测试用的API Key
        ReflectionTestUtils.setField(amapPoiService, "amapApiKey", "test-api-key");
    }

    @Test
    @DisplayName("搜索周边POI - 成功")
    void testSearchNearbyPoi_Success() {
        // Given
        String mockResponse = createMockSuccessResponse();
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);

        // When
        List<Map<String, Object>> result = amapPoiService.searchNearbyPoi(
                testLongitude, testLatitude, "050000", 1000, 20);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());

        Map<String, Object> firstPoi = result.get(0);
        assertEquals("测试餐厅", firstPoi.get("name"));
        assertEquals("餐饮服务", firstPoi.get("type"));
        assertEquals(100, firstPoi.get("distance"));

        verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
    }

    @Test
    @DisplayName("搜索周边POI - API Key未配置")
    void testSearchNearbyPoi_NoApiKey() {
        // Given
        ReflectionTestUtils.setField(amapPoiService, "amapApiKey", null);

        // When
        List<Map<String, Object>> result = amapPoiService.searchNearbyPoi(
                testLongitude, testLatitude, "050000", 1000, 20);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(restTemplate, never()).getForObject(anyString(), eq(String.class));
    }

    @Test
    @DisplayName("搜索周边POI - API返回失败")
    void testSearchNearbyPoi_ApiFailure() {
        // Given
        String mockResponse = createMockFailureResponse();
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);

        // When
        List<Map<String, Object>> result = amapPoiService.searchNearbyPoi(
                testLongitude, testLatitude, "050000", 1000, 20);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("搜索周边POI - 无结果")
    void testSearchNearbyPoi_NoResults() {
        // Given
        String mockResponse = createMockEmptyResponse();
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);

        // When
        List<Map<String, Object>> result = amapPoiService.searchNearbyPoi(
                testLongitude, testLatitude, "050000", 1000, 20);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("搜索周边餐饮 - 成功")
    void testSearchNearbyRestaurants_Success() {
        // Given
        String mockResponse = createMockSuccessResponse();
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);

        // When
        List<Map<String, Object>> result = amapPoiService.searchNearbyRestaurants(
                testLongitude, testLatitude, 1000);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("搜索周边购物 - 成功")
    void testSearchNearbyShopping_Success() {
        // Given
        String mockResponse = createMockSuccessResponse();
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);

        // When
        List<Map<String, Object>> result = amapPoiService.searchNearbyShopping(
                testLongitude, testLatitude, 1000);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("搜索周边生活服务 - 成功")
    void testSearchNearbyLifeService_Success() {
        // Given
        String mockResponse = createMockSuccessResponse();
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);

        // When
        List<Map<String, Object>> result = amapPoiService.searchNearbyLifeService(
                testLongitude, testLatitude, 1000);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("搜索周边娱乐 - 成功")
    void testSearchNearbyEntertainment_Success() {
        // Given
        String mockResponse = createMockSuccessResponse();
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);

        // When
        List<Map<String, Object>> result = amapPoiService.searchNearbyEntertainment(
                testLongitude, testLatitude, 1000);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("搜索周边酒店 - 成功")
    void testSearchNearbyHotels_Success() {
        // Given
        String mockResponse = createMockSuccessResponse();
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);

        // When
        List<Map<String, Object>> result = amapPoiService.searchNearbyHotels(
                testLongitude, testLatitude, 1000);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("搜索周边POI - 异常处理")
    void testSearchNearbyPoi_Exception() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));

        // When
        List<Map<String, Object>> result = amapPoiService.searchNearbyPoi(
                testLongitude, testLatitude, "050000", 1000, 20);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建模拟成功响应
     */
    private String createMockSuccessResponse() {
        JSONObject response = new JSONObject();
        response.put("status", "1");
        response.put("info", "OK");

        JSONObject poi1 = new JSONObject();
        poi1.put("id", "B001");
        poi1.put("name", "测试餐厅");
        poi1.put("type", "餐饮服务");
        poi1.put("typecode", "050000");
        poi1.put("address", "测试地址1");
        poi1.put("location", "116.397428,39.90923");
        poi1.put("distance", 100);
        poi1.put("tel", "010-12345678");

        JSONObject poi2 = new JSONObject();
        poi2.put("id", "B002");
        poi2.put("name", "测试咖啡厅");
        poi2.put("type", "餐饮服务");
        poi2.put("typecode", "050000");
        poi2.put("address", "测试地址2");
        poi2.put("location", "116.397500,39.90930");
        poi2.put("distance", 200);
        poi2.put("tel", "010-87654321");

        response.put("pois", JSON.toJSONString(new Object[]{poi1, poi2}));

        return response.toJSONString();
    }

    /**
     * 创建模拟失败响应
     */
    private String createMockFailureResponse() {
        JSONObject response = new JSONObject();
        response.put("status", "0");
        response.put("info", "INVALID_USER_KEY");
        return response.toJSONString();
    }

    /**
     * 创建模拟空结果响应
     */
    private String createMockEmptyResponse() {
        JSONObject response = new JSONObject();
        response.put("status", "1");
        response.put("info", "OK");
        response.put("pois", "[]");
        return response.toJSONString();
    }
}
