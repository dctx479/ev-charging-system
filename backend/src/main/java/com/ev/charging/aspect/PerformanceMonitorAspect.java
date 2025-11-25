package com.ev.charging.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 性能监控切面
 * 记录接口响应时间，识别慢接口
 */
@Slf4j
@Aspect
@Component
public class PerformanceMonitorAspect {

    // 慢接口阈值（毫秒）
    private static final long SLOW_THRESHOLD = 500;

    /**
     * 定义切点：所有Controller方法
     */
    @Pointcut("execution(* com.ev.charging.controller..*.*(..))")
    public void controllerMethods() {
    }

    /**
     * 环绕通知：记录方法执行时间
     */
    @Around("controllerMethods()")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;

        long startTime = System.currentTimeMillis();

        try {
            // 执行目标方法
            Object result = joinPoint.proceed();

            long executionTime = System.currentTimeMillis() - startTime;

            // 记录执行时间
            if (executionTime > SLOW_THRESHOLD) {
                log.warn("[SLOW API] {} executed in {}ms (threshold: {}ms)",
                        fullMethodName, executionTime, SLOW_THRESHOLD);
            } else {
                log.debug("[API] {} executed in {}ms", fullMethodName, executionTime);
            }

            return result;

        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[API ERROR] {} failed after {}ms: {}",
                    fullMethodName, executionTime, throwable.getMessage());
            throw throwable;
        }
    }
}
