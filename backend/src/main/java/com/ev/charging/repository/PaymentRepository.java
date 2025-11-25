package com.ev.charging.repository;

import com.ev.charging.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 支付记录Repository
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 根据订单ID查询支付记录
     */
    Optional<Payment> findByOrderId(Long orderId);

    /**
     * 根据支付流水号查询
     */
    Optional<Payment> findByPaymentNo(String paymentNo);
}
