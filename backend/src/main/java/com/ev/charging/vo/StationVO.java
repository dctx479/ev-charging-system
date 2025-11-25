package com.ev.charging.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "充电站信息")
public class StationVO {

    /**
     * 充电站ID
     */
    @Schema(description = "充电站ID", example = "1")
    private Long id;

    /**
     * 运营商ID
     */
    @Schema(description = "运营商ID", example = "1")
    private Long operatorId;

    /**
     * 充电站名称
     */
    @Schema(description = "充电站名称", example = "国贸充电站")
    private String name;

    /**
     * 充电站地址
     */
    @Schema(description = "充电站详细地址", example = "北京市朝阳区建国门外大街1号")
    private String address;

    /**
     * 省份
     */
    @Schema(description = "省份", example = "北京市")
    private String province;

    /**
     * 城市
     */
    @Schema(description = "城市", example = "北京市")
    private String city;

    /**
     * 区县
     */
    @Schema(description = "区县", example = "朝阳区")
    private String district;

    /**
     * 经度
     */
    @Schema(description = "经度坐标", example = "116.458551")
    private Double longitude;

    /**
     * 纬度
     */
    @Schema(description = "纬度坐标", example = "39.918163")
    private Double latitude;

    /**
     * 停车费(元/小时)
     */
    @Schema(description = "停车费(元/小时)", example = "5.00")
    private java.math.BigDecimal parkingFee;

    /**
     * 联系电话
     */
    @Schema(description = "联系电话", example = "010-12345678")
    private String phone;

    /**
     * 营业时间
     */
    @Schema(description = "营业时间", example = "00:00-24:00")
    private String businessHours;

    /**
     * 总充电桩数量
     */
    @Schema(description = "总充电桩数量", example = "20")
    private Integer totalPiles;

    /**
     * 可用充电桩数量
     */
    @Schema(description = "当前可用充电桩数量", example = "5")
    private Integer availablePiles;

    /**
     * 充电站状态
     */
    @Schema(description = "充电站状态", example = "正常", allowableValues = {"正常", "维护中", "停用"})
    private String status;

    /**
     * 充电站图片URL
     */
    @Schema(description = "充电站图片URL", example = "https://example.com/station.jpg")
    private String imageUrl;

    /**
     * 充电站描述
     */
    @Schema(description = "充电站描述信息", example = "国贸核心商圈，配备快充桩")
    private String description;

    /**
     * 评分
     */
    @Schema(description = "用户评分（0-5）", example = "4.5", minimum = "0", maximum = "5")
    private Double rating;

    /**
     * 评价数量
     */
    @Schema(description = "评价数量", example = "128")
    private Integer reviewCount;

    /**
     * 距离（千米）- 仅在查询附近充电站时返回
     */
    @Schema(description = "距离用户位置的距离（公里）", example = "2.5")
    private Double distance;

    /**
     * 格式化距离显示
     */
    @Schema(description = "格式化的距离文本", example = "2.5公里")
    private String distanceText;

    /**
     * 是否有光伏设施 0-无 1-有
     */
    @Schema(description = "是否有光伏设施", example = "1", allowableValues = {"0", "1"})
    private Byte hasSolar;

    /**
     * 是否有储能设施 0-无 1-有
     */
    @Schema(description = "是否有储能设施", example = "1", allowableValues = {"0", "1"})
    private Byte hasStorage;

    /**
     * 光伏装机容量(kW)
     */
    @Schema(description = "光伏装机容量(kW)", example = "100.00")
    private java.math.BigDecimal solarCapacity;

    /**
     * 储能容量(kWh)
     */
    @Schema(description = "储能容量(kWh)", example = "500.00")
    private java.math.BigDecimal storageCapacity;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2024-01-01 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
