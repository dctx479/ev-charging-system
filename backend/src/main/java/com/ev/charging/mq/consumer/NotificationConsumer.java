package com.ev.charging.mq.consumer;

import com.ev.charging.config.RabbitMQConfig;
import com.ev.charging.mq.event.NotificationMessage;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 通知消费者
 */
@Component
@Slf4j
public class NotificationConsumer {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotification(@Payload NotificationMessage notification,
                                   Channel channel,
                                   Message message,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("收到通知消息: userId={}, type={}, title={}, messageId={}",
                    notification.getUserId(), notification.getType(),
                    notification.getTitle(), notification.getMessageId());

            // 幂等性检查（仅检查是否已成功处理，不写入标记）
            if (isDuplicate(notification.getMessageId())) {
                log.warn("重复消息: messageId={}", notification.getMessageId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 处理通知（这里可以集成短信、邮件、推送等）
            sendNotification(notification);

            // 业务处理成功，标记为已处理（防止重复消费）
            markProcessed(notification.getMessageId());

            // 手动确认
            channel.basicAck(deliveryTag, false);
            log.info("通知消息处理成功: userId={}", notification.getUserId());

        } catch (Exception e) {
            log.error("处理通知消息失败: userId={}, type={}, reason={}",
                    notification.getUserId(), notification.getType(), e.getMessage());
            log.debug("处理异常详情", e);
            handleException(channel, deliveryTag, notification.getMessageId(), e);
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

    private void sendNotification(NotificationMessage notification) {
        // 实现通知发送逻辑
        try {
            log.info("开始发送通知: userId={}, type={}, title={}",
                    notification.getUserId(), notification.getType(), notification.getTitle());

            // TODO: 集成实际的通知服务
            // - 短信通知
            // - 邮件通知
            // - App推送
            // - WebSocket推送

            // 模拟发送成功
            log.info("通知发送成功: userId={}, type={}", notification.getUserId(), notification.getType());
        } catch (Exception e) {
            // 通知发送失败时记录日志，但不阻塞消费流程
            // 用户可能收不到及时通知，但数据不会受影响
            log.error("通知发送失败: userId={}, type={}, reason={}",
                    notification.getUserId(), notification.getType(), e.getMessage());
            log.debug("通知发送异常详情", e);
            // 注意: 此处不重新抛异常，通知失败不应导致消息重试
            // 如果需要确保通知可靠性，应考虑:
            // 1. 将失败的通知保存到数据库
            // 2. 实现定时任务重试失败的通知
            // 3. 为关键通知类型配置额外的重试机制
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
                log.info("消息重新入队: retryCount={}", retryCount);
            } else {
                redisTemplate.delete("mq:retry:" + messageId);
                channel.basicNack(deliveryTag, false, false);
                log.error("消息超过重试次数，进入死信队列");
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
