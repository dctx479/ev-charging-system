package com.ev.charging.aspect;

import com.alibaba.fastjson2.JSON;
import com.ev.charging.entity.OperationLog;
import com.ev.charging.service.OperationLogSaveService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志切面
 * 支持两种模式：
 * 1. 自动记录管理后台所有操作（基于包名）
 * 2. 使用@OperationLog注解标记需要记录的方法
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogSaveService operationLogSaveService;

    // 敏感字段列表（用于脱敏）
    private static final List<String> SENSITIVE_FIELDS = List.of(
        "password", "oldPassword", "newPassword", "token", "secret", "key"
    );

    /**
     * 定义切点1：管理后台的所有Controller方法
     */
    @Pointcut("execution(* com.ev.charging.controller.Admin*.*(..))")
    public void adminControllerPointcut() {
    }

    /**
     * 定义切点2：带有@OperationLog注解的方法
     */
    @Pointcut("@annotation(com.ev.charging.annotation.OperationLog)")
    public void operationLogAnnotationPointcut() {
    }

    /**
     * 环绕通知1：记录管理后台操作日志
     */
    @Around("adminControllerPointcut()")
    public Object aroundAdmin(ProceedingJoinPoint joinPoint) throws Throwable {
        return recordLog(joinPoint, null);
    }

    /**
     * 环绕通知2：记录带注解的操作日志
     */
    @Around("operationLogAnnotationPointcut()")
    public Object aroundAnnotation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        com.ev.charging.annotation.OperationLog annotation = method.getAnnotation(com.ev.charging.annotation.OperationLog.class);
        return recordLog(joinPoint, annotation);
    }

    /**
     * 记录操作日志
     */
    private Object recordLog(ProceedingJoinPoint joinPoint, com.ev.charging.annotation.OperationLog annotation) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        // 构建日志对象
        OperationLog operationLog = OperationLog.builder()
                .module(getModuleName(joinPoint, annotation))
                .operationType(getOperationType(joinPoint, annotation))
                .description(getDescription(joinPoint, annotation))
                .method(request != null ? request.getMethod() : "")
                .url(request != null ? request.getRequestURI() : "")
                .params(getParams(joinPoint))
                .ip(request != null ? getIpAddress(request) : "")
                .build();

        // 获取用户信息
        if (request != null && request.getAttribute("userId") != null) {
            operationLog.setUserId((Long) request.getAttribute("userId"));
            operationLog.setUsername((String) request.getAttribute("username"));
        } else {
            // 如果没有用户信息，使用系统默认
            operationLog.setUserId(0L);
            operationLog.setUsername("system");
        }

        Object result = null;
        try {
            // 执行方法
            result = joinPoint.proceed();
            operationLog.setStatus("SUCCESS");
        } catch (Exception e) {
            operationLog.setStatus("FAILURE");
            operationLog.setErrorMsg(truncate(e.getMessage(), 1000));
            throw e;
        } finally {
            // 计算执行时长
            long duration = System.currentTimeMillis() - startTime;
            operationLog.setDuration(duration);

            // 异步保存日志（通过独立Service调用，确保@Async通过Spring AOP代理生效）
            operationLogSaveService.saveLogAsync(operationLog);
        }

        return result;
    }

    /**
     * 获取模块名称
     */
    private String getModuleName(ProceedingJoinPoint joinPoint, com.ev.charging.annotation.OperationLog annotation) {
        // 优先使用注解中的模块名
        if (annotation != null && !annotation.module().isEmpty()) {
            return annotation.module();
        }

        // 根据类名推断模块名
        String className = joinPoint.getTarget().getClass().getSimpleName();
        if (className.contains("User")) {
            return "用户管理";
        } else if (className.contains("Station")) {
            return "充电站管理";
        } else if (className.contains("Pile")) {
            return "充电桩管理";
        } else if (className.contains("Order")) {
            return "订单管理";
        } else if (className.contains("Operator")) {
            return "运营商管理";
        } else if (className.contains("Fault")) {
            return "故障管理";
        } else if (className.contains("Maintenance")) {
            return "维护管理";
        } else if (className.contains("V2G")) {
            return "V2G管理";
        } else if (className.contains("Credit")) {
            return "积分管理";
        }
        return "系统管理";
    }

    /**
     * 获取操作类型
     */
    private String getOperationType(ProceedingJoinPoint joinPoint, com.ev.charging.annotation.OperationLog annotation) {
        // 优先使用注解中的操作类型
        if (annotation != null && !annotation.operation().isEmpty()) {
            return annotation.operation();
        }

        // 根据方法名推断操作类型
        String methodName = joinPoint.getSignature().getName().toLowerCase();
        if (methodName.contains("create") || methodName.contains("add") || methodName.contains("save")) {
            return "CREATE";
        } else if (methodName.contains("update") || methodName.contains("edit") || methodName.contains("modify")) {
            return "UPDATE";
        } else if (methodName.contains("delete") || methodName.contains("remove")) {
            return "DELETE";
        } else if (methodName.contains("review") || methodName.contains("approve") || methodName.contains("reject")) {
            return "REVIEW";
        } else if (methodName.contains("get") || methodName.contains("list") || methodName.contains("query") || methodName.contains("search")) {
            return "QUERY";
        } else if (methodName.contains("export")) {
            return "EXPORT";
        } else if (methodName.contains("import")) {
            return "IMPORT";
        }
        return "OTHER";
    }

    /**
     * 获取操作描述
     */
    private String getDescription(ProceedingJoinPoint joinPoint, com.ev.charging.annotation.OperationLog annotation) {
        // 优先使用注解中的描述
        if (annotation != null && !annotation.description().isEmpty()) {
            return annotation.description();
        }

        // 根据方法名生成描述
        String methodName = joinPoint.getSignature().getName();

        if (methodName.contains("review")) {
            return "审核操作";
        } else if (methodName.contains("updateStatus")) {
            return "更新状态";
        } else if (methodName.contains("delete")) {
            return "删除数据";
        } else if (methodName.contains("create") || methodName.contains("add")) {
            return "创建数据";
        } else if (methodName.contains("update")) {
            return "更新数据";
        } else if (methodName.contains("list")) {
            return "查询列表";
        }

        return methodName;
    }

    /**
     * 获取请求参数（带敏感信息脱敏）
     */
    private String getParams(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return "";
            }

            // 过滤掉不需要记录的参数
            List<Object> filteredArgs = new ArrayList<>();
            for (Object arg : args) {
                // 跳过HttpServletRequest、HttpServletResponse、MultipartFile等
                if (arg instanceof HttpServletRequest
                    || arg instanceof jakarta.servlet.http.HttpServletResponse
                    || arg instanceof MultipartFile) {
                    continue;
                }
                filteredArgs.add(arg);
            }

            if (filteredArgs.isEmpty()) {
                return "";
            }

            // 转换为JSON并进行敏感信息脱敏
            String json = JSON.toJSONString(filteredArgs);
            json = desensitize(json);

            // 限制长度
            return truncate(json, 2000);

        } catch (Exception e) {
            log.warn("获取请求参数失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 敏感信息脱敏
     */
    private String desensitize(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        for (String field : SENSITIVE_FIELDS) {
            // 匹配形如 "password":"xxx" 的模式
            json = json.replaceAll("(\"" + field + "\"\\s*:\\s*\")([^\"]*)(\")", "$1******$3");
        }

        return json;
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    /**
     * 获取IP地址（考虑代理情况）
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 如果通过多个代理，X-Forwarded-For会有多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
