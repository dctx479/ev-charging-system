package com.ev.charging.scheduler;

import com.ev.charging.constant.OrderConstants;
import com.ev.charging.entity.ChargeOrder;
import com.ev.charging.entity.ChargingPile;
import com.ev.charging.repository.ChargeOrderRepository;
import com.ev.charging.repository.ChargingPileRepository;
import com.ev.charging.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时定时任务
 *
 * 功能：
 * 1. 充电超时检查：充电中订单超过指定时间自动标记为异常
 * 2. 支付超时检查：已完成订单超过指定时间未支付自动取消
 *
 * @author EV Team
 */
@Component
@Slf4j
public class OrderTimeoutScheduler {

    @Autowired
    private ChargeOrderRepository orderRepository;

    @Autowired
    private ChargingPileRepository pileRepository;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 充电超时时长（小时）
     */
    @Value("${order.timeout.charging-hours:4}")
    private Integer chargingTimeoutHours;

    /**
     * 支付超时时长（小时）
     */
    @Value("${order.timeout.payment-hours:24}")
    private Integer paymentTimeoutHours;

    /**
     * 充电超时检查（每5分钟执行一次）
     *
     * 检查规则：
     * - 订单状态为"充电中"（orderStatus = ORDER_STATUS_CHARGING = 2）
     * - 开始充电时间超过配置的超时时长（默认4小时）
     *
     * 处理逻辑：
     * - 将订单状态标记为"异常"（orderStatus = ORDER_STATUS_EXCEPTION = 5）
     * - 释放充电桩，设置状态为"空闲"（status = 1）
     * - 设置订单结束时间
     * - 记录详细日志
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 每次执行完成后等待5分钟再执行下一次
    public void checkChargingTimeout() {
        try {
            // 计算超时时间点
            LocalDateTime timeoutPoint = LocalDateTime.now().minusHours(chargingTimeoutHours);

            // 查找充电超时的订单
            List<ChargeOrder> timeoutOrders = orderRepository.findChargingTimeoutOrders(timeoutPoint);

            if (timeoutOrders.isEmpty()) {
                log.debug("充电超时检查：未发现超时订单");
                return;
            }

            log.warn("检测到 {} 个充电超时订单，开始处理...", timeoutOrders.size());

            for (ChargeOrder order : timeoutOrders) {
                try {
                    // 使用Spring代理调用以确保每个订单在独立事务中处理
                    OrderTimeoutScheduler proxy = applicationContext.getBean(OrderTimeoutScheduler.class);
                    proxy.handleChargingTimeout(order);
                } catch (Exception e) {
                    log.error("处理充电超时订单失败: orderId={}, orderNo={}, error={}",
                            order.getId(), order.getOrderNo(), e.getMessage(), e);
                }
            }

            log.info("充电超时检查完成：处理了 {} 个超时订单", timeoutOrders.size());

        } catch (Exception e) {
            log.error("充电超时检查任务执行失败", e);
        }
    }

    /**
     * 处理单个充电超时订单（独立事务，单条失败不影响其他订单）
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleChargingTimeout(ChargeOrder order) {
        log.warn("处理充电超时订单: orderId={}, orderNo={}, pileId={}, startTime={}, 已充电时长={}小时",
                order.getId(),
                order.getOrderNo(),
                order.getPileId(),
                order.getStartTime(),
                java.time.Duration.between(order.getStartTime(), LocalDateTime.now()).toHours());

        // 1. 更新订单状态为"异常"
        order.setOrderStatus(OrderConstants.ORDER_STATUS_EXCEPTION);
        order.setEndTime(LocalDateTime.now());
        orderRepository.save(order);

        // 2. 释放充电桩
        try {
            ChargingPile pile = pileRepository.findById(order.getPileId()).orElse(null);
            if (pile != null) {
                pile.setStatus((byte) 1); // 设置为空闲
                pileRepository.save(pile);
                // 清除缓存，确保后续查询读取到最新空闲状态
                cacheService.evictPileCache(pile.getId());
                log.info("已释放充电桩: pileId={}, pileNo={}", pile.getId(), pile.getPileNo());
            } else {
                log.warn("充电桩不存在: pileId={}", order.getPileId());
            }
        } catch (Exception e) {
            log.error("释放充电桩失败: pileId={}", order.getPileId(), e);
        }

        log.info("充电超时订单已标记为异常: orderId={}, orderNo={}", order.getId(), order.getOrderNo());
    }

    /**
     * 支付超时检查（每10分钟执行一次）
     *
     * 检查规则：
     * - 订单状态为"已完成"（orderStatus = ORDER_STATUS_COMPLETED = 3）
     * - 支付状态为"未支付"（paymentStatus = 0）
     * - 结束充电时间超过配置的超时时长（默认24小时）
     *
     * 处理逻辑：
     * - 将订单状态标记为"已取消"（orderStatus = ORDER_STATUS_CANCELLED = 4）
     * - 记录详细日志
     */
    @Scheduled(fixedDelay = 600000, initialDelay = 60000) // 每次执行完成后等待10分钟再执行下一次
    public void checkPaymentTimeout() {
        try {
            // 计算超时时间点
            LocalDateTime timeoutPoint = LocalDateTime.now().minusHours(paymentTimeoutHours);

            // 查找支付超时的订单
            List<ChargeOrder> timeoutOrders = orderRepository.findPaymentTimeoutOrders(timeoutPoint);

            if (timeoutOrders.isEmpty()) {
                log.debug("支付超时检查：未发现超时订单");
                return;
            }

            log.warn("检测到 {} 个支付超时订单，开始处理...", timeoutOrders.size());

            for (ChargeOrder order : timeoutOrders) {
                try {
                    OrderTimeoutScheduler proxy = applicationContext.getBean(OrderTimeoutScheduler.class);
                    proxy.handlePaymentTimeout(order);
                } catch (Exception e) {
                    log.error("处理支付超时订单失败: orderId={}, orderNo={}, error={}",
                            order.getId(), order.getOrderNo(), e.getMessage(), e);
                }
            }

            log.info("支付超时检查完成：处理了 {} 个超时订单", timeoutOrders.size());

        } catch (Exception e) {
            log.error("支付超时检查任务执行失败", e);
        }
    }

    /**
     * 处理单个支付超时订单（独立事务，单条失败不影响其他订单）
     */
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentTimeout(ChargeOrder order) {
        long overtimeHours = order.getEndTime() != null
                ? java.time.Duration.between(order.getEndTime(), LocalDateTime.now()).toHours()
                : -1;
        log.warn("处理支付超时订单: orderId={}, orderNo={}, totalFee={}, endTime={}, 已超时={}小时",
                order.getId(),
                order.getOrderNo(),
                order.getTotalFee(),
                order.getEndTime(),
                overtimeHours);

        // 更新订单状态为"已取消"
        order.setOrderStatus(OrderConstants.ORDER_STATUS_CANCELLED);
        orderRepository.save(order);

        log.info("支付超时订单已取消: orderId={}, orderNo={}, totalFee={}",
                order.getId(), order.getOrderNo(), order.getTotalFee());
    }

    /**
     * 系统启动时的初始化检查（延迟1分钟执行）
     *
     * 用于处理系统重启前遗留的超时订单
     */
    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE) // 只执行一次
    public void initializeTimeoutCheck() {
        log.info("====== 订单超时初始化检查开始 ======");
        try {
            // 通过 Spring 代理调用，确保 @Transactional 正常生效，避免 AOP 自调用失效
            OrderTimeoutScheduler proxy = applicationContext.getBean(OrderTimeoutScheduler.class);
            proxy.checkChargingTimeout();
            proxy.checkPaymentTimeout();
            log.info("====== 订单超时初始化检查完成 ======");
        } catch (Exception e) {
            log.error("订单超时初始化检查失败", e);
        }
    }
}
