package com.ev.charging.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建代充订单DTO
 */
@Data
@Schema(description = "创建代充订单请求")
public class CreateValetOrderDTO {

    /**
     * 站点ID
     */
    @Schema(description = "充电站ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "站点ID不能为空")
    private Long stationId;

    /**
     * 充电桩ID
     */
    @Schema(description = "充电桩ID", example = "1")
    private Long pileId;

    /**
     * 车牌号
     */
    @Schema(description = "车牌号", example = "京A12345", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "车牌号不能为空")
    @Size(min = 5, max = 20, message = "车牌号长度必须在5-20个字符之间")
    private String carPlate;

    /**
     * 停车位置
     */
    @Schema(description = "停车位置描述", example = "地下一层B区23号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "停车位置不能为空")
    @Size(max = 255, message = "停车位置长度不能超过255个字符")
    private String parkingLocation;

    /**
     * 目标电量%
     */
    @Schema(description = "目标充电电量（%）", example = "80", minimum = "0", maximum = "100")
    @NotNull(message = "目标电量不能为空")
    @Min(value = 20, message = "目标电量不能低于20%")
    @Max(value = 100, message = "目标电量不能超过100%")
    private Byte targetSoc;

    /**
     * 代充师傅ID（用户选择的师傅）
     */
    @Schema(description = "代充师傅ID", example = "1")
    private Long chargerId;

    /**
     * 取车时间
     */
    @Schema(description = "预计取车时间", example = "2024-01-20 18:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pickupTime;
}
