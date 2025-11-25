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
import org.springframework.web.bind.annotation.*;

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
     *
     * @param dto     请求参数
     * @param request HTTP请求
     * @return 排队记录ID
     */
    @Operation(
            summary = "加入排队",
            description = "用户加入充电站的虚拟排队，无需现场等待",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "加入排队成功"),
            @ApiResponse(responseCode = "400", description = "参数错误或已在排队中"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @PostMapping("/join")
    public Result<Long> joinQueue(
            @Parameter(description = "排队请求参数", required = true)
            @RequestBody @Valid JoinQueueDTO dto,
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        log.info("用户{}请求加入站点{}排队", userId, dto.getStationId());

        try {
            Long queueId = queueService.joinQueue(userId, dto);
            return Result.success("加入排队成功", queueId);
        } catch (IllegalArgumentException e) {
            log.warn("加入排队失败: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("加入排队异常", e);
            return Result.error(500, "系统异常，请稍后重试");
        }
    }

    /**
     * 获取我的排队状态
     *
     * @param request HTTP请求
     * @return 排队状态
     */
    @Operation(
            summary = "获取我的排队状态",
            description = "查询当前用户的排队状态，包括前面等待人数、预计等待时间等",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "404", description = "无排队记录"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
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
        } catch (Exception e) {
            log.error("查询排队状态异常", e);
            return Result.error(500, "系统异常，请稍后重试");
        }
    }

    /**
     * 离开队列
     *
     * @param request HTTP请求
     * @return 操作结果
     */
    @Operation(
            summary = "离开队列",
            description = "用户主动离开排队队列",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "离开成功"),
            @ApiResponse(responseCode = "400", description = "无排队记录"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @DeleteMapping("/leave")
    public Result<Void> leaveQueue(@Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        log.info("用户{}请求离开队列", userId);

        try {
            queueService.leaveQueue(userId);
            return Result.success("已离开队列", null);
        } catch (IllegalArgumentException e) {
            log.warn("离开队列失败: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("离开队列异常", e);
            return Result.error(500, "系统异常，请稍后重试");
        }
    }

    /**
     * 获取站点排队信息
     *
     * @param stationId 充电站ID
     * @return 排队信息
     */
    @Operation(
            summary = "获取站点排队信息",
            description = "查询指定充电站的排队情况，包括排队人数、预计等待时间等"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "充电站不存在"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @GetMapping("/station/{stationId}")
    public Result<StationQueueInfoVO> getStationQueueInfo(
            @Parameter(description = "充电站ID", required = true, example = "1")
            @PathVariable Long stationId) {
        log.info("查询站点{}排队信息", stationId);

        try {
            StationQueueInfoVO info = queueService.getStationQueueInfo(stationId);
            return Result.success(info);
        } catch (IllegalArgumentException e) {
            log.warn("查询站点排队信息失败: {}", e.getMessage());
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("查询站点排队信息异常", e);
            return Result.error(500, "系统异常，请稍后重试");
        }
    }
}
