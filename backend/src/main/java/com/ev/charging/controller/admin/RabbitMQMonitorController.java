package com.ev.charging.controller.admin;

import com.ev.charging.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * RabbitMQ监控管理控制器
 */
@RestController
@RequestMapping("/admin/mq")
@Tag(name = "RabbitMQ监控", description = "消息队列监控和管理接口")
@RequiredArgsConstructor
@Slf4j
public class RabbitMQMonitorController {

    private final AmqpAdmin amqpAdmin;

    @GetMapping("/stats")
    @Operation(summary = "获取队列统计信息")
    public Result<List<QueueStats>> getQueueStats() {
        List<QueueStats> statsList = new ArrayList<>();

        String[] queues = {
            "order.completed",
            "order.paid",
            "credit.change",
            "notification",
            "dlx.queue"
        };

        for (String queueName : queues) {
            try {
                QueueInformation info = amqpAdmin.getQueueInfo(queueName);
                if (info != null) {
                    QueueStats stats = QueueStats.builder()
                            .queueName(queueName)
                            .messageCount(info.getMessageCount())
                            .consumerCount(info.getConsumerCount())
                            .build();
                    statsList.add(stats);
                }
            } catch (Exception e) {
                log.warn("获取队列信息失败: {}", queueName, e);
            }
        }

        return Result.success(statsList);
    }

    @PostMapping("/purge/{queueName}")
    @Operation(summary = "清空队列")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> purgeQueue(@PathVariable String queueName) {
        amqpAdmin.purgeQueue(queueName);
        log.info("队列已清空: {}", queueName);
        return Result.success(null);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueueStats {
        private String queueName;
        private Integer messageCount;
        private Integer consumerCount;
    }
}
