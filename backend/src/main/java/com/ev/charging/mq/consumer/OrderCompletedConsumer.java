package com.ev.charging.mq.consumer;

import com.ev.charging.config.RabbitMQConfig;
import com.ev.charging.mq.event.NotificationMessage;
import com.ev.charging.mq.event.OrderCompletedEvent;
import com.ev.charging.mq.producer.MessageProducer;
import com.ev.charging.service.StatisticsService;
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
 * 订单完成消费者
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCompletedConsumer {

    private final MessageProducer messageProducer;
    private final StringRedisTemplate redisTemplate;
    private final StatisticsService statisticsService;

    /**
     * 处理订单完成事件
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_COMPLETED_QUEUE)
    public void handleOrderCompleted(@Payload OrderCompletedEvent event,
                                     Channel channel,
                                     Message message,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("收到订单完成事件: orderId={}, messageId={}", event.getOrderId(), event.getMessageId());

            // 1. 幂等性检查（仅检查是否已成功处理，不写入标记）
            if (isDuplicate(event.getMessageId())) {
                log.warn("重复消息: messageId={}", event.getMessageId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 发送通知
            sendCompletionNotification(event);

            // 3. 业务处理成功，标记为已处理（防止重复消费）
            markProcessed(event.getMessageId());

            // 4. 手动确认
            channel.basicAck(deliveryTag, false);
            log.info("订单完成事件处理成功: orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("处理订单完成事件失败: orderId={}", event.getOrderId(), e);
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

    /**
     * 发送充电完成通知 - 异常隔离，通知失败不影响主流程
     *
     * 注意: 这个方法在主消费流程中调用，任何异常都被catch并记录，
     * 不会导致整条消息重试。通知失败时，用户可能收不到及时提醒，
     * 但订单状态和数据不会受影响。
     */
    private void sendCompletionNotification(OrderCompletedEvent event) {
        try {
            NotificationMessage notification = NotificationMessage.builder()
                    .userId(event.getUserId())
                    .type("ORDER_COMPLETED")
                    .title("充电已完成")
                    .content(String.format("您的充电订单已完成，充电量%.2fkWh，费用%.2f元",
                            event.getChargeAmount(), event.getTotalFee()))
                    .relatedId(String.valueOf(event.getOrderId()))
                    .relatedType("ORDER")
                    .build();

            messageProducer.sendNotification(notification);
            log.info("充电完成通知已发送: orderId={}, userId={}", event.getOrderId(), event.getUserId());
        } catch (Exception e) {
            // 通知失败不阻塞主流程，仅记录警告日志
            // 用户可能收不到及时通知，但订单状态已正确更新
            log.warn("发送充电完成通知失败: orderId={}, userId={}, reason={}",
                    event.getOrderId(), event.getUserId(), e.getMessage());
            log.debug("通知异常详情", e);
        }
    }

    /**
     * 异常处理：使用 Redis INCR 计数实现最多3次重试，超限后送死信队列
     * requeue=true 的消息不经过 DLX，x-death 头不会被设置，故需 Redis 计数
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
