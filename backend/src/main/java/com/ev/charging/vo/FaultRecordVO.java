package com.ev.charging.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 故障记录VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaultRecordVO {

    private Long id;
    private Long pileId;
    private String pileNo;
    private String pileName;
    private Long stationId;
    private String stationName;
    private Byte faultType;
    private String faultTypeText;
    private String faultCode;
    private String faultDescription;
    private Byte severity;
    private String severityText;
    private Byte reportSource;
    private String reportSourceText;
    private Long reporterId;
    private String reporterName;
    private LocalDateTime faultTime;
    private Byte repairStatus;
    private String repairStatusText;
    private String repairPerson;
    private LocalDateTime repairStartTime;
    private LocalDateTime repairEndTime;
    private BigDecimal repairCost;
    private String repairNote;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
