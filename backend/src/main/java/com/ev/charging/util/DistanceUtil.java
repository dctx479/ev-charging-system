package com.ev.charging.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 距离计算工具类
 * 使用Haversine公式计算两个经纬度坐标之间的距离
 */
public class DistanceUtil {

    /**
     * 地球半径（单位：千米）
     */
    private static final double EARTH_RADIUS = 6371.0;

    /**
     * 计算两点之间的距离
     *
     * @param lat1 第一个点的纬度
     * @param lon1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lon2 第二个点的经度
     * @return 距离（单位：千米）
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // 转换为弧度
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // 应用Haversine公式
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    /**
     * 计算两点之间的距离（支持BigDecimal坐标）
     *
     * @param lat1 第一个点的纬度
     * @param lon1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lon2 第二个点的经度
     * @return 距离（单位：千米）
     */
    public static double calculateDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        return calculateDistance(
                lat1.doubleValue(),
                lon1.doubleValue(),
                lat2.doubleValue(),
                lon2.doubleValue()
        );
    }

    /**
     * 计算两点之间的距离为BigDecimal（支持高精度）
     *
     * @param lat1 第一个点的纬度
     * @param lon1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lon2 第二个点的经度
     * @return 距离（单位：千米，精度4位小数）
     */
    public static BigDecimal calculateDistanceAsBigDecimal(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        double distance = calculateDistance(lat1, lon1, lat2, lon2);
        return BigDecimal.valueOf(distance).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 计算两点之间的距离（米）
     *
     * @param lat1 第一个点的纬度
     * @param lon1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lon2 第二个点的经度
     * @return 距离（单位：米）
     */
    public static double calculateDistanceInMeters(double lat1, double lon1, double lat2, double lon2) {
        return calculateDistance(lat1, lon1, lat2, lon2) * 1000;
    }

    /**
     * 计算两点之间的距离（米，支持BigDecimal坐标）
     *
     * @param lat1 第一个点的纬度
     * @param lon1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lon2 第二个点的经度
     * @return 距离（单位：米）
     */
    public static double calculateDistanceInMeters(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        return calculateDistanceInMeters(
                lat1.doubleValue(),
                lon1.doubleValue(),
                lat2.doubleValue(),
                lon2.doubleValue()
        );
    }

    /**
     * 格式化距离显示
     *
     * @param distance 距离（单位：千米）
     * @return 格式化后的距离字符串
     */
    public static String formatDistance(double distance) {
        if (distance < 1) {
            return String.format("%.0f米", distance * 1000);
        } else {
            return String.format("%.2f公里", distance);
        }
    }
}
