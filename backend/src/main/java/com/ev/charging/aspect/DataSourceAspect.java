package com.ev.charging.aspect;

import com.ev.charging.datasource.DataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据源切换AOP
 * 拦截所有标注@Transactional的方法，根据readOnly属性自动选择主库或从库
 * 
 * @author Architecture Team
 * @since 2026-01-23
 */
@Slf4j
@Aspect
@Component
@Order(1)  // 确保在@Transactional之前执行
@Profile("replication")
public class DataSourceAspect {
    
    /**
     * 环绕通知：拦截@Transactional注解的方法
     * 
     * @param point 连接点
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) point.getSignature();
        
        // 获取@Transactional注解
        Transactional transactional = signature.getMethod().getAnnotation(Transactional.class);
        
        // 判断是否为只读事务
        if (transactional != null && transactional.readOnly()) {
            // 只读事务，路由到从库
            DataSourceContextHolder.setSlave();
            log.debug("只读事务，路由到从库: {}.{}", 
                    signature.getDeclaringTypeName(), signature.getName());
        } else {
            // 写事务，路由到主库
            DataSourceContextHolder.setMaster();
            log.debug("写事务，路由到主库: {}.{}", 
                    signature.getDeclaringTypeName(), signature.getName());
        }
        
        try {
            // 执行目标方法
            return point.proceed();
        } finally {
            // 清除ThreadLocal，防止内存泄漏
            DataSourceContextHolder.clear();
        }
    }
}
