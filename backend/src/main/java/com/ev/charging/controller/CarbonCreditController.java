package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.dto.CreditRecordVO;
import com.ev.charging.dto.CreditStatisticsVO;
import com.ev.charging.dto.RedeemDTO;
import com.ev.charging.service.CarbonCreditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 碳积分控制器
 */
@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
@Slf4j
public class CarbonCreditController {

    private final CarbonCreditService carbonCreditService;

    /**
     * 获取积分余额
     *
     * @param userId 用户ID
     * @return 积分余额
     */
    @GetMapping("/balance")
    public Result<Integer> getCreditBalance(@RequestParam Long userId) {
        log.info("查询用户 {} 的积分余额", userId);
        Integer balance = carbonCreditService.getCreditBalance(userId);
        return Result.success(balance);
    }

    /**
     * 获取积分记录
     *
     * @param userId     用户ID
     * @param creditType 积分类型（可选：1-充电 2-签到 3-兑换 4-活动）
     * @return 积分记录列表
     */
    @GetMapping("/history")
    public Result<List<CreditRecordVO>> getCreditHistory(
            @RequestParam Long userId,
            @RequestParam(required = false) Byte creditType) {
        log.info("查询用户 {} 的积分记录，类型: {}", userId, creditType);
        List<CreditRecordVO> records = carbonCreditService.getCreditHistory(userId, creditType);
        return Result.success(records);
    }

    /**
     * 每日签到
     *
     * @param userId 用户ID
     * @return 获得的积分
     */
    @PostMapping("/checkin")
    public Result<Integer> dailyCheckIn(@RequestParam Long userId) {
        log.info("用户 {} 请求签到", userId);
        try {
            Integer credits = carbonCreditService.dailyCheckIn(userId);
            return Result.success("签到成功，获得" + credits + "积分", credits);
        } catch (RuntimeException e) {
            log.warn("签到失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取积分统计
     *
     * @param userId 用户ID
     * @return 积分统计信息
     */
    @GetMapping("/statistics")
    public Result<CreditStatisticsVO> getCreditStatistics(@RequestParam Long userId) {
        log.info("查询用户 {} 的积分统计", userId);
        CreditStatisticsVO statistics = carbonCreditService.getCreditStatistics(userId);
        return Result.success(statistics);
    }

    /**
     * 兑换积分
     *
     * @param userId    用户ID
     * @param redeemDTO 兑换信息
     * @return 兑换结果
     */
    @PostMapping("/redeem")
    public Result<Void> redeemCredits(
            @RequestParam Long userId,
            @Valid @RequestBody RedeemDTO redeemDTO) {
        log.info("用户 {} 兑换积分: {}", userId, redeemDTO.getAmount());
        try {
            carbonCreditService.redeemCredits(userId, redeemDTO.getAmount(), redeemDTO.getDescription());
            return Result.success("兑换成功", null);
        } catch (RuntimeException e) {
            log.warn("兑换失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
