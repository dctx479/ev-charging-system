package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.dto.AIPredictionDTO.*;
import com.ev.charging.service.AIPredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * AI预测控制器
 * 提供充电时长预测和故障预测接口
 */
@RestController
@RequestMapping("/api/ai")
@Validated
@Slf4j
@Tag(name = "AI预测接口", description = "AI智能预测相关接口")
public class AIPredictionController {

    @Autowired
    private AIPredictionService aiPredictionService;

    /**
     * 充电时长预测
     *
     * @param request 预测请求参数
     * @return 预测结果（充电时长、充电量、预计费用）
     */
    @PostMapping("/predict/duration")
    @Operation(summary = "充电时长预测", description = "基于AI模型预测充电所需时长和费用")
    public Result<DurationPredictResponse> predictChargeDuration(
            @Valid @RequestBody DurationPredictRequest request) {

        log.info("收到充电时长预测请求: batteryCapacity={}, currentSoc={}, targetSoc={}, chargePower={}",
                request.getBatteryCapacity(), request.getCurrentSoc(),
                request.getTargetSoc(), request.getChargePower());

        try {
            DurationPredictResponse response = aiPredictionService.predictChargeDuration(request);
            return Result.success(response);

        } catch (IllegalArgumentException e) {
            log.warn("充电时长预测参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());

        } catch (Exception e) {
            log.error("充电时长预测失败: {}", e.getMessage(), e);
            return Result.error(500, "充电时长预测失败: " + e.getMessage());
        }
    }

    /**
     * 故障预测
     *
     * @param request 预测请求参数
     * @return 预测结果（故障概率、风险等级、维护建议）
     */
    @PostMapping("/predict/fault")
    @Operation(summary = "故障预测", description = "基于AI模型预测充电桩故障概率")
    public Result<FaultPredictResponse> predictFault(
            @Valid @RequestBody FaultPredictRequest request) {

        log.info("收到故障预测请求: totalChargeCount={}, healthScore={}, daysSinceLastMaintenance={}",
                request.getTotalChargeCount(), request.getHealthScore(),
                request.getDaysSinceLastMaintenance());

        try {
            FaultPredictResponse response = aiPredictionService.predictFault(request);
            return Result.success(response);

        } catch (IllegalArgumentException e) {
            log.warn("故障预测参数错误: {}", e.getMessage());
            return Result.error(400, e.getMessage());

        } catch (Exception e) {
            log.error("故障预测失败: {}", e.getMessage(), e);
            return Result.error(500, "故障预测失败: " + e.getMessage());
        }
    }

    /**
     * AI服务健康检查
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    @Operation(summary = "AI服务健康检查", description = "检查AI服务是否可用")
    public Result<Boolean> checkAIServiceHealth() {
        boolean available = aiPredictionService.isAIServiceAvailable();

        if (available) {
            return Result.success("AI服务运行正常", true);
        } else {
            return Result.error(503, "AI服务暂时不可用");
        }
    }
}
