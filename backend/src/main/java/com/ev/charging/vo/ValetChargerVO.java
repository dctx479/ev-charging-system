package com.ev.charging.vo;

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
public class ValetChargerVO {
    private Long id;
    private String name;
    private String phone;
    private String serviceArea;
    private Byte status;
    private Byte isCertified;
    private Integer completedOrders;
    private BigDecimal rating;
    private BigDecimal servicePrice;
    private Integer orderCount;  // 总订单数(前端需要)
    private String avatar;  // 头像(前端需要)
}
