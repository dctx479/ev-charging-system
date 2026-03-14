package com.ev.charging.service;

import com.ev.charging.dto.AIPredictionDTO.DurationPredictRequest;
import com.ev.charging.dto.AIPredictionDTO.DurationPredictResponse;
import com.ev.charging.dto.AIPredictionDTO.FaultPredictRequest;
import com.ev.charging.dto.AIPredictionDTO.FaultPredictResponse;
import com.ev.charging.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI预测服务
 * 调用Python Flask AI服务进行充电时长预测和故障预测
 * 修复：添加超时设置和简单熔断机制
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIPredictionService {

    private final RestTemplate restTemplate;

    /**
     * AI服务基础URL，从配置文件读取
     */
    @Value("${ai.service.url:http://localhost:5000}")
    private String aiServiceUrl;

    /**
     * 熔断器相关
     * 修复：添加简单的熔断机制（不需要引入Resilience4j库）
     */
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private volatile long lastFailureTime = 0;
    private static final int FAILURE_THRESHOLD = 3;  // 3次失败触发熔断
    private static final long CIRCUIT_BREAK_DURATION = 30000;  // 熔断30秒后自动恢复

    /**
     * 调用AI服务并解析响应的公共方法
     *
     * @param url     API端点URL
     * @param request 请求体
     * @return 解析后的data Map
     * @throws RuntimeException 如果响应格式错误或AI服务返回错误
     */
    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> callAIServiceAndParseData(String url, Object request) {
        java.util.Map<String, Object> responseMap = restTemplate.postForObject(url, request, java.util.Map.class);

        if (responseMap == null) {
            log.error("AI服务返回空响应");
            recordFailure();
            throw new BusinessException("AI服务返回空响应");
        }

        Integer code = (Integer) responseMap.get("code");
        if (code == null || code != 200) {
            String message = (String) responseMap.getOrDefault("message", "AI服务调用失败");
            log.error("AI服务返回错误: code={}, message={}", code, message);
            recordFailure();
            throw new BusinessException(message);
        }

        java.util.Map<String, Object> data = (java.util.Map<String, Object>) responseMap.get("data");
        if (data == null) {
            log.error("AI服务返回数据为空");
            recordFailure();
            throw new BusinessException("AI服务返回数据为空");
        }

        return data;
    }

    /**
     * 预测充电时长
     *
     * @param request 预测请求参数
     * @return 预测结果
     */
    public DurationPredictResponse predictChargeDuration(DurationPredictRequest request) {
        String url = aiServiceUrl + "/api/ai/predict/duration";

        log.info("调用AI服务预测充电时长: url={}, request={}", url, request);

        if (isCircuitOpen()) {
            log.warn("AI服务熔断器打开，返回默认值");
            return getDefaultDurationResponse(request);
        }

        try {
            validateDurationRequest(request);

            java.util.Map<String, Object> data = callAIServiceAndParseData(url, request);

            DurationPredictResponse response = new DurationPredictResponse();
            Object durationObj = data.get("duration");
            Object chargeAmountObj = data.get("charge_amount");
            Object estimatedCostObj = data.get("estimated_cost");
            if (durationObj == null || chargeAmountObj == null || estimatedCostObj == null) {
                log.error("AI服务返回数据字段缺失: duration={}, charge_amount={}, estimated_cost={}",
                        durationObj, chargeAmountObj, estimatedCostObj);
                recordFailure();
                throw new BusinessException("AI服务返回数据格式错误");
            }
            response.setDuration(new BigDecimal(durationObj.toString()));
            response.setChargeAmount(new BigDecimal(chargeAmountObj.toString()));
            response.setEstimatedCost(new BigDecimal(estimatedCostObj.toString()));

            resetFailure();

            log.info("充电时长预测成功: duration={}分钟, chargeAmount={}kWh, estimatedCost={}元",
                    response.getDuration(), response.getChargeAmount(), response.getEstimatedCost());

            return response;

        } catch (HttpClientErrorException e) {
            log.error("AI服务调用失败(客户端错误): status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            recordFailure();
            return getDefaultDurationResponse(request);

        } catch (HttpServerErrorException e) {
            log.error("AI服务调用失败(服务器错误): status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            recordFailure();
            return getDefaultDurationResponse(request);

        } catch (ResourceAccessException e) {
            log.error("AI服务连接超时或无法访问: {}", e.getMessage());
            recordFailure();
            return getDefaultDurationResponse(request);

        } catch (Exception e) {
            log.error("充电时长预测失败: {}", e.getMessage(), e);
            recordFailure();
            return getDefaultDurationResponse(request);
        }
    }

    /**
     * 预测故障概率
     *
     * @param request 预测请求参数
     * @return 预测结果
     */
    public FaultPredictResponse predictFault(FaultPredictRequest request) {
        String url = aiServiceUrl + "/api/ai/predict/fault";

        log.info("调用AI服务预测故障概率: url={}, request={}", url, request);

        if (isCircuitOpen()) {
            log.warn("AI服务熔断器打开，返回默认值");
            return getDefaultFaultResponse();
        }

        try {
            validateFaultRequest(request);

            java.util.Map<String, Object> data = callAIServiceAndParseData(url, request);

            FaultPredictResponse response = new FaultPredictResponse();
            Object faultProbObj = data.get("fault_probability");
            if (faultProbObj == null) {
                log.error("AI服务返回数据字段缺失: fault_probability=null");
                recordFailure();
                throw new BusinessException("AI服务返回数据格式错误");
            }
            response.setFaultProbability(new BigDecimal(faultProbObj.toString()));

            Object willFaultObj = data.get("will_fault");
            if (willFaultObj instanceof Boolean) {
                response.setWillFault((Boolean) willFaultObj);
            } else if (willFaultObj != null) {
                response.setWillFault(Boolean.parseBoolean(willFaultObj.toString()));
            } else {
                response.setWillFault(false);
            }

            Object suggestionObj = data.get("suggestion");
            response.setSuggestion(suggestionObj != null ? suggestionObj.toString() : "正常运行");
            response.calculateRiskLevel();

            resetFailure();

            log.info("故障预测成功: faultProbability={}%, willFault={}, riskLevel={}, suggestion={}",
                    response.getFaultProbability(), response.getWillFault(),
                    response.getRiskLevel(), response.getSuggestion());

            return response;

        } catch (HttpClientErrorException e) {
            log.error("AI服务调用失败(客户端错误): status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            recordFailure();
            return getDefaultFaultResponse();

        } catch (HttpServerErrorException e) {
            log.error("AI服务调用失败(服务器错误): status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            recordFailure();
            return getDefaultFaultResponse();

        } catch (ResourceAccessException e) {
            log.error("AI服务连接超时或无法访问: {}", e.getMessage());
            recordFailure();
            return getDefaultFaultResponse();

        } catch (Exception e) {
            log.error("故障预测失败: {}", e.getMessage(), e);
            recordFailure();
            return getDefaultFaultResponse();
        }
    }

    /**
     * 验证充电时长预测请求参数
     */
    private void validateDurationRequest(DurationPredictRequest request) {
        if (request.getCurrentSoc().compareTo(request.getTargetSoc()) >= 0) {
            throw new IllegalArgumentException("目标电量必须大于当前电量");
        }

        if (request.getBatteryCapacity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("电池容量必须大于0");
        }

        if (request.getChargePower().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("充电功率必须大于0");
        }

        // 如果没有提供温度，设置默认值为25℃
        if (request.getTemperature() == null) {
            request.setTemperature(new BigDecimal("25"));
        }
    }

    /**
     * 验证故障预测请求参数
     */
    private void validateFaultRequest(FaultPredictRequest request) {
        if (request.getTotalChargeCount() < 0) {
            throw new IllegalArgumentException("累计充电次数不能为负数");
        }

        if (request.getTotalChargeAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("累计充电量不能为负数");
        }

        if (request.getHealthScore() < 0 || request.getHealthScore() > 100) {
            throw new IllegalArgumentException("健康度评分必须在0-100之间");
        }

        if (request.getDaysSinceLastMaintenance() < 0) {
            throw new IllegalArgumentException("距上次维护天数不能为负数");
        }
    }

    /**
     * 检查AI服务是否可用
     *
     * @return true-可用，false-不可用
     */
    public boolean isAIServiceAvailable() {
        String url = aiServiceUrl + "/health";

        try {
            String response = restTemplate.getForObject(url, String.class);
            return response != null;
        } catch (Exception e) {
            log.warn("AI服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查熔断器是否打开
     * 修复：添加简单熔断机制
     */
    private boolean isCircuitOpen() {
        if (failureCount.get() >= FAILURE_THRESHOLD) {
            long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime;
            if (timeSinceLastFailure < CIRCUIT_BREAK_DURATION) {
                return true;  // 熔断器打开
            } else {
                // 熔断时间已过，重置
                resetFailure();
                return false;
            }
        }
        return false;
    }

    /**
     * 记录一次失败
     */
    private void recordFailure() {
        failureCount.incrementAndGet();
        lastFailureTime = System.currentTimeMillis();
        log.warn("AI服务调用失败, 失败次数: {}", failureCount.get());
    }

    /**
     * 重置失败计数
     */
    private void resetFailure() {
        failureCount.set(0);
        lastFailureTime = 0;
        log.debug("AI服务调用成功，重置失败计数");
    }

    /**
     * 获取默认的充电时长预测响应
     * 修复：AI服务不可用时返回默认值而不是抛异常
     */
    private DurationPredictResponse getDefaultDurationResponse(DurationPredictRequest request) {
        DurationPredictResponse response = new DurationPredictResponse();
        try {
            // 简单估算：基于电池容量和充电功率计算
            BigDecimal batteryCapacity = request.getBatteryCapacity();
            BigDecimal chargePower = request.getChargePower();
            BigDecimal currentSoc = request.getCurrentSoc();
            BigDecimal targetSoc = request.getTargetSoc();

            // 需要充电的容量
            BigDecimal chargeAmount = batteryCapacity
                    .multiply(targetSoc.subtract(currentSoc))
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);

            // 预估时间 = 充电量 / 充电功率 * 60
            BigDecimal duration = chargeAmount
                    .divide(chargePower, 2, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("60"));

            // 预估成本：0.8元/kWh（平时价格）
            BigDecimal estimatedCost = chargeAmount.multiply(new BigDecimal("0.8"));

            response.setDuration(duration);
            response.setChargeAmount(chargeAmount);
            response.setEstimatedCost(estimatedCost);

            log.info("AI服务不可用，使用默认预测: duration={}分钟", duration);
        } catch (Exception e) {
            log.error("计算默认预测值失败: {}", e.getMessage());
            // 返回极端默认值
            response.setDuration(new BigDecimal("120"));
            response.setChargeAmount(new BigDecimal("0"));
            response.setEstimatedCost(new BigDecimal("0"));
        }
        return response;
    }

    /**
     * 获取默认的故障预测响应
     * 修复：AI服务不可用时返回默认值而不是抛异常
     */
    private FaultPredictResponse getDefaultFaultResponse() {
        FaultPredictResponse response = new FaultPredictResponse();
        response.setFaultProbability(new BigDecimal("50"));  // 50%中等风险（百分比，与AI服务返回格式一致）
        response.setWillFault(false);
        response.setSuggestion("AI服务暂时不可用，建议进行定期检查");
        response.calculateRiskLevel();
        log.info("AI服务不可用，使用默认故障预测");
        return response;
    }
}
