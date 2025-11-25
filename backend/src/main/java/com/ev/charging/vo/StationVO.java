package com.ev.charging.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 充电站展示VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationVO {

    /**
     * 充电站ID
     */
    private Long id;

    /**
     * 充电站名称
     */
    private String name;

    /**
     * 充电站地址
     */
    private String address;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 营业时间
     */
    private String businessHours;

    /**
     * 总充电桩数量
     */
    private Integer totalPiles;

    /**
     * 可用充电桩数量
     */
    private Integer availablePiles;

    /**
     * 充电站状态
     */
    private String status;

    /**
     * 充电站图片URL
     */
    private String imageUrl;

    /**
     * 充电站描述
     */
    private String description;

    /**
     * 评分
     */
    private Double rating;

    /**
     * 评价数量
     */
    private Integer reviewCount;

    /**
     * 距离（千米）- 仅在查询附近充电站时返回
     */
    private Double distance;

    /**
     * 格式化距离显示
     */
    private String distanceText;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
