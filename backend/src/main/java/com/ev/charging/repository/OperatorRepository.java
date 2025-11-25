package com.ev.charging.repository;

import com.ev.charging.entity.Operator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 运营商数据访问层
 */
@Repository
public interface OperatorRepository extends JpaRepository<Operator, Long> {

    /**
     * 根据运营商编码查询
     *
     * @param code 运营商编码
     * @return 运营商对象
     */
    Optional<Operator> findByCode(String code);

    /**
     * 根据状态查询运营商
     *
     * @param status 状态
     * @return 运营商列表
     */
    java.util.List<Operator> findByStatus(Byte status);
}
