package com.ev.charging.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ配置类
 */
@Configuration
@Slf4j
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "order.topic";
    public static final String CHARGE_EXCHANGE = "charge.topic";
    public static final String USER_EXCHANGE = "user.topic";
    public static final String NOTIFICATION_EXCHANGE = "notification.topic";
    public static final String DLX_EXCHANGE = "dlx.exchange";

    public static final String ORDER_COMPLETED_QUEUE = "order.completed";
    public static final String ORDER_PAID_QUEUE = "order.paid";
    public static final String CREDIT_CHANGE_QUEUE = "credit.change";
    public static final String NOTIFICATION_QUEUE = "notification";
    public static final String DLX_QUEUE = "dlx.queue";

    public static final String ORDER_COMPLETED_KEY = "order.completed";
    public static final String ORDER_PAID_KEY = "order.paid";
    public static final String CREDIT_CHANGE_KEY = "credit.change";
    public static final String NOTIFICATION_KEY = "notification";

    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.findAndRegisterModules();
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        template.setMandatory(true);

        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("消息发送成功: correlationData={}", correlationData);
            } else {
                log.error("消息发送失败: correlationData={}, cause={}", correlationData, cause);
            }
        });
        
        template.setReturnsCallback(returned -> {
            log.error("消息无法路由: message={}, exchange={}, routingKey={}",
                    returned.getMessage(), returned.getExchange(), returned.getRoutingKey());
        });
        
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(1);
        // 防止反序列化失败导致消息无限重投
        factory.setErrorHandler(t -> {
            if (t.getCause() instanceof MessageConversionException) {
                log.error("MQ消息反序列化失败，拒绝并不重新入队: {}", t.getMessage());
                throw new AmqpRejectAndDontRequeueException("Deserialization failed", t);
            }
            throw t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
        });
        return factory;
    }

    @Bean
    public TopicExchange orderExchange() {
        return ExchangeBuilder.topicExchange(ORDER_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange chargeExchange() {
        return ExchangeBuilder.topicExchange(CHARGE_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange userExchange() {
        return ExchangeBuilder.topicExchange(USER_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange notificationExchange() {
        return ExchangeBuilder.topicExchange(NOTIFICATION_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange(DLX_EXCHANGE).durable(true).build();
    }

    private Map<String, Object> getQueueArgs() {
        Map<String, Object> args = new HashMap<>();
        // 消息 TTL: 24小时（86400000ms）
        // 防止死消息长期堆积在队列中
        args.put("x-message-ttl", 86400000);
        // 死信交换机配置，过期消息转发到 DLX
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", "dlx");
        return args;
    }

    @Bean
    public Queue orderCompletedQueue() {
        return QueueBuilder.durable(ORDER_COMPLETED_QUEUE).withArguments(getQueueArgs()).build();
    }

    @Bean
    public Queue orderPaidQueue() {
        return QueueBuilder.durable(ORDER_PAID_QUEUE).withArguments(getQueueArgs()).build();
    }

    @Bean
    public Queue creditChangeQueue() {
        return QueueBuilder.durable(CREDIT_CHANGE_QUEUE).withArguments(getQueueArgs()).build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).withArguments(getQueueArgs()).build();
    }

    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(DLX_QUEUE).build();
    }

    @Bean
    public Binding orderCompletedBinding() {
        return BindingBuilder.bind(orderCompletedQueue()).to(orderExchange()).with(ORDER_COMPLETED_KEY);
    }

    @Bean
    public Binding orderPaidBinding() {
        return BindingBuilder.bind(orderPaidQueue()).to(orderExchange()).with(ORDER_PAID_KEY);
    }

    @Bean
    public Binding creditChangeBinding() {
        return BindingBuilder.bind(creditChangeQueue()).to(orderExchange()).with(CREDIT_CHANGE_KEY);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(notificationExchange()).with(NOTIFICATION_KEY);
    }

    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(dlxExchange()).with("dlx");
    }
}
