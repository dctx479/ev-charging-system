package com.ev.charging.repository;

import com.ev.charging.entity.ValetChargeOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ValetChargeOrderRepository extends JpaRepository<ValetChargeOrder, Long> {

    Optional<ValetChargeOrder> findByOrderNo(String orderNo);

    List<ValetChargeOrder> findByUserIdOrderByCreateTimeDesc(Long userId);

    List<ValetChargeOrder> findByChargerIdOrderByCreateTimeDesc(Long chargerId);

    /**
     * 原子性接单（CAS：仅在 orderStatus=1 时更新，防止并发竞态）
     *
     * @return 影响行数（0 表示已被抢单）
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ValetChargeOrder o SET o.chargerId = :chargerId, o.orderStatus = 2, o.assignTime = :assignTime WHERE o.orderNo = :orderNo AND o.orderStatus = 1")
    int acceptOrderAtomically(@Param("orderNo") String orderNo,
                              @Param("chargerId") Long chargerId,
                              @Param("assignTime") LocalDateTime assignTime);

    /**
     * 查询所有代充订单（管理端使用）
     */
    List<ValetChargeOrder> findAllByOrderByCreateTimeDesc();
}
