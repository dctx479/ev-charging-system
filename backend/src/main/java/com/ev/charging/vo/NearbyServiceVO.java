package com.ev.charging.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 周边服务VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyServiceVO {

    /**
     * 服务ID
     */
    private Long id;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务类型：1-餐饮 2-咖啡厅 3-购物 4-娱乐 5-其他
     */
    private Byte serviceType;

    /**
     * 服务类型文本
     */
    private String serviceTypeText;

    /**
     * 服务描述
     */
    private String description;

    /**
     * 距离（米）
     */
    private Integer distance;

    /**
     * 距离文本（如：100米、1.2公里）
     */
    private String distanceText;

    /**
     * 平均消费时间（分钟）
     */
    private Integer avgConsumeTime;

    /**
     * 评分（1-5分）
     */
    private BigDecimal rating;

    /**
     * 地址
     */
    private String address;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 营业时间
     */
    private String businessHours;

    /**
     * 优惠信息
     */
    private String couponInfo;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 推荐理由
     */
    private String recommendReason;

    /**
     * 获取服务类型文本
     */
    public static String getServiceTypeText(Byte serviceType) {
        if (serviceType == null) {
            return "未知";
        }
        return switch (serviceType) {
            case 1 -> "餐饮";
            case 2 -> "咖啡厅";
            case 3 -> "购物";
            case 4 -> "娱乐";
            case 5 -> "其他";
            default -> "未知";
        };
    }

    /**
     * 格式化距离文本
     */
    public static String formatDistance(Integer distance) {
        if (distance == null) {
            return "未知";
        }
        if (distance < 1000) {
            return distance + "米";
        } else {
            return String.format("%.1f公里", distance / 1000.0);
        }
    }
}
