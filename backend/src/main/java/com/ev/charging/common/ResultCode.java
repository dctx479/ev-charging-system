package com.ev.charging.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一状态码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    // 成功
    SUCCESS(200, "操作成功"),

    // 客户端错误 4xx
    ERROR(400, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "无权访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    // 服务端错误 5xx
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),

    // 业务错误 1xxx
    USER_NOT_EXIST(1001, "用户不存在"),
    USER_ALREADY_EXIST(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    TOKEN_EXPIRED(1004, "Token已过期"),
    TOKEN_INVALID(1005, "Token无效"),

    STATION_NOT_FOUND(2001, "充电站不存在"),
    PILE_NOT_FOUND(2002, "充电桩不存在"),
    PILE_UNAVAILABLE(2003, "充电桩不可用"),
    PILE_BUSY(2004, "充电桩使用中"),

    ORDER_NOT_FOUND(3001, "订单不存在"),
    ORDER_CANCELLED(3002, "订单已取消"),
    ORDER_ALREADY_PAID(3003, "订单已支付"),
    INSUFFICIENT_BALANCE(3004, "余额不足"),

    QUEUE_FULL(4001, "排队已满"),
    QUEUE_EXPIRED(4002, "排队已过期"),
    QUEUE_NOT_FOUND(4003, "排队记录不存在"),
    QUEUE_CALL_TIMEOUT(4004, "叫号超时，请重新排队"),
    USER_ALREADY_QUEUING(4005, "用户已在排队"),
    USER_ALREADY_CHARGING(4006, "用户已有充电订单进行中"),

    INSUFFICIENT_CREDITS(5001, "积分不足"),
    CREDIT_PRODUCT_NOT_FOUND(5002, "积分商品不存在"),

    V2G_DISCHARGE_FAILED(6001, "V2G放电失败"),
    V2G_NOT_ELIGIBLE(6002, "不满足V2G条件");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 消息
     */
    private final String message;
}
