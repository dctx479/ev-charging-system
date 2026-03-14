package com.ev.charging.constant;

import java.math.BigDecimal;

/**
 * 订单相关常量
 */
public class OrderConstants {

    // ==================== 订单状态 ====================
    /**
     * 订单状态：待支付
     */
    public static final byte ORDER_STATUS_PENDING_PAYMENT = 1;

    /**
     * 订单状态：充电中
     */
    public static final byte ORDER_STATUS_CHARGING = 2;

    /**
     * 订单状态：已完成
     */
    public static final byte ORDER_STATUS_COMPLETED = 3;

    /**
     * 订单状态：已取消
     */
    public static final byte ORDER_STATUS_CANCELLED = 4;

    /**
     * 订单状态：异常
     */
    public static final byte ORDER_STATUS_EXCEPTION = 5;

    // ==================== 支付状态 ====================
    /**
     * 支付状态：未支付
     */
    public static final byte PAYMENT_STATUS_UNPAID = 0;

    /**
     * 支付状态：已支付
     */
    public static final byte PAYMENT_STATUS_PAID = 1;

    /**
     * 支付状态：退款中
     */
    public static final byte PAYMENT_STATUS_REFUNDING = 2;

    /**
     * 支付状态：已退款
     */
    public static final byte PAYMENT_STATUS_REFUNDED = 3;

    // ==================== 支付方式 ====================
    /**
     * 支付方式：微信支付
     */
    public static final byte PAYMENT_METHOD_WECHAT = 1;

    /**
     * 支付方式：支付宝
     */
    public static final byte PAYMENT_METHOD_ALIPAY = 2;

    /**
     * 支付方式：余额支付
     */
    public static final byte PAYMENT_METHOD_BALANCE = 3;

    // ==================== 峰谷平电价 (元/kWh) ====================
    /**
     * 谷时电价（23:00-07:00）
     */
    public static final BigDecimal PRICE_VALLEY = new BigDecimal("0.40");

    /**
     * 平时电价（07:00-10:00, 15:00-18:00, 21:00-23:00）
     */
    public static final BigDecimal PRICE_FLAT = new BigDecimal("0.80");

    /**
     * 峰时电价（10:00-15:00, 18:00-21:00）
     */
    public static final BigDecimal PRICE_PEAK = new BigDecimal("1.20");

    /**
     * 服务费费率（元/kWh）
     */
    public static final BigDecimal SERVICE_FEE_RATE = new BigDecimal("0.50");

    /**
     * 充电安全上限（kWh），防止异常数据超过大多数电动车电池容量
     */
    public static final BigDecimal MAX_CHARGE_CAPACITY = new BigDecimal("200");

    // ==================== 排队系统相关 ====================
    /**
     * 排队状态：排队中
     */
    public static final byte QUEUE_STATUS_QUEUING = 1;

    /**
     * 排队状态：已叫号
     */
    public static final byte QUEUE_STATUS_CALLED = 2;

    /**
     * 排队状态：已取消
     */
    public static final byte QUEUE_STATUS_CANCELLED = 3;

    /**
     * 排队状态：已过号
     */
    public static final byte QUEUE_STATUS_EXPIRED = 4;

    /**
     * 排队系统：最大排队人数
     */
    public static final int MAX_QUEUE_SIZE = 20;

    /**
     * 排队系统：叫号有效期（分钟），用户叫号后需要在规定时间内到达
     */
    public static final int QUEUE_CALL_TIMEOUT_MINUTES = 30;

    /**
     * 获取订单状态文本
     */
    public static String getOrderStatusText(Byte status) {
        if (status == null) return "未知";
        return switch (status) {
            case ORDER_STATUS_PENDING_PAYMENT -> "待支付";
            case ORDER_STATUS_CHARGING -> "充电中";
            case ORDER_STATUS_COMPLETED -> "已完成";
            case ORDER_STATUS_CANCELLED -> "已取消";
            case ORDER_STATUS_EXCEPTION -> "异常";
            default -> "未知";
        };
    }

    /**
     * 获取支付状态文本
     */
    public static String getPaymentStatusText(Byte status) {
        if (status == null) return "未知";
        return switch (status) {
            case PAYMENT_STATUS_UNPAID -> "未支付";
            case PAYMENT_STATUS_PAID -> "已支付";
            case PAYMENT_STATUS_REFUNDING -> "退款中";
            case PAYMENT_STATUS_REFUNDED -> "已退款";
            default -> "未知";
        };
    }

    /**
     * 获取支付方式文本
     */
    public static String getPaymentMethodText(Byte method) {
        if (method == null) return "";
        return switch (method) {
            case PAYMENT_METHOD_WECHAT -> "微信支付";
            case PAYMENT_METHOD_ALIPAY -> "支付宝";
            case PAYMENT_METHOD_BALANCE -> "余额支付";
            default -> "未知";
        };
    }

    /**
     * 获取排队状态文本
     */
    public static String getQueueStatusText(Byte status) {
        if (status == null) return "未知";
        return switch (status) {
            case QUEUE_STATUS_QUEUING -> "排队中";
            case QUEUE_STATUS_CALLED -> "已叫号";
            case QUEUE_STATUS_CANCELLED -> "已取消";
            case QUEUE_STATUS_EXPIRED -> "已过号";
            default -> "未知";
        };
    }
}
