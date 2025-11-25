package com.ev.charging.repository;

import com.ev.charging.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 用户数据访问层
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据手机号查找用户
     *
     * @param phone 手机号
     * @return 用户信息
     */
    Optional<User> findByPhone(String phone);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return true-存在，false-不存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查手机号是否存在
     *
     * @param phone 手机号
     * @return true-存在，false-不存在
     */
    boolean existsByPhone(String phone);

    /**
     * 根据用户类型统计数量
     *
     * @param userType 用户类型
     * @return 数量
     */
    long countByUserType(String userType);

    /**
     * 根据状态统计数量
     *
     * @param status 状态
     * @return 数量
     */
    long countByStatus(String status);

    /**
     * 原子性增加用户余额（防止并发竞态条件）
     *
     * @param userId 用户ID
     * @param amount 增加金额（正数）
     * @return 影响行数
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.balance = u.balance + :amount WHERE u.id = :userId")
    int addBalanceAtomically(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 原子性扣减用户余额（带余额充足检查，防止并发竞态条件）
     *
     * @param userId 用户ID
     * @param amount 扣减金额（正数）
     * @return 影响行数（0表示余额不足或用户不存在）
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.balance = u.balance - :amount WHERE u.id = :userId AND u.balance >= :amount")
    int deductBalanceAtomically(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 原子性增加用户碳积分
     *
     * @param userId 用户ID
     * @param credits 增加积分
     * @return 影响行数
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.carbonCredits = u.carbonCredits + :credits WHERE u.id = :userId")
    int addCarbonCreditsAtomically(@Param("userId") Long userId, @Param("credits") Integer credits);

    /**
     * 原子性扣减用户碳积分（带积分充足检查）
     *
     * @param userId 用户ID
     * @param credits 扣减积分
     * @return 影响行数（0表示积分不足或用户不存在）
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.carbonCredits = u.carbonCredits - :credits WHERE u.id = :userId AND u.carbonCredits >= :credits")
    int deductCarbonCreditsAtomically(@Param("userId") Long userId, @Param("credits") Integer credits);
}
