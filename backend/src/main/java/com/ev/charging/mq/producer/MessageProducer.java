package com.ev.charging.mq.producer;

import com.ev.charging.config.RabbitMQConfig;
import com.ev.charging.mq.event.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 消息生产者服务
 *
 * 注意: 当前实现使用 RabbitTemplate 的 confirm callback 和 return callback
 * 来处理发送失败：
 * - confirmCallback: 消息是否成功发送到 RabbitMQ 服务器
 * - returnCallback: 消息是否成功被路由到队列
 *
 * 这两个 callback 已在 RabbitMQConfig 中配置，会输出日志告警
 * 但消息丢失时无法自动重试，需要上层应用处理异常
 */
@Service
@Slf4j
public class MessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送订单完成消息
     *
     * 异常处理说明:
     * - convertAndSend 的异常通常表示 RabbitTemplate 初始化问题或网络连接失败
     * - 消息发送成功但无法路由时，会通过 returnCallback 输出日志
     * - 发送异常时记录日志，但不重新抛异常（避免打断业务逻辑）
     *
     * 实际系统中，应考虑:
     * 1. 保存到数据库的失败消息表，定时重试
     * 2. 使用消息队列的本地事务表方案
     * 3. 监控发送失败率，告警给运维
     */
    public void sendOrderCompletedMessage(OrderCompletedEvent event) {
        try {
            if (event.getMessageId() == null) {
                event.setMessageId(UUID.randomUUID().toString());
            }
            if (event.getEventTime() == null) {
                event.setEventTime(LocalDateTime.now());
            }

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_COMPLETED_KEY,
                event
            );

            log.info("发送订单完成消息成功: orderId={}, messageId={}",
                    event.getOrderId(), event.getMessageId());
        } catch (Exception e) {
            // 发送失败时记录错误日志，便于追踪
            log.error("发送订单完成消息失败: orderId={}, messageId={}, reason={}",
                    event.getOrderId(), event.getMessageId(), e.getMessage());
            log.debug("消息发送异常详情", e);

            // 关键: 这里不重新抛异常，避免中断业务流程
            // 消息丢失由 RabbitTemplate 的 callback 机制记录日志告警
            // 后续可实现: 保存失败消息到数据库，定时重试

            // TODO: 实现失败消息持久化
            // messageFailureService.saveFailedMessage("ORDER_COMPLETED", event);
        }
    }

    /**
     * 发送订单支付消息
     */
    public void sendOrderPaidMessage(OrderPaidEvent event) {
        try {
            if (event.getMessageId() == null) {
                event.setMessageId(UUID.randomUUID().toString());
            }
            if (event.getEventTime() == null) {
                event.setEventTime(LocalDateTime.now());
            }

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_PAID_KEY,
                event
            );

            log.info("发送订单支付消息成功: orderId={}, amount={}, messageId={}",
                    event.getOrderId(), event.getAmount(), event.getMessageId());
        } catch (Exception e) {
            log.error("发送订单支付消息失败: orderId={}, amount={}, messageId={}, reason={}",
                    event.getOrderId(), event.getAmount(), event.getMessageId(), e.getMessage());
            log.debug("消息发送异常详情", e);

            // TODO: 实现失败消息持久化
            // messageFailureService.saveFailedMessage("ORDER_PAID", event);
        }
    }

    /**
     * 发送积分变更消息
     */
    public void sendCreditChangeMessage(CreditChangeEvent event) {
        try {
            if (event.getMessageId() == null) {
                event.setMessageId(UUID.randomUUID().toString());
            }
            if (event.getEventTime() == null) {
                event.setEventTime(LocalDateTime.now());
            }

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.CREDIT_CHANGE_KEY,
                event
            );

            log.info("发送积分变更消息成功: userId={}, credits={}, messageId={}",
                    event.getUserId(), event.getCreditAmount(), event.getMessageId());
        } catch (Exception e) {
            log.error("发送积分变更消息失败: userId={}, credits={}, messageId={}, reason={}",
                    event.getUserId(), event.getCreditAmount(), event.getMessageId(), e.getMessage());
            log.debug("消息发送异常详情", e);

            // TODO: 实现失败消息持久化
            // messageFailureService.saveFailedMessage("CREDIT_CHANGE", event);
        }
    }

    /**
     * 发送通知消息
     *
     * 通知消息较为非关键，发送失败时用户可能收不到及时提醒
     * 但不会影响订单、积分等核心数据
     */
    public void sendNotification(NotificationMessage message) {
        try {
            if (message.getMessageId() == null) {
                message.setMessageId(UUID.randomUUID().toString());
            }
            if (message.getEventTime() == null) {
                message.setEventTime(LocalDateTime.now());
            }

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_KEY,
                message
            );

            log.info("发送通知消息成功: userId={}, type={}, messageId={}",
                    message.getUserId(), message.getType(), message.getMessageId());
        } catch (Exception e) {
            log.warn("发送通知消息失败: userId={}, type={}, messageId={}, reason={}",
                    message.getUserId(), message.getType(), message.getMessageId(), e.getMessage());
            log.debug("通知消息发送异常详情", e);

            // 通知消息非关键，发送失败仅记录警告日志
            // 用户可能收不到及时通知，但不影响业务
        }
    }
}
