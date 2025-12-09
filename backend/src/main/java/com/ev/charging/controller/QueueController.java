package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.dto.JoinQueueDTO;
import com.ev.charging.service.QueueService;
import com.ev.charging.vo.QueueStatusVO;
import com.ev.charging.vo.StationQueueInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 排队管理控制器
 */
@RestController
@RequestMapping("/queue")
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
    @PostMapping("/join")
    public Result<Long> joinQueue(@RequestBody @Valid JoinQueueDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
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
    @GetMapping("/status")
    public Result<QueueStatusVO> getQueueStatus(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("用户{}查询排队状态", userId);

        try {
            QueueStatusVO status = queueService.getQueueStatus(userId);
            return Result.success(status);
        } catch (IllegalArgumentException e) {
            log.warn("查询排队状态失败: {}", e.getMessage());
            return Result.error(404, e.getMessage());
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
    @DeleteMapping("/leave")
    public Result<Void> leaveQueue(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
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
    @GetMapping("/station/{stationId}")
    public Result<StationQueueInfoVO> getStationQueueInfo(@PathVariable Long stationId) {
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
