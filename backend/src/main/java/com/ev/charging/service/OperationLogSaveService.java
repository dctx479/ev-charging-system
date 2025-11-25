package com.ev.charging.service;

import com.ev.charging.entity.OperationLog;
import com.ev.charging.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 操作日志异步保存服务
 * 独立Service确保Spring AOP代理正确处理@Async注解
 * （同一类内自调用@Async方法不会生效，因为绕过了代理）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogSaveService {

    private final OperationLogRepository operationLogRepository;

    /**
     * 异步保存日志（不阻塞主业务）
     * 具有3次重试机制
     */
    @Async
    public void saveLogAsync(OperationLog operationLog) {
        // 对日志内容进行净化，防止日志注入攻击
        operationLog.setDescription(sanitizeLogInput(operationLog.getDescription()));
        operationLog.setErrorMsg(sanitizeLogInput(operationLog.getErrorMsg()));
        operationLog.setUsername(sanitizeLogInput(operationLog.getUsername()));
        operationLog.setUrl(sanitizeLogInput(operationLog.getUrl()));
        operationLog.setParams(sanitizeLogInput(operationLog.getParams()));

        int retries = 3;
        int retryCount = 0;

        while (retryCount < retries) {
            try {
                operationLogRepository.save(operationLog);
                log.debug("操作日志保存成功: {}", operationLog.getDescription());
                return;
            } catch (Exception e) {
                retryCount++;
                if (retryCount >= retries) {
                    log.error("操作日志保存失败（已重试{}次）: {}", retries, e.getMessage(), e);
                } else {
                    log.warn("操作日志保存失败，准备第{}次重试", retryCount);
                    try {
                        Thread.sleep(retryCount * 100L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("日志保存重试被中断", ie);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 净化日志输入，防止日志注入攻击
     */
    private String sanitizeLogInput(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.replaceAll("[\\r\\n\\t\\x00]", " ");
    }
}
