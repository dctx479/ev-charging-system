package com.ev.charging.repository;

import com.ev.charging.entity.ValetCharger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ValetChargerRepository extends JpaRepository<ValetCharger, Long> {

    List<ValetCharger> findByServiceAreaAndStatus(String serviceArea, Byte status);

    List<ValetCharger> findByStatus(Byte status);

    /**
     * 根据用户ID查询代充师傅信息
     * 用于所有权验证，确保只有师傅本人可以操作
     */
    Optional<ValetCharger> findByUserId(Long userId);
}
