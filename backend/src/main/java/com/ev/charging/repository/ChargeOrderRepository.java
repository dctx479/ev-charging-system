package com.ev.charging.repository;

import com.ev.charging.entity.ChargeOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 充电订单Repository
 */
@Repository
public interface ChargeOrderRepository extends JpaRepository<ChargeOrder, Long> {

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
    @Query("SELECT COALESCE(SUM(o.chargeAmount), 0) FROM ChargeOrder o WHERE o.userId = :userId AND o.orderStatus = 1")
    BigDecimal sumChargeAmountByUserId(@Param("userId") Long userId);

    /**
     * 统计用户充电次数
     */
    @Query("SELECT COUNT(o) FROM ChargeOrder o WHERE o.userId = :userId AND o.orderStatus = 1")
    Long countCompletedOrdersByUserId(@Param("userId") Long userId);

    /**
     * 查询用户进行中的订单
     */
    Optional<ChargeOrder> findByUserIdAndOrderStatus(Long userId, Byte orderStatus);

    /**
     * 查询充电桩的进行中订单
     */
    Optional<ChargeOrder> findByPileIdAndOrderStatus(Long pileId, Byte orderStatus);
}
