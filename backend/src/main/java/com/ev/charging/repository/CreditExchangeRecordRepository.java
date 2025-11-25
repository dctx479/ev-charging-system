package com.ev.charging.repository;

import com.ev.charging.entity.CreditExchangeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditExchangeRecordRepository extends JpaRepository<CreditExchangeRecord, Long> {

    List<CreditExchangeRecord> findByUserIdOrderByCreateTimeDesc(Long userId);

    Long countByUserIdAndProductId(Long userId, Long productId);
}
