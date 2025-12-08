package com.ev.charging.service;

import com.ev.charging.dto.AIPredictionDTO.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * AI预测服务
 * 调用Python Flask AI服务进行充电时长预测和故障预测
 */
@Service
@Slf4j
public class AIPredictionService {

    @Autowired
    private RestTemplate restTemplate;

    /**
     * AI服务基础URL，从配置文件读取
     */
    @Value("${ai.service.url:http://localhost:5000}")
    private String aiServiceUrl;

    /**
     * 预测充电时长
     *
     * @param request 预测请求参数
     * @return 预测结果
     */
    public DurationPredictResponse predictChargeDuration(DurationPredictRequest request) {
        String url = aiServiceUrl + "/api/ai/predict/duration";

        log.info("调用AI服务预测充电时长: url={}, request={}", url, request);

        try {
            // 验证请求参数
            validateDurationRequest(request);

            // 调用AI服务
            DurationPredictResponse response = restTemplate.postForObject(
                    url,
                    request,
                    DurationPredictResponse.class
            );

            if (response == null) {
                log.error("AI服务返回空响应");
                throw new RuntimeException("AI服务返回空响应");
            }

            log.info("充电时长预测成功: duration={}分钟, chargeAmount={}kWh, estimatedCost={}元",
                    response.getDuration(), response.getChargeAmount(), response.getEstimatedCost());

            return response;

        } catch (HttpClientErrorException e) {
            log.error("AI服务调用失败(客户端错误): status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("AI服务调用失败: " + e.getMessage());

        } catch (HttpServerErrorException e) {
            log.error("AI服务调用失败(服务器错误): status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("AI服务内部错误，请稍后重试");

        } catch (ResourceAccessException e) {
            log.error("AI服务连接超时或无法访问: {}", e.getMessage());
            throw new RuntimeException("AI服务暂时不可用，请稍后重试");

        } catch (Exception e) {
            log.error("充电时长预测失败: {}", e.getMessage(), e);
            throw new RuntimeException("充电时长预测失败: " + e.getMessage());
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

        try {
            // 验证请求参数
            validateFaultRequest(request);

            // 调用AI服务
            FaultPredictResponse response = restTemplate.postForObject(
                    url,
                    request,
                    FaultPredictResponse.class
            );

            if (response == null) {
                log.error("AI服务返回空响应");
                throw new RuntimeException("AI服务返回空响应");
            }

            // 计算风险等级
            response.calculateRiskLevel();

            log.info("故障预测成功: faultProbability={}%, willFault={}, riskLevel={}, suggestion={}",
                    response.getFaultProbability(), response.getWillFault(),
                    response.getRiskLevel(), response.getSuggestion());

            return response;

        } catch (HttpClientErrorException e) {
            log.error("AI服务调用失败(客户端错误): status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("AI服务调用失败: " + e.getMessage());

        } catch (HttpServerErrorException e) {
            log.error("AI服务调用失败(服务器错误): status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("AI服务内部错误，请稍后重试");

        } catch (ResourceAccessException e) {
            log.error("AI服务连接超时或无法访问: {}", e.getMessage());
            throw new RuntimeException("AI服务暂时不可用，请稍后重试");

        } catch (Exception e) {
            log.error("故障预测失败: {}", e.getMessage(), e);
            throw new RuntimeException("故障预测失败: " + e.getMessage());
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
}
