package com.ev.charging.repository;

import com.ev.charging.entity.CreditPendingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 积分待发放记录Repository
 */
@Repository
public interface CreditPendingRecordRepository extends JpaRepository<CreditPendingRecord, Long> {

    /**
     * 查询待重试的积分记录（状态为0且重试次数<3）
     *
     * @return 待重试的记录列表
     */
    @Query("SELECT p FROM CreditPendingRecord p WHERE p.status = 0 AND p.retryCount < 3 ORDER BY p.createTime ASC")
    List<CreditPendingRecord> findPendingRetryRecords();

    /**
     * 根据订单ID查询待发放记录
     *
     * @param orderId 订单ID
     * @return 待发放记录
     */
    Optional<CreditPendingRecord> findByOrderId(Long orderId);

    /**
     * 查询用户的待发放积分记录
     *
     * @param userId 用户ID
     * @param status 状态（可选）
     * @return 待发放记录列表
     */
    List<CreditPendingRecord> findByUserIdAndStatusOrderByCreateTimeDesc(Long userId, Byte status);

    /**
     * 查询用户的所有待发放积分记录
     *
     * @param userId 用户ID
     * @return 待发放记录列表
     */
    List<CreditPendingRecord> findByUserIdOrderByCreateTimeDesc(Long userId);

    /**
     * 查询已放弃的记录（需要人工处理）
     *
     * @return 已放弃的记录列表
     */
    List<CreditPendingRecord> findByStatusOrderByCreateTimeDesc(Byte status);

    /**
     * 按状态统计记录数（用于统计接口，避免全量加载）
     *
     * @param status 状态
     * @return 记录数
     */
    long countByStatus(Byte status);
}
