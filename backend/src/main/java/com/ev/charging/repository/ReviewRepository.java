package com.ev.charging.repository;

import com.ev.charging.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 评价表Repository
 * 用于存储和查询用户对充电站点、充电桩和服务的评价信息
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * 根据用户ID查询评价列表（按创建时间倒序）
     */
    List<Review> findByUserIdOrderByCreateTimeDesc(Long userId);

    /**
     * 根据用户ID查询评价列表（分页）
     */
    Page<Review> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    /**
     * 根据站点ID查询评价列表（按创建时间倒序）
     */
    List<Review> findByStationIdOrderByCreateTimeDesc(Long stationId);

    /**
     * 根据站点ID查询评价列表（分页）
     */
    Page<Review> findByStationIdOrderByCreateTimeDesc(Long stationId, Pageable pageable);

    /**
     * 根据充电桩ID查询评价列表（按创建时间倒序）
     */
    List<Review> findByPileIdOrderByCreateTimeDesc(Long pileId);

    /**
     * 根据充电桩ID查询评价列表（分页）
     */
    Page<Review> findByPileIdOrderByCreateTimeDesc(Long pileId, Pageable pageable);

    /**
     * 根据订单ID查询评价
     */
    Optional<Review> findByOrderId(Long orderId);

    /**
     * 统计用户发表的评价总数
     */
    long countByUserId(Long userId);

    /**
     * 统计充电站的评价总数
     */
    long countByStationId(Long stationId);

    /**
     * 统计充电桩的评价总数
     */
    long countByPileId(Long pileId);

    /**
     * 计算充电站的平均评分
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.stationId = :stationId")
    Double getAverageRatingByStationId(@Param("stationId") Long stationId);

    /**
     * 计算充电桩的平均评分
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.pileId = :pileId")
    Double getAverageRatingByPileId(@Param("pileId") Long pileId);

    /**
     * 计算充电站的平均服务评分
     */
    @Query("SELECT AVG(r.serviceRating) FROM Review r WHERE r.stationId = :stationId")
    Double getAverageServiceRatingByStationId(@Param("stationId") Long stationId);

    /**
     * 计算充电站的平均设施评分
     */
    @Query("SELECT AVG(r.facilityRating) FROM Review r WHERE r.stationId = :stationId")
    Double getAverageFacilityRatingByStationId(@Param("stationId") Long stationId);

    /**
     * 计算充电站的平均环境评分
     */
    @Query("SELECT AVG(r.environmentRating) FROM Review r WHERE r.stationId = :stationId")
    Double getAverageEnvironmentRatingByStationId(@Param("stationId") Long stationId);

    /**
     * 统计充电站指定评分等级的评价数量
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.stationId = :stationId AND r.rating = :rating")
    long countByStationIdAndRating(@Param("stationId") Long stationId, @Param("rating") Byte rating);

    /**
     * 按评分统计充电站的评价分布
     */
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.stationId = :stationId GROUP BY r.rating ORDER BY r.rating")
    List<Object[]> getStationReviewDistribution(@Param("stationId") Long stationId);
}
