package com.ev.charging.controller.admin;

import com.ev.charging.common.Result;
import com.ev.charging.entity.ChargeOrder;
import com.ev.charging.entity.CreditPendingRecord;
import com.ev.charging.entity.User;
import com.ev.charging.exception.BusinessException;
import com.ev.charging.repository.ChargeOrderRepository;
import com.ev.charging.repository.CreditPendingRecordRepository;
import com.ev.charging.repository.UserRepository;
import com.ev.charging.scheduler.CreditRetryScheduler;
import com.ev.charging.service.CarbonCreditService;
import com.ev.charging.vo.CreditPendingRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 碳积分补偿管理Controller
 * 提供管理员手动补发积分、查询待发放记录等功能
 */
@RestController
@RequestMapping("/admin/credit-compensation")
@RequiredArgsConstructor
@Slf4j
public class CreditCompensationController {

    private final CreditPendingRecordRepository creditPendingRecordRepository;
    private final CarbonCreditService carbonCreditService;
    private final CreditRetryScheduler creditRetryScheduler;
    private final ChargeOrderRepository orderRepository;
    private final UserRepository userRepository;

    /**
     * 查询所有待发放积分记录（分状态）
     *
     * @param status 状态（0-待重试 1-已成功 2-已放弃，null-全部）
     * @return 待发放记录列表
     */
    @GetMapping("/pending-records")
    public Result<List<CreditPendingRecordVO>> getPendingRecords(
            @RequestParam(required = false) Byte status
    ) {
        log.info("查询待发放积分记录，状态: {}", status);

        List<CreditPendingRecord> records;
        if (status != null) {
            records = creditPendingRecordRepository.findByStatusOrderByCreateTimeDesc(status);
        } else {
            records = creditPendingRecordRepository.findAll();
        }

        List<CreditPendingRecordVO> voList = records.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 查询用户的待发放积分记录
     *
     * @param userId 用户ID
     * @return 待发放记录列表
     */
    @GetMapping("/user/{userId}")
    public Result<List<CreditPendingRecordVO>> getUserPendingRecords(
            @PathVariable Long userId
    ) {
        log.info("查询用户待发放积分记录，用户ID: {}", userId);

        List<CreditPendingRecord> records = creditPendingRecordRepository
                .findByUserIdOrderByCreateTimeDesc(userId);

        List<CreditPendingRecordVO> voList = records.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 管理员手动补发积分（单条记录）
     *
     * @param recordId 待发放记录ID
     * @return 操作结果
     */
    @PostMapping("/manual-issue/{recordId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> manualIssueCredit(@PathVariable Long recordId) {
        log.info("管理员手动补发积分，记录ID: {}", recordId);

        CreditPendingRecord record = creditPendingRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("待发放记录不存在"));

        // 检查记录状态
        if (record.getStatus() == 1) {
            return Result.error("该记录已成功发放，无需重复处理");
        }

        try {
            // 尝试发放积分
            Integer credits = carbonCreditService.addCreditsForCharging(
                    record.getOrderId(),
                    record.getUserId(),
                    record.getChargeAmount()
            );

            // 更新记录状态为"已成功"
            record.setStatus((byte) 1);
            record.setLastError(null);
            creditPendingRecordRepository.save(record);

            log.info("手动补发积分成功: recordId={}, orderId={}, credits={}",
                    recordId, record.getOrderId(), credits);

            return Result.success("积分补发成功，已发放 " + credits + " 积分");

        } catch (Exception e) {
            log.error("手动补发积分失败: recordId={}", recordId, e);

            // 更新错误信息
            record.setLastError(e.getMessage() != null && e.getMessage().length() > 500
                    ? e.getMessage().substring(0, 500)
                    : e.getMessage());
            creditPendingRecordRepository.save(record);

            return Result.error("积分补发失败，请稍后重试");
        }
    }

    /**
     * 批量手动补发积分（已放弃的记录）
     *
     * @return 操作结果
     */
    @PostMapping("/batch-issue-abandoned")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> batchIssueAbandonedRecords() {
        log.info("管理员批量补发已放弃的积分记录");

        List<CreditPendingRecord> abandonedRecords = creditPendingRecordRepository
                .findByStatusOrderByCreateTimeDesc((byte) 2);

        if (abandonedRecords.isEmpty()) {
            return Result.success("没有需要处理的已放弃记录");
        }

        int successCount = 0;
        int failedCount = 0;

        for (CreditPendingRecord record : abandonedRecords) {
            try {
                Integer credits = carbonCreditService.addCreditsForCharging(
                        record.getOrderId(),
                        record.getUserId(),
                        record.getChargeAmount()
                );

                record.setStatus((byte) 1);
                record.setLastError(null);
                creditPendingRecordRepository.save(record);

                successCount++;
                log.info("批量补发积分成功: recordId={}, orderId={}, credits={}",
                        record.getId(), record.getOrderId(), credits);

            } catch (Exception e) {
                failedCount++;
                log.error("批量补发积分失败: recordId={}", record.getId(), e);

                record.setLastError(e.getMessage() != null && e.getMessage().length() > 500
                        ? e.getMessage().substring(0, 500)
                        : e.getMessage());
                creditPendingRecordRepository.save(record);
            }
        }

        String message = String.format("批量补发完成：总计 %d 条，成功 %d 条，失败 %d 条",
                abandonedRecords.size(), successCount, failedCount);

        log.info(message);
        return Result.success(message);
    }

    /**
     * 手动触发重试定时任务
     *
     * @return 操作结果
     */
    @PostMapping("/trigger-retry")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> triggerRetry() {
        log.info("管理员手动触发积分重试任务");

        creditRetryScheduler.retryNow();
        return Result.success("重试任务已触发，请稍后查看结果");
    }

    /**
     * 删除待发放记录（谨慎操作）
     *
     * @param recordId 记录ID
     * @return 操作结果
     */
    @DeleteMapping("/{recordId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteRecord(@PathVariable Long recordId) {
        log.warn("管理员删除待发放积分记录，记录ID: {}", recordId);

        CreditPendingRecord record = creditPendingRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("待发放记录不存在"));

        if (record.getStatus() == 1) {
            return Result.error("该记录已成功发放，不允许删除");
        }

        creditPendingRecordRepository.delete(record);
        log.info("待发放记录已删除: recordId={}", recordId);

        return Result.success("记录已删除");
    }

    /**
     * 获取统计数据
     *
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public Result<Object> getStatistics() {
        long pendingCount = creditPendingRecordRepository.countByStatus((byte) 0);
        long successCount = creditPendingRecordRepository.countByStatus((byte) 1);
        long abandonedCount = creditPendingRecordRepository.countByStatus((byte) 2);

        return Result.success(new Object() {
            public final long pending = pendingCount;
            public final long success = successCount;
            public final long abandoned = abandonedCount;
            public final long total = pendingCount + successCount + abandonedCount;
        });
    }

    /**
     * 转换为VO
     */
    private CreditPendingRecordVO convertToVO(CreditPendingRecord record) {
        // 查询订单信息
        ChargeOrder order = orderRepository.findById(record.getOrderId()).orElse(null);

        // 查询用户信息
        User user = userRepository.findById(record.getUserId()).orElse(null);

        // 计算应发放积分数
        int creditsToIssue = (int) (record.getChargeAmount().doubleValue() * 0.785 * 10);

        return CreditPendingRecordVO.builder()
                .id(record.getId())
                .orderId(record.getOrderId())
                .orderNo(order != null ? order.getOrderNo() : "未知订单")
                .userId(record.getUserId())
                .userNickname(user != null ? user.getNickname() : "未知用户")
                .chargeAmount(record.getChargeAmount())
                .creditsToIssue(creditsToIssue)
                .retryCount(record.getRetryCount())
                .lastError(record.getLastError())
                .status(record.getStatus())
                .statusText(CreditPendingRecordVO.getStatusText(record.getStatus()))
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }
}
