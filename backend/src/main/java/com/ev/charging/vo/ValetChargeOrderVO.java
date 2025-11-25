package com.ev.charging.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValetChargeOrderVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long stationId;
    private String stationName;  // 站点名称
    private Long chargerId;
    private String chargerName;  // 师傅姓名
    private String chargerPhone;  // 师傅电话
    private String chargerAvatar;  // 师傅头像
    private Long pileId;
    private String carPlate;
    private String parkingLocation;
    private String carLocation;  // 车辆位置(兼容前端)
    private Byte targetSoc;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pickupTime;
    private BigDecimal serviceFee;
    private BigDecimal servicePrice;  // 服务价格(兼容前端)
    private BigDecimal chargeFee;  // 电费
    private BigDecimal totalFee;  // 总费用
    private Byte orderStatus;
    private Byte status;  // 订单状态(兼容前端)
    private Boolean evaluated;  // 是否已评价
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime assignTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startChargeTime;  // 开始充电时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
