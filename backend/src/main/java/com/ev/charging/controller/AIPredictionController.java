package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.dto.AIPredictionDTO.DurationPredictRequest;
import com.ev.charging.dto.AIPredictionDTO.DurationPredictResponse;
import com.ev.charging.dto.AIPredictionDTO.FaultPredictRequest;
import com.ev.charging.dto.AIPredictionDTO.FaultPredictResponse;
import com.ev.charging.service.AIPredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI预测控制器
 * 提供充电时长预测和故障预测接口
 */
@RestController
@RequestMapping(value = "/ai", produces = "application/json;charset=UTF-8")
@Validated
@Slf4j
@Tag(name = "AI预测接口", description = "AI智能预测相关接口")
@RequiredArgsConstructor
public class AIPredictionController {

    private final AIPredictionService aiPredictionService;

    /**
     * 充电时长预测
     *
     * @param request 预测请求参数
     * @return 预测结果（充电时长、充电量、预计费用）
     */
    @PostMapping("/predict/duration")
    @Operation(summary = "充电时长预测", description = "基于AI模型预测充电所需时长和费用")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "预测成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "500", description = "AI服务不可用或预测失败")
    })
    public Result<DurationPredictResponse> predictChargeDuration(
            @Parameter(description = "充电时长预测请求参数", required = true)
            @Valid @RequestBody DurationPredictRequest request) {

        log.info("收到充电时长预测请求: batteryCapacity={}, currentSoc={}, targetSoc={}, chargePower={}",
                request.getBatteryCapacity(), request.getCurrentSoc(),
                request.getTargetSoc(), request.getChargePower());

        DurationPredictResponse response = aiPredictionService.predictChargeDuration(request);
        return Result.success(response);
    }

    /**
     * 故障预测
     *
     * @param request 预测请求参数
     * @return 预测结果（故障概率、风险等级、维护建议）
     */
    @PostMapping("/predict/fault")
    @Operation(summary = "故障预测", description = "基于AI模型预测充电桩故障概率")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "预测成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "500", description = "AI服务不可用或预测失败")
    })
    public Result<FaultPredictResponse> predictFault(
            @Parameter(description = "故障预测请求参数", required = true)
            @Valid @RequestBody FaultPredictRequest request) {

        log.info("收到故障预测请求: totalChargeCount={}, healthScore={}, daysSinceLastMaintenance={}",
                request.getTotalChargeCount(), request.getHealthScore(),
                request.getDaysSinceLastMaintenance());

        FaultPredictResponse response = aiPredictionService.predictFault(request);
        return Result.success(response);
    }

    /**
     * AI服务健康检查
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    @Operation(summary = "AI服务健康检查", description = "检查AI服务是否可用")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI服务运行正常"),
            @ApiResponse(responseCode = "503", description = "AI服务暂时不可用")
    })
    public Result<Boolean> checkAIServiceHealth() {
        boolean available = aiPredictionService.isAIServiceAvailable();

        if (available) {
            return Result.success("AI服务运行正常", true);
        } else {
            return Result.error(503, "AI服务暂时不可用");
        }
    }
}
