package com.ev.charging.mq.consumer;

import com.ev.charging.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * 死信队列消费者
 *
 * 职责: 处理所有重试超限或处理失败的消息
 * 处理流程:
 * 1. 记录完整的死信信息到日志（包含消息体和头信息）
 * 2. 解析消息头中的 x-death 信息（重试次数、原始队列等）
 * 3. 输出明确的告警信息
 *
 * 注意: 目前实现为日志记录，实际系统应考虑:
 * - 保存到数据库的 dead_letter_message 表
 * - 发送告警邮件或钉钉通知
 * - 定时任务查询并分析失败消息模式
 */
@Component
@Slf4j
public class DlxConsumer {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @RabbitListener(queues = RabbitMQConfig.DLX_QUEUE)
    public void handleDeadLetter(Message message,
                                 Channel channel,
                                 @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
            long currentTime = System.currentTimeMillis();
            String timestamp = dateFormat.format(new Date(currentTime));

            log.error("================== 死信消息警告 ==================");
            log.error("[时间] {}", timestamp);
            log.error("[消息体] {}", messageBody);

            // 解析并记录消息头信息
            Map<String, ?> headers = message.getMessageProperties().getHeaders();
            if (headers != null && !headers.isEmpty()) {
                log.error("[消息头信息]");
                headers.forEach((key, value) -> {
                    if ("x-death".equals(key)) {
                        // x-death 是一个列表，记录了每次重试的信息
                        log.error("  - {}: {} (重试历史信息)", key, value);
                    } else {
                        log.error("  - {}: {}", key, value);
                    }
                });
            }

            // 解析消息属性
            String contentType = message.getMessageProperties().getContentType();
            String exchange = message.getMessageProperties().getReceivedExchange();
            String routingKey = message.getMessageProperties().getReceivedRoutingKey();

            log.error("[原始信息] exchange={}, routingKey={}, contentType={}",
                    exchange, routingKey, contentType);

            // 记录死信消息（目前为日志，可扩展为数据库保存）
            recordDeadLetter(message, messageBody);

            // 发送告警
            sendAlert(message, messageBody);

            // 确认消息，防止死信队列堆积
            channel.basicAck(deliveryTag, false);
            log.error("死信消息已处理，ACK确认");
            log.error("=================================================");

        } catch (Exception e) {
            log.error("处理死信消息失败: {}", e.getMessage());
            log.error("处理异常详情", e);
            try {
                // 死信处理失败时不再重新入队（避免死循环），直接拒绝但不发送回队列
                channel.basicNack(deliveryTag, false, false);
                log.error("死信消息已拒绝（不重新入队），防止死循环");
            } catch (Exception ex) {
                log.error("死信消息确认失败: {}", ex.getMessage());
            }
        }
    }

    /**
     * 记录死信消息到日志
     *
     * 实际系统应实现:
     * - 保存到 MySQL dead_letter_message 表，包含:
     *   - message_id: 原始消息ID
     *   - message_body: 消息体内容
     *   - original_queue: 原始队列名
     *   - retry_count: 重试次数
     *   - failure_reason: 失败原因
     *   - created_at: 创建时间
     *   - first_failure_time: 首次失败时间
     *   - last_failure_time: 最后失败时间
     */
    private void recordDeadLetter(Message message, String messageBody) {
        try {
            // 这里仅记录到日志，若要保存到数据库，需要:
            // 1. 创建 DeadLetterMessage 实体类
            // 2. 创建 DeadLetterMessageRepository 接口
            // 3. 调用 save 方法持久化

            // 示例日志记录（实际系统应持久化）:
            log.warn("死信消息已记录，需人工排查: body={}", messageBody);

            // TODO: 调用 deadLetterMessageService.saveDlxMessage(message)
            // TODO: 实现数据库持久化

        } catch (Exception e) {
            log.error("保存死信消息失败: {}", e.getMessage());
            log.debug("保存异常详情", e);
            // 保存失败不影响消息确认，消息已从队列移出
        }
    }

    /**
     * 发送告警通知
     *
     * 实际系统应实现:
     * - 发送邮件告警给运营团队
     * - 发送钉钉/企业微信告警
     * - 记录到告警系统（如 PagerDuty）
     *
     * 关键信息:
     * - 死信队列中有消息堆积 → 系统有故障
     * - 需要立即人工介入处理
     */
    private void sendAlert(Message message, String messageBody) {
        try {
            String alertMessage = String.format(
                    "系统告警: MQ死信队列收到消息，需要人工排查处理。" +
                    "消息内容: %s, 时间: %s",
                    messageBody, dateFormat.format(new Date())
            );

            // 这里仅记录告警日志
            log.error("告警内容: {}", alertMessage);

            // TODO: 集成实际的告警通知
            // - emailService.sendAlert(alertMessage);
            // - dingTalkService.sendAlert(alertMessage);
            // - alertSystemService.logAlert(alertMessage);

        } catch (Exception e) {
            log.error("发送告警失败: {}", e.getMessage());
            // 告警失败不影响消息确认
        }
    }
}
