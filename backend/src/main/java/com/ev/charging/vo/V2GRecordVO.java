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
public class V2GRecordVO {
    private Long id;
    private Long userId;
    private String userName;
    private Long pileId;
    private String pileNo;
    private Long stationId;
    private String stationName;
    private Byte recordType;  // 1充电 2放电
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    private BigDecimal energyAmount;
    private BigDecimal electricityPrice;
    private BigDecimal amount;
    private Byte batteryHealthBefore;
    private Byte batteryHealthAfter;
    private BigDecimal profit;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
