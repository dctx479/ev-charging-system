package com.ev.charging.controller.admin;

import com.ev.charging.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * RabbitMQ监控管理控制器
 */
@RestController
@RequestMapping("/admin/mq")
@Tag(name = "RabbitMQ监控", description = "消息队列监控和管理接口")
@Slf4j
public class RabbitMQMonitorController {

    @Autowired
    private AmqpAdmin amqpAdmin;

    @GetMapping("/stats")
    @Operation(summary = "获取队列统计信息")
    public Result<List<QueueStats>> getQueueStats() {
        try {
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
        } catch (Exception e) {
            log.error("获取队列统计失败", e);
            return Result.error("获取队列统计失败，请稍后重试");
        }
    }

    @PostMapping("/purge/{queueName}")
    @Operation(summary = "清空队列")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> purgeQueue(@PathVariable String queueName) {
        try {
            amqpAdmin.purgeQueue(queueName);
            log.info("队列已清空: {}", queueName);
            return Result.success(null);
        } catch (Exception e) {
            log.error("清空队列失败: {}", queueName, e);
            return Result.error("清空队列失败，请稍后重试");
        }
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
