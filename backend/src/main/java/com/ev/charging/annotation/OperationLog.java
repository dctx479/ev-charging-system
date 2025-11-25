package com.ev.charging.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 * 用于标记需要记录的关键操作
 *
 * 使用示例：
 * @OperationLog(module = "用户管理", operation = "DELETE", description = "删除用户")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /**
     * 操作模块（如：用户管理、充电站管理、充电桩管理等）
     */
    String module() default "";

    /**
     * 操作类型（如：CREATE、UPDATE、DELETE、QUERY、REVIEW等）
     */
    String operation() default "";

    /**
     * 操作描述
     */
    String description() default "";
}
