package com.ev.charging.constant;

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
}
