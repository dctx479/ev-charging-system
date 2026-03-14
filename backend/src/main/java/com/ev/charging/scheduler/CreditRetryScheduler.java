package com.ev.charging.scheduler;

import com.ev.charging.entity.CreditPendingRecord;
import com.ev.charging.repository.CreditPendingRecordRepository;
import com.ev.charging.service.CarbonCreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 积分重试定时任务
 * 每5分钟执行一次，尝试重新发放失败的碳积分
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CreditRetryScheduler {

    private final CreditPendingRecordRepository creditPendingRecordRepository;

    private final CarbonCreditService carbonCreditService;

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 定时任务：每5分钟执行一次
     * 尝试重新发放失败的碳积分
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 每次执行完成后等待5分钟再执行下一次
    public void retryPendingCredits() {
        try {
            doRetryPendingCredits();
        } catch (Exception e) {
            log.error("积分重试定时任务异常，调度器线程安全退出", e);
        }
    }

    private void doRetryPendingCredits() {
        log.info("开始执行积分重试定时任务...");

        // 1. 查找状态为"待重试"且重试次数<3的记录
        List<CreditPendingRecord> pendingRecords = creditPendingRecordRepository.findPendingRetryRecords();

        if (pendingRecords.isEmpty()) {
            log.debug("没有待重试的积分记录");
            return;
        }

        log.info("找到 {} 条待重试的积分记录", pendingRecords.size());

        int successCount = 0;
        int failedCount = 0;
        int abandonedCount = 0;

        // 2. 逐条尝试发放积分
        for (CreditPendingRecord record : pendingRecords) {
            try {
                log.info("尝试重试积分发放: recordId={}, orderId={}, userId={}, retryCount={}",
                        record.getId(), record.getOrderId(), record.getUserId(), record.getRetryCount());

                // 尝试发放积分
                Integer credits = carbonCreditService.addCreditsForCharging(
                        record.getOrderId(),
                        record.getUserId(),
                        record.getChargeAmount()
                );

                // 3. 成功则更新状态为"已成功"
                record.setStatus((byte) 1);
                record.setLastError(null);
                creditPendingRecordRepository.save(record);

                successCount++;
                log.info("积分发放成功: recordId={}, orderId={}, credits={}",
                        record.getId(), record.getOrderId(), credits);

            } catch (Exception e) {
                // 4. 失败则增加重试次数
                record.setRetryCount(record.getRetryCount() + 1);
                record.setLastError(e.getMessage() != null && e.getMessage().length() > 500
                        ? e.getMessage().substring(0, 500)
                        : e.getMessage());

                // 5. 超过最大重试次数，标记为"已放弃"
                if (record.getRetryCount() >= MAX_RETRY_COUNT) {
                    record.setStatus((byte) 2);
                    abandonedCount++;
                    log.error("积分发放失败，已超过最大重试次数，标记为已放弃: recordId={}, orderId={}, retryCount={}",
                            record.getId(), record.getOrderId(), record.getRetryCount(), e);
                } else {
                    failedCount++;
                    log.warn("积分发放失败，将在下次重试: recordId={}, orderId={}, retryCount={}, error={}",
                            record.getId(), record.getOrderId(), record.getRetryCount(), e.getMessage());
                }

                creditPendingRecordRepository.save(record);
            }
        }

        log.info("积分重试定时任务执行完成: 总计={}, 成功={}, 失败={}, 放弃={}",
                pendingRecords.size(), successCount, failedCount, abandonedCount);

        // 如果有已放弃的记录，发出告警
        if (abandonedCount > 0) {
            log.error("!!!注意!!! 有 {} 条积分记录已放弃，需要人工处理", abandonedCount);
            // TODO: 可以在这里发送告警通知（邮件、短信、钉钉等）
        }
    }

    /**
     * 立即执行重试任务（供手动触发）
     */
    public void retryNow() {
        log.info("手动触发积分重试任务");
        try {
            doRetryPendingCredits();
        } catch (Exception e) {
            log.error("手动积分重试任务异常", e);
        }
    }
}
