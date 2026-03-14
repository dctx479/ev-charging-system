package com.ev.charging.mq.consumer;

import com.ev.charging.config.RabbitMQConfig;
import com.ev.charging.mq.event.CreditChangeEvent;
import com.ev.charging.mq.event.NotificationMessage;
import com.ev.charging.mq.event.OrderPaidEvent;
import com.ev.charging.mq.producer.MessageProducer;
import com.ev.charging.service.CarbonCreditService;
import com.ev.charging.exception.BusinessException;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 订单支付消费者
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderPaidConsumer {

    private final CarbonCreditService carbonCreditService;
    private final MessageProducer messageProducer;
    private final StringRedisTemplate redisTemplate;

    @RabbitListener(queues = RabbitMQConfig.ORDER_PAID_QUEUE)
    public void handleOrderPaid(@Payload OrderPaidEvent event,
                                Channel channel,
                                Message message,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("收到订单支付事件: orderId={}, amount={}, messageId={}",
                    event.getOrderId(), event.getAmount(), event.getMessageId());

            // 幂等性检查（仅检查是否已成功处理，不写入标记）
            if (isDuplicate(event.getMessageId())) {
                log.warn("重复消息: messageId={}", event.getMessageId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 1. 发放碳积分（关键业务操作，失败需触发重试）
            try {
                Integer credits = carbonCreditService.addCreditsForCharging(
                        event.getOrderId(),
                        event.getUserId(),
                        event.getChargeAmount()
                );
                log.info("碳积分发放成功: orderId={}, userId={}, credits={}",
                        event.getOrderId(), event.getUserId(), credits);

                // 发送积分变更事件（非关键，失败不阻塞）
                try {
                    CreditChangeEvent creditEvent = CreditChangeEvent.builder()
                            .userId(event.getUserId())
                            .orderId(event.getOrderId())
                            .creditAmount(credits)
                            .changeType("CHARGE_REWARD")
                            .description("充电获得积分奖励")
                            .build();
                    messageProducer.sendCreditChangeMessage(creditEvent);
                } catch (Exception ce) {
                    log.warn("积分变更事件发送失败（不影响积分发放）: orderId={}", event.getOrderId(), ce);
                }

            } catch (Exception e) {
                // 碳积分发放失败：抛出异常触发消息重试机制
                log.error("碳积分发放失败，将触发重试: orderId={}, userId={}, amount={}, reason={}",
                        event.getOrderId(), event.getUserId(), event.getChargeAmount(), e.getMessage());
                throw new BusinessException("碳积分发放失败，需要重试", e);
            }

            // 2. 发送支付成功通知
            sendPaymentNotification(event);

            // 3. 业务处理成功，标记为已处理（防止重复消费）
            markProcessed(event.getMessageId());

            // 手动确认
            channel.basicAck(deliveryTag, false);
            log.info("订单支付事件处理成功: orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("处理订单支付事件失败: orderId={}", event.getOrderId(), e);
            handleException(channel, deliveryTag, event.getMessageId(), e);
        }
    }

    /**
     * 幂等性检查：仅检查消息是否已成功处理，不写入标记
     * 标记写入在业务逻辑成功后由 markProcessed() 完成
     */
    private boolean isDuplicate(String messageId) {
        String key = "mq:msg:" + messageId;
        String value = redisTemplate.opsForValue().get(key);
        return "done".equals(value);
    }

    /**
     * 标记消息为已成功处理（在业务逻辑成功后调用）
     * 与 isDuplicate() 配合，确保只有成功处理的消息才被标记，
     * 避免业务失败后重试时消息被误判为重复而丢弃
     */
    private void markProcessed(String messageId) {
        String key = "mq:msg:" + messageId;
        redisTemplate.opsForValue().set(key, "done", 24, TimeUnit.HOURS);
    }

    private void sendPaymentNotification(OrderPaidEvent event) {
        try {
            NotificationMessage notification = NotificationMessage.builder()
                    .userId(event.getUserId())
                    .type("ORDER_PAID")
                    .title("支付成功")
                    .content(String.format("您已成功支付%.2f元，订单号：%s",
                            event.getAmount(), event.getOrderNo()))
                    .relatedId(String.valueOf(event.getOrderId()))
                    .relatedType("ORDER")
                    .build();

            messageProducer.sendNotification(notification);
            log.info("支付成功通知已发送: orderId={}, userId={}", event.getOrderId(), event.getUserId());
        } catch (Exception e) {
            // 支付通知发送失败不影响订单状态，仅记录日志
            log.warn("支付成功通知发送失败: orderId={}, userId={}, reason={}",
                    event.getOrderId(), event.getUserId(), e.getMessage());
            log.debug("支付通知异常详情", e);
        }
    }

    /**
     * 处理消费异常：使用 Redis INCR 计数实现最多3次重试，超限后送死信队列
     * 注意：x-death header 仅在消息经过 DLX 路由时才被设置，requeue=true 的消息不经过 DLX，
     * 因此原 getRetryCount 方法始终返回0，会导致无限重试循环，已修复为 Redis 计数方式。
     */
    private void handleException(Channel channel, long deliveryTag, String messageId, Exception e) {
        try {
            int retryCount = incrementRetryCount(messageId);
            if (retryCount <= 3) {
                channel.basicNack(deliveryTag, false, true);
                log.info("消息重新入队: messageId={}, retryCount={}", messageId, retryCount);
            } else {
                redisTemplate.delete("mq:retry:" + messageId);
                channel.basicNack(deliveryTag, false, false);
                log.error("消息超过重试次数，进入死信队列: messageId={}", messageId);
            }
        } catch (IOException ex) {
            log.error("消息确认失败", ex);
        }
    }

    /**
     * 使用 Redis Lua 脚本原子递增重试计数，确保 TTL 安全设置
     * 防止 key 永久残留导致的资源泄漏
     */
    private int incrementRetryCount(String messageId) {
        String key = "mq:retry:" + messageId;
        String luaScript = "local count = redis.call('INCR', KEYS[1]); "
                + "redis.call('EXPIRE', KEYS[1], 3600); "
                + "return count;";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
        Long count = redisTemplate.execute(script, Collections.singletonList(key));
        return count != null ? count.intValue() : 1;
    }
}
