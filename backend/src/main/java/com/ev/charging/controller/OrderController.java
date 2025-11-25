package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.dto.CreateOrderDTO;
import com.ev.charging.service.OrderService;
import com.ev.charging.vo.OrderDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "订单管理", description = "充电订单创建、查询、支付、取消等接口")
@RestController
@RequestMapping(value = "/orders", produces = "application/json;charset=UTF-8")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单（开始充电）
     */
    @Operation(
            summary = "创建充电订单",
            description = "用户选择充电桩和充电模式，创建充电订单并开始充电",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "订单创建成功，返回订单ID"),
            @ApiResponse(responseCode = "400", description = "参数错误或充电桩不可用"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @PostMapping
    public Result<Long> createOrder(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Parameter(description = "订单创建信息", required = true) @RequestBody @Valid CreateOrderDTO dto) {
        log.info("创建订单: userId={}, pileId={}, chargeMode={}", userId, dto.getPileId(), dto.getChargeMode());

        try {
            Long orderId = orderService.createOrder(userId, dto);
            return Result.success(orderId);
        } catch (IllegalArgumentException e) {
            log.warn("创建订单参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建订单失败", e);
            return Result.error("创建订单失败，请稍后重试");
        }
    }

    /**
     * 结束充电
     * 安全修复: 移除客户端传入的金额参数，只接受endSoc
     * 所有费用计算在服务端完成，防止金额篡改攻击
     */
    @PutMapping("/{id}/end")
    public Result<Void> endCharging(@PathVariable Long id,
                                    @RequestAttribute("userId") Long userId,
                                    @RequestBody Map<String, Object> params) {
        log.info("结束充电: orderId={}, userId={}", id, userId);

        try {
            Object endSocObj = params.get("endSoc");
            if (endSocObj == null) {
                return Result.error("endSoc 不能为空");
            }
            Integer endSoc = ((Number) endSocObj).intValue();

            // 传入userId进行所有权验证，金额由服务端计算
            orderService.endCharging(id, userId, endSoc);
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.warn("结束充电参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("结束充电失败", e);
            return Result.error("结束充电失败，请稍后重试");
        }
    }

    /**
     * 获取订单列表
     */
    @Operation(
            summary = "获取订单列表",
            description = "分页查询当前用户的充电订单列表，可按状态筛选",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping
    public Result<Page<OrderDetailVO>> getOrderList(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Parameter(description = "订单状态（1待支付 2充电中 3已完成 4已取消 5异常）") @RequestParam(required = false) Byte orderStatus,
            @Parameter(description = "支付状态（0未支付 1已支付 2退款中 3已退款）") @RequestParam(required = false) Byte paymentStatus,
            @Parameter(description = "订单号搜索") @RequestParam(required = false) String orderNo,
            @Parameter(description = "页码（从0开始）", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") int size) {

        log.info("获取订单列表: userId={}, orderStatus={}, paymentStatus={}, orderNo={}, page={}, size={}", userId, orderStatus, paymentStatus, orderNo, page, size);

        try {
            Page<OrderDetailVO> orderPage = orderService.getOrderList(userId, orderStatus, paymentStatus, orderNo, page, size);
            return Result.success(orderPage);
        } catch (Exception e) {
            log.error("获取订单列表失败", e);
            return Result.error("获取订单列表失败，请稍后重试");
        }
    }

    /**
     * 获取当前进行中的订单
     */
    @GetMapping("/current")
    public Result<OrderDetailVO> getCurrentOrder(@RequestAttribute("userId") Long userId) {
        log.info("获取当前进行中的订单: userId={}", userId);

        try {
            OrderDetailVO order = orderService.getCurrentOrder(userId);
            return Result.success(order);
        } catch (Exception e) {
            log.error("获取当前订单失败", e);
            return Result.error("获取当前订单失败，请稍后重试");
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
            return Result.error("获取订单详情失败，请稍后重试");
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
        } catch (IllegalArgumentException e) {
            log.warn("取消订单参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("取消订单失败", e);
            return Result.error("取消订单失败，请稍后重试");
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
            Object pmObj = params.get("paymentMethod");
            if (pmObj == null) {
                return Result.error("paymentMethod 不能为空");
            }
            Byte paymentMethod = ((Number) pmObj).byteValue();
            orderService.payOrder(id, userId, paymentMethod);
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.warn("支付订单参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("支付订单失败", e);
            return Result.error("支付订单失败，请稍后重试");
        }
    }
}
