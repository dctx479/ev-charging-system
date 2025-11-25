package com.ev.charging.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "operation_log")
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 操作用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 操作用户名
     */
    @Column(name = "username", length = 50)
    private String username;

    /**
     * 操作模块
     */
    @Column(name = "module", length = 50, nullable = false)
    private String module;

    /**
     * 操作类型：CREATE-创建, UPDATE-更新, DELETE-删除, QUERY-查询, REVIEW-审核
     */
    @Column(name = "operation_type", length = 20, nullable = false)
    private String operationType;

    /**
     * 操作描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 请求方法
     */
    @Column(name = "method", length = 10)
    private String method;

    /**
     * 请求URL
     */
    @Column(name = "url", length = 200)
    private String url;

    /**
     * 请求参数
     */
    @Column(name = "params", columnDefinition = "TEXT")
    private String params;

    /**
     * IP地址
     */
    @Column(name = "ip", length = 50)
    private String ip;

    /**
     * 操作状态：SUCCESS-成功, FAILURE-失败
     */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    /**
     * 错误信息
     */
    @Column(name = "error_msg", length = 1000)
    private String errorMsg;

    /**
     * 执行时长（毫秒）
     */
    @Column(name = "duration")
    private Long duration;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}
