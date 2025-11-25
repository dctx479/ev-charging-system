package com.ev.charging.mq.consumer;

import com.ev.charging.config.RabbitMQConfig;
import com.ev.charging.mq.event.CreditChangeEvent;
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
 * 积分变更事件消费者
 *
 * 职责: 处理用户积分变更事件，更新用户积分记录
 * 支持的变更类型:
 * - CHARGE_REWARD: 充电获得的奖励积分
 * - CONSUMPTION: 积分消费
 * - MANUAL_ADJUSTMENT: 运营手动调整
 */
@Component
@Slf4j
public class CreditChangeEventConsumer {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private com.ev.charging.service.CarbonCreditService carbonCreditService;

    @RabbitListener(queues = RabbitMQConfig.CREDIT_CHANGE_QUEUE)
    public void handleCreditChangeEvent(@Payload CreditChangeEvent event,
                                       Channel channel,
                                       Message message,
                                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("收到积分变更事件: userId={}, creditAmount={}, changeType={}, messageId={}",
                    event.getUserId(), event.getCreditAmount(), event.getChangeType(), event.getMessageId());

            // 幂等性检查（仅检查是否已成功处理，不写入标记）
            if (isDuplicate(event.getMessageId())) {
                log.warn("重复消息: messageId={}", event.getMessageId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 处理积分变更
            processCreditChange(event);

            // 业务处理成功，标记为已处理（防止重复消费）
            markProcessed(event.getMessageId());

            // 手动确认
            channel.basicAck(deliveryTag, false);
            log.info("积分变更事件处理成功: userId={}, changeType={}", event.getUserId(), event.getChangeType());

        } catch (Exception e) {
            log.error("处理积分变更事件失败: userId={}, changeType={}, reason={}",
                    event.getUserId(), event.getChangeType(), e.getMessage());
            log.debug("处理异常详情", e);
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
     * 处理积分变更逻辑
     *
     * 注意: 当前实现仅记录日志，实际系统应该:
     * 1. 验证积分变更的合法性
     * 2. 更新数据库中用户的积分余额
     * 3. 记录积分变更历史（便于审计）
     * 4. 如果积分消费，检查用户是否有足够的积分
     */
    private void processCreditChange(CreditChangeEvent event) {
        log.info("处理积分变更: userId={}, creditAmount={}, changeType={}, description={}",
                event.getUserId(), event.getCreditAmount(), event.getChangeType(), event.getDescription());

        // 根据变更类型执行对应操作
        String changeType = event.getChangeType();
        if ("CONSUMPTION".equals(changeType) && event.getCreditAmount() > 0) {
            carbonCreditService.redeemCredits(event.getUserId(), event.getCreditAmount(), event.getDescription());
        }
        // CHARGE_REWARD 类型已由 OrderPaidConsumer 直接调用 addCreditsForCharging，此处仅做日志确认
        // MANUAL_ADJUSTMENT 等其他类型可在后续迭代中扩展

        log.info("积分变更处理成功: userId={}, changeType={}", event.getUserId(), event.getChangeType());
    }

    /**
     * 异常处理：使用 Redis INCR 计数实现最多3次重试，超限后送死信队列
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
