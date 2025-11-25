package com.ev.charging.repository;

import com.ev.charging.entity.CreditProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CreditProductRepository extends JpaRepository<CreditProduct, Long> {

    List<CreditProduct> findByStatusOrderByCreateTimeDesc(Byte status);

    List<CreditProduct> findByCategoryAndStatusOrderByCreateTimeDesc(String category, Byte status);

    @Modifying
    @Transactional
    @Query("UPDATE CreditProduct p SET p.stock = p.stock - 1, p.exchangedCount = p.exchangedCount + 1 WHERE p.id = :id AND p.stock > 0")
    int decrementStockAtomically(@Param("id") Long id);
}
