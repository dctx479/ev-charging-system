package com.ev.charging.repository;

import com.ev.charging.entity.ChargeOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 充电订单Repository
 */
@Repository
public interface ChargeOrderRepository extends JpaRepository<ChargeOrder, Long>, JpaSpecificationExecutor<ChargeOrder> {

    /**
     * 根据订单号查询
     */
    Optional<ChargeOrder> findByOrderNo(String orderNo);

    /**
     * 根据用户ID查询订单列表（分页）
     */
    Page<ChargeOrder> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    /**
     * 根据用户ID和订单状态查询订单列表（分页）
     */
    Page<ChargeOrder> findByUserIdAndOrderStatusOrderByCreateTimeDesc(
            Long userId, Byte orderStatus, Pageable pageable);

    /**
     * 统计用户累计充电量
     */
    @Query("SELECT COALESCE(SUM(o.chargeAmount), 0) FROM ChargeOrder o WHERE o.userId = :userId AND o.orderStatus = 3")
    BigDecimal sumChargeAmountByUserId(@Param("userId") Long userId);

    /**
     * 统计用户充电次数
     */
    @Query("SELECT COUNT(o) FROM ChargeOrder o WHERE o.userId = :userId AND o.orderStatus = 3")
    Long countCompletedOrdersByUserId(@Param("userId") Long userId);

    /**
     * 查询用户进行中的订单
     */
    Optional<ChargeOrder> findByUserIdAndOrderStatus(Long userId, Byte orderStatus);

    /**
     * 查询充电桩的进行中订单
     */
    Optional<ChargeOrder> findByPileIdAndOrderStatus(Long pileId, Byte orderStatus);

    /**
     * 查询指定时间范围内的订单
     */
    List<ChargeOrder> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定时间范围内的订单总数
     */
    @Query("SELECT COUNT(o) FROM ChargeOrder o WHERE o.createTime BETWEEN :startTime AND :endTime")
    Long countByCreateTimeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的总收入
     */
    @Query("SELECT COALESCE(SUM(o.totalFee), 0) FROM ChargeOrder o WHERE o.createTime BETWEEN :startTime AND :endTime AND o.paymentStatus = 1")
    BigDecimal sumTotalFeeByCreateTimeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的总充电量
     */
    @Query("SELECT COALESCE(SUM(o.chargeAmount), 0) FROM ChargeOrder o WHERE o.createTime BETWEEN :startTime AND :endTime AND o.orderStatus = 3")
    BigDecimal sumChargeAmountByCreateTimeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查找充电超时的订单
     * 订单状态为"充电中"（orderStatus = 2）且开始时间早于指定时间点的订单
     */
    @Query("SELECT o FROM ChargeOrder o WHERE o.orderStatus = 2 AND o.startTime < :timeout")
    List<ChargeOrder> findChargingTimeoutOrders(@Param("timeout") LocalDateTime timeout);

    /**
     * 查找支付超时的订单
     * 订单状态为"已完成"（orderStatus = 3）且未支付且结束时间早于指定时间点的订单
     */
    @Query("SELECT o FROM ChargeOrder o WHERE o.orderStatus = 3 AND o.paymentStatus = 0 AND o.endTime < :timeout")
    List<ChargeOrder> findPaymentTimeoutOrders(@Param("timeout") LocalDateTime timeout);

    /**
     * 按订单状态统计数量
     */
    long countByOrderStatus(Byte orderStatus);

    /**
     * 按支付状态统计数量
     */
    long countByPaymentStatus(Byte paymentStatus);

    /**
     * Fix 3: 原子性更新支付状态
     * 并发安全的支付状态更新，防止ABA问题（check-then-act竞态）
     * 使用原子的 CAS（Compare-And-Set）操作：
     * 只有当前支付状态为 expectedStatus 时，才更新为 newStatus
     * 这避免了读-检查-写之间的竞态条件
     *
     * @param orderId 订单ID
     * @param newStatus 新支付状态
     * @param expectedStatus 期望的当前支付状态（用于CAS检查）
     * @return 更新的行数 (0 或 1)，返回1表示更新成功，0表示状态不匹配或订单不存在
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChargeOrder o SET o.paymentStatus = :newStatus WHERE o.id = :orderId AND o.paymentStatus = :expectedStatus")
    int atomicUpdatePayStatus(
            @Param("orderId") Long orderId,
            @Param("newStatus") Byte newStatus,
            @Param("expectedStatus") Byte expectedStatus
    );
}

