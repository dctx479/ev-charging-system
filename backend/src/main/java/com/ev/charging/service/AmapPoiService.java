package com.ev.charging.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 高德地图POI搜索服务
 */
@Slf4j
@Service
public class AmapPoiService {

    @Value("${amap.api.key:}")
    private String amapApiKey;

    private static final String AMAP_POI_SEARCH_URL = "https://restapi.amap.com/v3/place/around";

    private final RestTemplate restTemplate;

    public AmapPoiService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 搜索周边POI
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @param types     POI类型（餐饮服务|购物服务|生活服务|体育休闲服务|医疗保健服务|住宿服务|风景名胜|商务住宅|政府机构及社会团体|科教文化服务|交通设施服务|金融保险服务|公司企业|道路附属设施|地名地址信息|公共设施）
     * @param radius    搜索半径（米），默认1000米
     * @param limit     返回数量，默认20
     * @return POI列表
     */
    public List<Map<String, Object>> searchNearbyPoi(
            BigDecimal longitude,
            BigDecimal latitude,
            String types,
            Integer radius,
            Integer limit) {

        if (amapApiKey == null || amapApiKey.isEmpty()) {
            log.warn("高德地图API Key未配置");
            return new ArrayList<>();
        }

        try {
            // 构建请求参数
            String location = longitude + "," + latitude;
            String url = String.format(
                    "%s?key=%s&location=%s&types=%s&radius=%d&offset=%d&extensions=all",
                    AMAP_POI_SEARCH_URL,
                    amapApiKey,
                    location,
                    types != null ? types : "",
                    radius != null ? radius : 1000,
                    limit != null ? limit : 20
            );

            // 构建日志URL，隐藏API Key
            String maskedUrl = url.replaceAll("key=[^&]*", "key=" + maskApiKey(amapApiKey));
            log.info("高德地图POI搜索请求: {}", maskedUrl);

            // 发送请求
            String response = restTemplate.getForObject(url, String.class);
            JSONObject jsonResponse = JSON.parseObject(response);

            // 检查响应状态
            String status = jsonResponse.getString("status");
            if (!"1".equals(status)) {
                log.error("高德地图API请求失败: {}", jsonResponse.getString("info"));
                return new ArrayList<>();
            }

            // 解析POI数据
            JSONArray pois = jsonResponse.getJSONArray("pois");
            if (pois == null || pois.isEmpty()) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (int i = 0; i < pois.size(); i++) {
                JSONObject poi = pois.getJSONObject(i);

                Map<String, Object> poiData = new HashMap<>();
                poiData.put("id", poi.getString("id"));
                poiData.put("name", poi.getString("name"));
                poiData.put("type", poi.getString("type"));
                poiData.put("typecode", poi.getString("typecode"));
                poiData.put("address", poi.getString("address"));
                poiData.put("distance", poi.getInteger("distance"));
                poiData.put("tel", poi.getString("tel"));

                // 解析位置
                String locationStr = poi.getString("location");
                if (locationStr != null && !locationStr.isEmpty()) {
                    String[] coords = locationStr.split(",");
                    if (coords.length == 2) {
                        poiData.put("longitude", new BigDecimal(coords[0]));
                        poiData.put("latitude", new BigDecimal(coords[1]));
                    }
                }

                // 营业时间
                poiData.put("businessHours", poi.getString("biz_ext"));

                // 评分
                JSONObject bizExt = poi.getJSONObject("biz_ext");
                if (bizExt != null) {
                    poiData.put("rating", bizExt.getString("rating"));
                }

                result.add(poiData);
            }

            log.info("搜索到 {} 个POI", result.size());
            return result;

        } catch (Exception e) {
            log.error("高德地图POI搜索失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 搜索周边餐饮服务
     */
    public List<Map<String, Object>> searchNearbyRestaurants(
            BigDecimal longitude,
            BigDecimal latitude,
            Integer radius) {
        return searchNearbyPoi(longitude, latitude, "050000", radius, 20);
    }

    /**
     * 搜索周边购物服务
     */
    public List<Map<String, Object>> searchNearbyShopping(
            BigDecimal longitude,
            BigDecimal latitude,
            Integer radius) {
        return searchNearbyPoi(longitude, latitude, "060000", radius, 20);
    }

    /**
     * 搜索周边生活服务
     */
    public List<Map<String, Object>> searchNearbyLifeService(
            BigDecimal longitude,
            BigDecimal latitude,
            Integer radius) {
        return searchNearbyPoi(longitude, latitude, "070000", radius, 20);
    }

    /**
     * 搜索周边休闲娱乐
     */
    public List<Map<String, Object>> searchNearbyEntertainment(
            BigDecimal longitude,
            BigDecimal latitude,
            Integer radius) {
        return searchNearbyPoi(longitude, latitude, "080000", radius, 20);
    }

    /**
     * 搜索周边酒店住宿
     */
    public List<Map<String, Object>> searchNearbyHotels(
            BigDecimal longitude,
            BigDecimal latitude,
            Integer radius) {
        return searchNearbyPoi(longitude, latitude, "100000", radius, 20);
    }

    /**
     * 掩盖API Key，用于日志输出
     */
    private String maskApiKey(String key) {
        if (key == null || key.length() < 4) {
            return "****";
        }
        return key.substring(0, 4) + "****";
    }
}
