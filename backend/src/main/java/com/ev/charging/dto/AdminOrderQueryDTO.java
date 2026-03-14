package com.ev.charging.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 管理后台订单查询DTO
 */
@Data
@Schema(description = "管理后台订单查询条件")
public class AdminOrderQueryDTO {

    /**
     * 订单状态：0-进行中 1-已完成 2-已取消 3-异常
     */
    @Schema(description = "订单状态", example = "1", allowableValues = {"0", "1", "2", "3"})
    private Byte orderStatus;

    /**
     * 支付状态：0-未支付 1-已支付 2-已退款
     */
    @Schema(description = "支付状态", example = "1", allowableValues = {"0", "1", "2"})
    private Byte paymentStatus;

    /**
     * 充电站ID
     */
    @Schema(description = "充电站ID（筛选特定站点）", example = "1")
    private Long stationId;

    /**
     * 充电桩ID
     */
    @Schema(description = "充电桩ID（筛选特定充电桩）", example = "5")
    private Long pileId;

    /**
     * 搜索关键词（用户手机号或订单号）
     */
    @Schema(description = "搜索关键词（用户手机号或订单号）", example = "13800138000")
    @Size(max = 50, message = "搜索关键词长度不能超过50个字符")
    private String keyword;

    /**
     * 开始时间
     */
    @Schema(description = "查询起始时间", example = "2024-01-01 00:00:00")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @Schema(description = "查询结束时间", example = "2024-01-31 23:59:59")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 页码（从0开始）
     */
    @Schema(description = "页码（从0开始）", example = "0", minimum = "0")
    @Min(value = 0, message = "页码不能小于0")
    private Integer page = 0;

    /**
     * 每页大小
     */
    @Schema(description = "每页记录数", example = "10", minimum = "1", maximum = "100")
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer size = 10;
}
