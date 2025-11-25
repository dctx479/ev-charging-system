package com.ev.charging.service;

import com.ev.charging.dto.JoinQueueDTO;
import com.ev.charging.entity.ChargingPile;
import com.ev.charging.entity.ChargingStation;
import com.ev.charging.entity.QueueRecord;
import com.ev.charging.repository.ChargingPileRepository;
import com.ev.charging.repository.ChargingStationRepository;
import com.ev.charging.repository.QueueRecordRepository;
import com.ev.charging.vo.QueueStatusVO;
import com.ev.charging.vo.StationQueueInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 排队服务层
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final QueueRecordRepository queueRecordRepository;
    private final ChargingStationRepository stationRepository;
    private final ChargingPileRepository pileRepository;
    private final OrderService orderService;
    private final com.ev.charging.util.RedisLockService redisLockService;

    /**
     * 自注入代理，用于让 checkExpiredCalls 正确触发 callNext 的 @Transactional AOP 代理，
     * 避免 this.callNext() 绕过 Spring 代理导致事务不生效的问题。
     */
    @Autowired
    @Lazy
    private QueueService self;

    // 排队状态常量（对应 queue_status 字段：1排队中 2已分配 3已取消 4超时）
    private static final byte STATUS_QUEUING = 1;    // 排队中
    private static final byte STATUS_ASSIGNED = 2;   // 已分配（已叫号）
    private static final byte STATUS_CANCELLED = 3;  // 已取消
    private static final byte STATUS_EXPIRED = 4;    // 超时

    // 叫号后的等待时间（分钟）
    private static final int CALL_TIMEOUT_MINUTES = 15;

    // 预估平均充电时长（分钟）
    private static final int AVERAGE_CHARGE_DURATION = 30;

    /**
     * 加入排队
     * 使用分布式锁防止并发排队和重复排队。
     * 注意：此方法无 @Transactional，分布式锁本身保证了临界区互斥。
     * 若保留 @Transactional，锁在 finally 中释放后事务尚未提交，存在短暂的脏读窗口。
     *
     * @param userId 用户ID
     * @param dto    请求参数
     * @return 排队记录ID
     */
    public Long joinQueue(Long userId, JoinQueueDTO dto) {
        Long stationId = dto.getStationId();

        // 1. 用户锁：防止同一用户并发排队（owner-safe，防止锁过期后误删）
        String userLockKey = com.ev.charging.util.RedisLockService.buildUserQueueLockKey(userId);
        String userOwner = redisLockService.tryLockWithOwner(userLockKey, 10);
        if (userOwner == null) {
            throw new IllegalArgumentException("请勿频繁操作，请稍后再试");
        }

        try {
            // 2. 站点排队锁：防止并发修改队列
            String stationLockKey = com.ev.charging.util.RedisLockService.buildStationQueueLockKey(stationId);
            String stationOwner = redisLockService.tryLockWithOwner(stationLockKey, 10);
            if (stationOwner == null) {
                throw new IllegalArgumentException("当前排队人数较多，请稍后再试");
            }

            try {
                // 3. 检查充电站是否存在
                ChargingStation station = stationRepository.findById(stationId)
                        .orElseThrow(() -> new IllegalArgumentException("充电站不存在"));

                if (station.getStatus() == null || station.getStatus() != 1) {
                    throw new IllegalArgumentException("该充电站暂停营业");
                }

                // 4. 检查用户是否已在该站点排队
                Optional<QueueRecord> existingQueue = queueRecordRepository.findByUserIdAndStationIdAndQueueStatusIn(
                        userId, stationId, Arrays.asList(STATUS_QUEUING, STATUS_ASSIGNED)
                );

                if (existingQueue.isPresent()) {
                    throw new IllegalArgumentException("您已在该站点排队，请勿重复排队");
                }

                // 5. 检查是否有空闲充电桩
                long availablePiles = pileRepository.countByStationIdAndStatus(stationId, (byte) 1);
                if (availablePiles > 0) {
                    throw new IllegalArgumentException("当前有空闲充电桩，无需排队，请直接开始充电");
                }

                // 6. 生成排队号
                String queueNo = generateQueueNo(stationId);

                // 7. 计算当前队列位置和预计等待时间
                long currentQueueCount = queueRecordRepository.countByStationIdAndQueueStatus(stationId, STATUS_QUEUING);
                int queuePosition = (int) currentQueueCount + 1;
                int estimatedWaitTime = calculateEstimatedWaitTime(stationId, queuePosition);

                // 8. 创建排队记录
                QueueRecord queueRecord = QueueRecord.builder()
                        .userId(userId)
                        .stationId(stationId)
                        .pileType(dto.getPileType())  // 从 DTO 获取充电类型
                        .queueNo(queueNo)
                        .queuePosition(queuePosition)
                        .estimatedWaitTime(estimatedWaitTime)
                        .queueStatus(STATUS_QUEUING)
                        .joinTime(LocalDateTime.now())
                        .build();

                try {
                    QueueRecord saved = queueRecordRepository.save(queueRecord);
                    log.info("用户{}加入站点{}排队，排队号：{}, 位置：{}", userId, stationId, queueNo, queuePosition);
                    return saved.getId();
                } catch (OptimisticLockingFailureException e) {
                    log.error("排队记录保存失败（版本冲突），用户：{}, 站点：{}", userId, stationId);
                    throw new IllegalArgumentException("操作冲突，请重试");
                }
            } finally {
                // 释放站点锁
                redisLockService.unlockSafe(stationLockKey, stationOwner);
            }
        } finally {
            // 释放用户锁
            redisLockService.unlockSafe(userLockKey, userOwner);
        }
    }

    /**
     * 获取我的排队状态
     *
     * @param userId 用户ID
     * @return 排队状态
     */
    public QueueStatusVO getQueueStatus(Long userId) {
        // 查询用户当前的排队记录（排队中或已分配）
        Optional<QueueRecord> recordOpt = queueRecordRepository.findByUserIdAndQueueStatusIn(
                userId, Arrays.asList(STATUS_QUEUING, STATUS_ASSIGNED)
        );

        if (recordOpt.isEmpty()) {
            throw new IllegalArgumentException("您当前没有排队记录");
        }

        QueueRecord record = recordOpt.get();

        // 如果是排队中，纯内存计算位置（GET不应写DB）
        if (record.getQueueStatus() == STATUS_QUEUING) {
            List<QueueRecord> beforeRecords = queueRecordRepository.findByStationIdAndQueueStatusOrderByJoinTimeAscIdAsc(
                    record.getStationId(), STATUS_QUEUING
            );
            int position = 1;
            for (QueueRecord qr : beforeRecords) {
                if (qr.getJoinTime().isBefore(record.getJoinTime())) {
                    position++;
                }
            }
            record.setQueuePosition(position);
            record.setEstimatedWaitTime(calculateEstimatedWaitTime(record.getStationId(), position));
        }

        return convertToQueueStatusVO(record);
    }

    /**
     * 离开队列
     *
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void leaveQueue(Long userId) {
        Optional<QueueRecord> recordOpt = queueRecordRepository.findByUserIdAndQueueStatusIn(
                userId, Arrays.asList(STATUS_QUEUING, STATUS_ASSIGNED)
        );

        if (recordOpt.isEmpty()) {
            throw new IllegalArgumentException("您当前没有排队记录");
        }

        QueueRecord record = recordOpt.get();
        record.setQueueStatus(STATUS_CANCELLED);
        record.setCancelTime(LocalDateTime.now());

        try {
            queueRecordRepository.save(record);
            log.info("用户{}离开队列，排队号：{}", userId, record.getQueueNo());

            // 更新后续排队者的位置
            updateSubsequentQueuePositions(record.getStationId(), record.getJoinTime());
        } catch (OptimisticLockingFailureException e) {
            log.error("离开队列失败（版本冲突），用户：{}, 排队号：{}", userId, record.getQueueNo());
            throw new IllegalArgumentException("操作冲突，请重试");
        }
    }

    /**
     * 获取站点排队信息
     *
     * @param stationId 充电站ID
     * @return 排队信息
     */
    public StationQueueInfoVO getStationQueueInfo(Long stationId) {
        ChargingStation station = stationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("充电站不存在"));

        // 统计排队人数
        long queueCount = queueRecordRepository.countByStationIdAndQueueStatus(stationId, STATUS_QUEUING);

        // 查询可用充电桩数量
        long availablePiles = pileRepository.countByStationIdAndStatus(stationId, (byte) 1);

        // 计算平均等待时间
        int averageWaitTime = 0;
        if (queueCount > 0 && availablePiles > 0) {
            averageWaitTime = (int) ((queueCount * AVERAGE_CHARGE_DURATION) / availablePiles);
        }

        // 判断是否建议排队
        boolean recommendQueue = queueCount < 10 && averageWaitTime < 60;
        String suggestion = getSuggestion(queueCount, availablePiles, averageWaitTime);

        return StationQueueInfoVO.builder()
                .stationId(stationId)
                .stationName(station.getName())
                .queueCount((int) queueCount)
                .availablePiles((int) availablePiles)
                .averageWaitTime(averageWaitTime)
                .recommendQueue(recommendQueue)
                .suggestion(suggestion)
                .build();
    }

    /**
     * 叫下一号（内部方法）
     * 当充电桩变为可用时，自动叫号并创建订单
     * 使用分布式锁防止并发叫号
     *
     * @param stationId 充电站ID
     * @param pileId    充电桩ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void callNext(Long stationId, Long pileId) {
        // 1. 站点排队锁：防止并发叫号（owner-safe）
        String stationLockKey = com.ev.charging.util.RedisLockService.buildStationQueueLockKey(stationId);
        String stationOwner = redisLockService.tryLockWithOwner(stationLockKey, 10);
        if (stationOwner == null) {
            log.warn("获取站点排队锁失败，跳过叫号：stationId={}", stationId);
            return;
        }

        try {
            // 注意：此处不再获取充电桩锁，因为 createOrderFromQueue() 内部已有桩锁。
            // 之前的实现会导致不可重入死锁（SETNX 非重入）。

            // 2. 查询该站点排队中的第一个人
            List<QueueRecord> queueList = queueRecordRepository.findByStationIdAndQueueStatusOrderByJoinTimeAscIdAsc(
                    stationId, STATUS_QUEUING
            );

            if (queueList.isEmpty()) {
                log.info("站点{}暂无排队，充电桩{}空闲", stationId, pileId);
                return;
            }

            QueueRecord firstInQueue = queueList.get(0);

            // 3. 更新状态为已分配
            firstInQueue.setQueueStatus(STATUS_ASSIGNED);
            firstInQueue.setAssignedTime(LocalDateTime.now());
            firstInQueue.setAssignedPileId(pileId);

            try {
                queueRecordRepository.save(firstInQueue);
                log.info("叫号成功：排队号{}，用户{}，充电桩{}", firstInQueue.getQueueNo(), firstInQueue.getUserId(), pileId);
            } catch (OptimisticLockingFailureException e) {
                log.warn("叫号失败（版本冲突），排队号：{}, 充电桩：{}", firstInQueue.getQueueNo(), pileId);
                return;
            }

            // 4. 自动创建订单
            try {
                Long orderId = orderService.createOrderFromQueue(firstInQueue.getUserId(), pileId);

                // 更新排队记录中的订单ID
                Optional<QueueRecord> refreshedOpt = queueRecordRepository.findById(firstInQueue.getId());
                if (refreshedOpt.isPresent()) {
                    QueueRecord refreshed = refreshedOpt.get();
                    refreshed.setOrderId(orderId);
                    try {
                        queueRecordRepository.save(refreshed);
                        log.info("自动创建订单成功：排队号{}，用户{}，订单ID{}",
                                firstInQueue.getQueueNo(), firstInQueue.getUserId(), orderId);
                    } catch (OptimisticLockingFailureException e) {
                        log.warn("更新排队记录失败（版本冲突），排队号：{}, 订单ID：{}", firstInQueue.getQueueNo(), orderId);
                    }
                }
            } catch (Exception e) {
                log.error("自动创建订单失败：排队号{}，用户{}，错误：{}",
                        firstInQueue.getQueueNo(), firstInQueue.getUserId(), e.getMessage(), e);
            }

            // 5. 发送通知给用户（设置已发送标志）
            try {
                Optional<QueueRecord> finalRecord = queueRecordRepository.findById(firstInQueue.getId());
                if (finalRecord.isPresent()) {
                    QueueRecord toNotify = finalRecord.get();
                    toNotify.setNotifySent((byte) 1);
                    queueRecordRepository.save(toNotify);
                    log.info("用户通知已发送：排队号{}", firstInQueue.getQueueNo());
                }
            } catch (OptimisticLockingFailureException e) {
                log.warn("更新通知标志失败（版本冲突），排队号：{}", firstInQueue.getQueueNo());
            }

            // 6. 更新后续排队者的位置
            updateSubsequentQueuePositions(stationId, firstInQueue.getJoinTime());
        } finally {
            // 释放站点锁
            redisLockService.unlockSafe(stationLockKey, stationOwner);
        }
    }

    /**
     * 定时任务：检查过号
     * 每分钟执行一次。
     * 注意：不加 @Transactional，避免定时任务持有跨越所有记录处理的长事务。
     * 每条记录的状态更新通过 save() 各自提交；callNext 通过 self 代理调用以保证其 @Transactional 生效。
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000) // 每次执行完成后等待1分钟再执行下一次
    public void checkExpiredCalls() {
        try {
            doCheckExpiredCalls();
        } catch (Exception e) {
            log.error("过号检查定时任务异常，调度器线程安全退出", e);
        }
    }

    private void doCheckExpiredCalls() {
        List<QueueRecord> expiredRecords = queueRecordRepository.findExpiredRecords(
                LocalDateTime.now().minusMinutes(CALL_TIMEOUT_MINUTES), STATUS_ASSIGNED
        );

        for (QueueRecord record : expiredRecords) {
            try {
                record.setQueueStatus(STATUS_EXPIRED);
                queueRecordRepository.save(record);
                log.warn("排队记录过号：排队号{}，用户{}", record.getQueueNo(), record.getUserId());

                // 如果有分配的充电桩，重新叫号
                // 使用 self 代理调用，保证 callNext 的 @Transactional 正确生效（避免 this 调用绕过 AOP 代理）
                if (record.getAssignedPileId() != null) {
                    self.callNext(record.getStationId(), record.getAssignedPileId());
                }
            } catch (OptimisticLockingFailureException e) {
                log.warn("过号处理失败（版本冲突），排队号：{}", record.getQueueNo());
                // 版本冲突时继续处理下一条记录
            }
        }
    }

    /**
     * 生成排队号
     * 格式：站点ID-日期-序号（例如：1-20250108-001）
     *
     * @param stationId 充电站ID
     * @return 排队号
     */
    private String generateQueueNo(Long stationId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = LocalDateTime.of(today, LocalTime.MIN);

        long todayCount = queueRecordRepository.countTodayQueue(stationId, startOfDay);

        String dateStr = today.toString().replace("-", "");
        String sequenceStr = String.format("%03d", todayCount + 1);

        return stationId + "-" + dateStr + "-" + sequenceStr;
    }

    /**
     * 计算预计等待时间
     *
     * @param stationId     充电站ID
     * @param queuePosition 队列位置
     * @return 预计等待时间（分钟）
     */
    private int calculateEstimatedWaitTime(Long stationId, int queuePosition) {
        // 查询可用充电桩数量
        long availablePiles = pileRepository.countByStationIdAndStatus(stationId, (byte) 1);

        if (availablePiles == 0) {
            availablePiles = 1; // 避免除零
        }

        // 简单算法：(队列位置 * 平均充电时长) / 充电桩数量
        return (int) ((queuePosition * AVERAGE_CHARGE_DURATION) / availablePiles);
    }

    /**
     * 更新队列位置
     *
     * @param record 排队记录
     */
    private void updateQueuePosition(QueueRecord record) {
        // 统计在该记录之前加入且仍在排队的人数
        List<QueueRecord> beforeRecords = queueRecordRepository.findByStationIdAndQueueStatusOrderByJoinTimeAscIdAsc(
                record.getStationId(), STATUS_QUEUING
        );

        int position = 1;
        for (QueueRecord qr : beforeRecords) {
            if (qr.getJoinTime().isBefore(record.getJoinTime())) {
                position++;
            }
        }

        record.setQueuePosition(position);
        record.setEstimatedWaitTime(calculateEstimatedWaitTime(record.getStationId(), position));
        try {
            queueRecordRepository.save(record);
        } catch (OptimisticLockingFailureException e) {
            log.warn("更新队列位置失败（版本冲突），排队号：{}", record.getQueueNo());
        }
    }

    /**
     * 更新站点内所有排队者的位置（离队或叫号后重新编号）
     * 使用 saveAll 批量写，避免 N+1 写问题。
     * 注意：对于批量更新，个别记录的乐观锁冲突不会导致整个操作失败，而是跳过该记录继续处理下一个。
     */
    private void updateSubsequentQueuePositions(Long stationId, LocalDateTime afterTime) {
        List<QueueRecord> queuingRecords = queueRecordRepository.findByStationIdAndQueueStatusOrderByJoinTimeAscIdAsc(
                stationId, STATUS_QUEUING
        );

        int position = 1;
        for (QueueRecord record : queuingRecords) {
            record.setQueuePosition(position);
            record.setEstimatedWaitTime(calculateEstimatedWaitTime(stationId, position));
            position++;
        }
        // 批量保存，避免循环内逐条写库
        try {
            queueRecordRepository.saveAll(queuingRecords);
        } catch (OptimisticLockingFailureException e) {
            log.warn("批量更新队列位置失败（版本冲突），站点ID：{}, 受影响记录数：{}", stationId, queuingRecords.size());
            // 可选：逐条重试
            for (QueueRecord record : queuingRecords) {
                try {
                    queueRecordRepository.save(record);
                } catch (OptimisticLockingFailureException ignored) {
                    log.debug("单条更新队列位置失败，排队号：{}", record.getQueueNo());
                }
            }
        }
    }

    /**
     * 转换为VO
     *
     * @param record 排队记录
     * @return QueueStatusVO
     */
    private QueueStatusVO convertToQueueStatusVO(QueueRecord record) {
        ChargingStation station = stationRepository.findById(record.getStationId()).orElse(null);

        // 计算前面排队人数
        int peopleAhead = record.getQueuePosition() - 1;
        if (record.getQueueStatus() == STATUS_ASSIGNED) {
            peopleAhead = 0;
        }

        // 计算过期时间：叫号时间 + 15分钟
        LocalDateTime expireTime = null;
        if (record.getAssignedTime() != null) {
            expireTime = record.getAssignedTime().plusMinutes(15);
        }

        // 计算是否即将过号：叫号后还剩5分钟以内
        boolean willExpireSoon = false;
        if (record.getAssignedTime() != null && expireTime != null) {
            LocalDateTime fiveMinsBeforeExpire = expireTime.minusMinutes(5);
            willExpireSoon = LocalDateTime.now().isAfter(fiveMinsBeforeExpire) &&
                            LocalDateTime.now().isBefore(expireTime);
        }

        return QueueStatusVO.builder()
                .id(record.getId())
                .queueNo(record.getQueueNo())
                .stationId(record.getStationId())
                .stationName(station != null ? station.getName() : "未知站点")
                .queuePosition(record.getQueuePosition())
                .peopleAhead(peopleAhead)
                .estimatedWaitTime(record.getEstimatedWaitTime())
                .status(record.getQueueStatus())
                .statusText(getStatusText(record.getQueueStatus()))
                .joinTime(record.getJoinTime())
                .callTime(record.getAssignedTime())
                .expireTime(expireTime)
                .pileId(record.getAssignedPileId())
                .willExpireSoon(willExpireSoon)
                .build();
    }

    /**
     * 获取状态文本
     *
     * @param status 状态码
     * @return 状态文本
     */
    private String getStatusText(Byte status) {
        if (status == null) return "未知状态";
        switch (status) {
            case STATUS_QUEUING:
                return "排队中";
            case STATUS_ASSIGNED:
                return "已分配";
            case STATUS_CANCELLED:
                return "已取消";
            case STATUS_EXPIRED:
                return "已超时";
            default:
                return "未知状态";
        }
    }

    /**
     * 获取排队建议
     *
     * @param queueCount      排队人数
     * @param availablePiles  可用充电桩数
     * @param averageWaitTime 平均等待时间
     * @return 建议文本
     */
    private String getSuggestion(long queueCount, long availablePiles, int averageWaitTime) {
        if (availablePiles > 0 && queueCount == 0) {
            return "当前无需排队，可直接充电";
        } else if (queueCount < 5) {
            return "排队人数较少，建议排队";
        } else if (queueCount < 10) {
            return "排队人数适中，预计等待" + averageWaitTime + "分钟";
        } else {
            return "排队人数较多，建议前往其他站点";
        }
    }
}
