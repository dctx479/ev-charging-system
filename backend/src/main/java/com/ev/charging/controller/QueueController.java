package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.dto.JoinQueueDTO;
import com.ev.charging.service.QueueService;
import com.ev.charging.vo.QueueStatusVO;
import com.ev.charging.vo.StationQueueInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 排队管理控制器
 */
@Tag(name = "智能排队系统", description = "虚拟排队、位置更新、到号通知等功能")
@RestController
@RequestMapping(value = "/queue", produces = "application/json;charset=UTF-8")
@RequiredArgsConstructor
@Slf4j
public class QueueController {

    private final QueueService queueService;

    /**
     * 加入排队
     */
    @Operation(
            summary = "加入排队",
            description = "用户加入充电站的虚拟排队，无需现场等待",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "加入排队成功"),
            @ApiResponse(responseCode = "400", description = "参数错误或已在排队中"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @PostMapping("/join")
    public Result<Long> joinQueue(
            @Parameter(description = "排队请求参数", required = true)
            @RequestBody @Valid JoinQueueDTO dto,
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        log.info("用户{}请求加入站点{}排队", userId, dto.getStationId());
        Long queueId = queueService.joinQueue(userId, dto);
        return Result.success("加入排队成功", queueId);
    }

    /**
     * 获取我的排队状态
     */
    @Operation(
            summary = "获取我的排队状态",
            description = "查询当前用户的排队状态，包括前面等待人数、预计等待时间等",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping("/status")
    public Result<QueueStatusVO> getQueueStatus(@Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        log.info("用户{}查询排队状态", userId);
        try {
            QueueStatusVO status = queueService.getQueueStatus(userId);
            return Result.success(status);
        } catch (IllegalArgumentException e) {
            // 无排队记录属于正常状态，返回 null data 而非错误，避免前端显示错误提示
            log.info("用户{}当前无排队记录", userId);
            return Result.success(null);
        }
    }

    /**
     * 离开队列
     */
    @Operation(
            summary = "离开队列",
            description = "用户主动离开排队队列",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "离开成功"),
            @ApiResponse(responseCode = "400", description = "无排队记录"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @DeleteMapping("/leave")
    public Result<Void> leaveQueue(@Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        log.info("用户{}请求离开队列", userId);
        queueService.leaveQueue(userId);
        return Result.success("已离开队列", null);
    }

    /**
     * 获取站点排队信息
     */
    @Operation(
            summary = "获取站点排队信息",
            description = "查询指定充电站的排队情况，包括排队人数、预计等待时间等"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/station/{stationId}")
    public Result<StationQueueInfoVO> getStationQueueInfo(
            @Parameter(description = "充电站ID", required = true, example = "1")
            @PathVariable Long stationId) {
        log.info("查询站点{}排队信息", stationId);
        StationQueueInfoVO info = queueService.getStationQueueInfo(stationId);
        return Result.success(info);
    }
}
