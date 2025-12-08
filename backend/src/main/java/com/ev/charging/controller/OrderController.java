package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.dto.CreateOrderDTO;
import com.ev.charging.service.OrderService;
import com.ev.charging.vo.OrderDetailVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 订单Controller
 */
@RestController
@RequestMapping("/api/orders")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单（开始充电）
     */
    @PostMapping
    public Result<Long> createOrder(@RequestAttribute("userId") Long userId,
                                    @RequestBody @Valid CreateOrderDTO dto) {
        log.info("创建订单: userId={}, pileId={}, chargeMode={}", userId, dto.getPileId(), dto.getChargeMode());

        try {
            Long orderId = orderService.createOrder(userId, dto);
            return Result.success(orderId);
        } catch (Exception e) {
            log.error("创建订单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 结束充电
     */
    @PutMapping("/{id}/end")
    public Result<Void> endCharging(@PathVariable Long id,
                                    @RequestAttribute("userId") Long userId,
                                    @RequestBody Map<String, Object> params) {
        log.info("结束充电: orderId={}, userId={}", id, userId);

        try {
            Integer endSoc = (Integer) params.get("endSoc");
            BigDecimal actualChargeAmount = new BigDecimal(params.get("actualChargeAmount").toString());

            orderService.endCharging(id, endSoc, actualChargeAmount);
            return Result.success();
        } catch (Exception e) {
            log.error("结束充电失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取订单列表
     */
    @GetMapping
    public Result<Page<OrderDetailVO>> getOrderList(
            @RequestAttribute("userId") Long userId,
            @RequestParam(required = false) Byte orderStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("获取订单列表: userId={}, orderStatus={}, page={}, size={}", userId, orderStatus, page, size);

        try {
            Page<OrderDetailVO> orderPage = orderService.getOrderList(userId, orderStatus, page, size);
            return Result.success(orderPage);
        } catch (Exception e) {
            log.error("获取订单列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable Long id,
                                                @RequestAttribute("userId") Long userId) {
        log.info("获取订单详情: orderId={}, userId={}", id, userId);

        try {
            OrderDetailVO order = orderService.getOrderDetail(id);

            // 验证订单归属
            if (!order.getUserId().equals(userId)) {
                return Result.error("无权查看此订单");
            }

            return Result.success(order);
        } catch (Exception e) {
            log.error("获取订单详情失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消订单
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id,
                                    @RequestAttribute("userId") Long userId) {
        log.info("取消订单: orderId={}, userId={}", id, userId);

        try {
            orderService.cancelOrder(id, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("取消订单失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 支付订单（模拟支付）
     */
    @PostMapping("/{id}/pay")
    public Result<Void> payOrder(@PathVariable Long id,
                                 @RequestAttribute("userId") Long userId,
                                 @RequestBody Map<String, Object> params) {
        log.info("支付订单: orderId={}, userId={}", id, userId);

        try {
            Byte paymentMethod = ((Number) params.get("paymentMethod")).byteValue();
            orderService.payOrder(id, userId, paymentMethod);
            return Result.success();
        } catch (Exception e) {
            log.error("支付订单失败", e);
            return Result.error(e.getMessage());
        }
    }
}
