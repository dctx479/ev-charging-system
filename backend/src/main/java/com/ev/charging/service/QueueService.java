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

    // 排队状态常量
    private static final byte STATUS_QUEUING = 0;    // 排队中
    private static final byte STATUS_CALLED = 1;     // 已叫号
    private static final byte STATUS_EXPIRED = 2;    // 已过号
    private static final byte STATUS_CANCELLED = 3;  // 已取消

    // 叫号后的等待时间（分钟）
    private static final int CALL_TIMEOUT_MINUTES = 15;

    // 预估平均充电时长（分钟）
    private static final int AVERAGE_CHARGE_DURATION = 30;

    /**
     * 加入排队
     *
     * @param userId 用户ID
     * @param dto    请求参数
     * @return 排队记录ID
     */
    @Transactional
    public Long joinQueue(Long userId, JoinQueueDTO dto) {
        Long stationId = dto.getStationId();

        // 1. 检查充电站是否存在
        ChargingStation station = stationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("充电站不存在"));

        if (!"ACTIVE".equals(station.getStatus())) {
            throw new IllegalArgumentException("该充电站暂停营业");
        }

        // 2. 检查用户是否已在该站点排队
        Optional<QueueRecord> existingQueue = queueRecordRepository.findByUserIdAndStationIdAndStatusIn(
                userId, stationId, Arrays.asList(STATUS_QUEUING, STATUS_CALLED)
        );

        if (existingQueue.isPresent()) {
            throw new IllegalArgumentException("您已在该站点排队，请勿重复排队");
        }

        // 3. 检查是否有空闲充电桩
        long availablePiles = pileRepository.countByStationIdAndStatus(stationId, "AVAILABLE");
        if (availablePiles > 0) {
            throw new IllegalArgumentException("当前有空闲充电桩，无需排队，请直接开始充电");
        }

        // 4. 生成排队号
        String queueNo = generateQueueNo(stationId);

        // 5. 计算当前队列位置和预计等待时间
        long currentQueueCount = queueRecordRepository.countByStationIdAndStatus(stationId, STATUS_QUEUING);
        int queuePosition = (int) currentQueueCount + 1;
        int estimatedWaitTime = calculateEstimatedWaitTime(stationId, queuePosition);

        // 6. 创建排队记录
        QueueRecord queueRecord = QueueRecord.builder()
                .userId(userId)
                .stationId(stationId)
                .queueNo(queueNo)
                .queuePosition(queuePosition)
                .estimatedWaitTime(estimatedWaitTime)
                .status(STATUS_QUEUING)
                .joinTime(LocalDateTime.now())
                .build();

        QueueRecord saved = queueRecordRepository.save(queueRecord);

        log.info("用户{}加入站点{}排队，排队号：{}, 位置：{}", userId, stationId, queueNo, queuePosition);

        return saved.getId();
    }

    /**
     * 获取我的排队状态
     *
     * @param userId 用户ID
     * @return 排队状态
     */
    public QueueStatusVO getQueueStatus(Long userId) {
        // 查询用户当前的排队记录（排队中或已叫号）
        Optional<QueueRecord> recordOpt = queueRecordRepository.findByUserIdAndStatusIn(
                userId, Arrays.asList(STATUS_QUEUING, STATUS_CALLED)
        );

        if (recordOpt.isEmpty()) {
            throw new IllegalArgumentException("您当前没有排队记录");
        }

        QueueRecord record = recordOpt.get();

        // 如果是排队中，需要重新计算位置
        if (record.getStatus() == STATUS_QUEUING) {
            updateQueuePosition(record);
        }

        return convertToQueueStatusVO(record);
    }

    /**
     * 离开队列
     *
     * @param userId 用户ID
     */
    @Transactional
    public void leaveQueue(Long userId) {
        Optional<QueueRecord> recordOpt = queueRecordRepository.findByUserIdAndStatusIn(
                userId, Arrays.asList(STATUS_QUEUING, STATUS_CALLED)
        );

        if (recordOpt.isEmpty()) {
            throw new IllegalArgumentException("您当前没有排队记录");
        }

        QueueRecord record = recordOpt.get();
        record.setStatus(STATUS_CANCELLED);
        queueRecordRepository.save(record);

        log.info("用户{}离开队列，排队号：{}", userId, record.getQueueNo());

        // 更新后续排队者的位置
        updateSubsequentQueuePositions(record.getStationId(), record.getJoinTime());
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
        long queueCount = queueRecordRepository.countByStationIdAndStatus(stationId, STATUS_QUEUING);

        // 查询可用充电桩数量
        long availablePiles = pileRepository.countByStationIdAndStatus(stationId, "AVAILABLE");

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
     * 当充电桩变为可用时，自动叫号
     *
     * @param stationId 充电站ID
     * @param pileId    充电桩ID
     */
    @Transactional
    public void callNext(Long stationId, Long pileId) {
        // 查询该站点排队中的第一个人
        List<QueueRecord> queueList = queueRecordRepository.findByStationIdAndStatusOrderByJoinTimeAsc(
                stationId, STATUS_QUEUING
        );

        if (queueList.isEmpty()) {
            log.info("站点{}暂无排队，充电桩{}空闲", stationId, pileId);
            return;
        }

        QueueRecord firstInQueue = queueList.get(0);

        // 更新状态为已叫号
        firstInQueue.setStatus(STATUS_CALLED);
        firstInQueue.setCallTime(LocalDateTime.now());
        firstInQueue.setExpireTime(LocalDateTime.now().plusMinutes(CALL_TIMEOUT_MINUTES));
        firstInQueue.setPileId(pileId);

        queueRecordRepository.save(firstInQueue);

        log.info("叫号成功：排队号{}，用户{}，充电桩{}", firstInQueue.getQueueNo(), firstInQueue.getUserId(), pileId);

        // TODO: 发送通知给用户（可通过WebSocket或短信）

        // 更新后续排队者的位置
        updateSubsequentQueuePositions(stationId, firstInQueue.getJoinTime());
    }

    /**
     * 定时任务：检查过号
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkExpiredCalls() {
        List<QueueRecord> expiredRecords = queueRecordRepository.findExpiredRecords(
                LocalDateTime.now(), STATUS_CALLED
        );

        for (QueueRecord record : expiredRecords) {
            record.setStatus(STATUS_EXPIRED);
            queueRecordRepository.save(record);

            log.warn("排队记录过号：排队号{}，用户{}", record.getQueueNo(), record.getUserId());

            // 如果有分配的充电桩，重新叫号
            if (record.getPileId() != null) {
                callNext(record.getStationId(), record.getPileId());
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
        long availablePiles = pileRepository.countByStationIdAndStatus(stationId, "AVAILABLE");

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
        List<QueueRecord> beforeRecords = queueRecordRepository.findByStationIdAndStatusInOrderByJoinTimeAsc(
                record.getStationId(), Arrays.asList(STATUS_QUEUING)
        );

        int position = 1;
        for (QueueRecord qr : beforeRecords) {
            if (qr.getJoinTime().isBefore(record.getJoinTime())) {
                position++;
            }
        }

        record.setQueuePosition(position);
        record.setEstimatedWaitTime(calculateEstimatedWaitTime(record.getStationId(), position));
        queueRecordRepository.save(record);
    }

    /**
     * 更新后续排队者的位置
     *
     * @param stationId 充电站ID
     * @param afterTime 在此时间之后加入的排队者
     */
    private void updateSubsequentQueuePositions(Long stationId, LocalDateTime afterTime) {
        List<QueueRecord> subsequentRecords = queueRecordRepository.findByStationIdAndStatusOrderByJoinTimeAsc(
                stationId, STATUS_QUEUING
        );

        int position = 1;
        for (QueueRecord record : subsequentRecords) {
            record.setQueuePosition(position);
            record.setEstimatedWaitTime(calculateEstimatedWaitTime(stationId, position));
            queueRecordRepository.save(record);
            position++;
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
        if (record.getStatus() == STATUS_CALLED) {
            peopleAhead = 0;
        }

        // 判断是否即将过号
        boolean willExpireSoon = false;
        if (record.getStatus() == STATUS_CALLED && record.getExpireTime() != null) {
            Duration duration = Duration.between(LocalDateTime.now(), record.getExpireTime());
            willExpireSoon = duration.toMinutes() <= 5;
        }

        return QueueStatusVO.builder()
                .id(record.getId())
                .queueNo(record.getQueueNo())
                .stationId(record.getStationId())
                .stationName(station != null ? station.getName() : "未知站点")
                .queuePosition(record.getQueuePosition())
                .peopleAhead(peopleAhead)
                .estimatedWaitTime(record.getEstimatedWaitTime())
                .status(record.getStatus())
                .statusText(getStatusText(record.getStatus()))
                .joinTime(record.getJoinTime())
                .callTime(record.getCallTime())
                .expireTime(record.getExpireTime())
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
        switch (status) {
            case STATUS_QUEUING:
                return "排队中";
            case STATUS_CALLED:
                return "已叫号";
            case STATUS_EXPIRED:
                return "已过号";
            case STATUS_CANCELLED:
                return "已取消";
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
