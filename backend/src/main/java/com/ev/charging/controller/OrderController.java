package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.dto.CreateOrderDTO;
import com.ev.charging.dto.EndChargingDTO;
import com.ev.charging.dto.PayOrderDTO;
import com.ev.charging.service.OrderService;
import com.ev.charging.vo.OrderDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单Controller
 */
@Tag(name = "订单管理", description = "充电订单创建、查询、支付、取消等接口")
@RestController
@RequestMapping(value = "/orders", produces = "application/json;charset=UTF-8")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

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
        Long orderId = orderService.createOrder(userId, dto);
        return Result.success(orderId);
    }

    /**
     * 结束充电
     * 安全修复: 移除客户端传入的金额参数，只接受endSoc
     * 所有费用计算在服务端完成，防止金额篡改攻击
     */
    @PutMapping("/{id}/end")
    public Result<Void> endCharging(@PathVariable Long id,
                                    @RequestAttribute("userId") Long userId,
                                    @RequestBody @Valid EndChargingDTO dto) {
        log.info("结束充电: orderId={}, userId={}", id, userId);
        orderService.endCharging(id, userId, dto.getEndSoc());
        return Result.success();
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
        Page<OrderDetailVO> orderPage = orderService.getOrderList(userId, orderStatus, paymentStatus, orderNo, page, size);
        return Result.success(orderPage);
    }

    /**
     * 获取当前进行中的订单
     */
    @GetMapping("/current")
    public Result<OrderDetailVO> getCurrentOrder(@RequestAttribute("userId") Long userId) {
        log.info("获取当前进行中的订单: userId={}", userId);
        OrderDetailVO order = orderService.getCurrentOrder(userId);
        return Result.success(order);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable Long id,
                                                @RequestAttribute("userId") Long userId) {
        log.info("获取订单详情: orderId={}, userId={}", id, userId);
        OrderDetailVO order = orderService.getOrderDetail(id);

        // 验证订单归属（防止IDOR：在返回任何数据前校验）
        if (order == null || !order.getUserId().equals(userId)) {
            return Result.error("无权查看此订单");
        }

        return Result.success(order);
    }

    /**
     * 取消订单
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id,
                                    @RequestAttribute("userId") Long userId) {
        log.info("取消订单: orderId={}, userId={}", id, userId);
        orderService.cancelOrder(id, userId);
        return Result.success();
    }

    /**
     * 支付订单（模拟支付）
     */
    @PostMapping("/{id}/pay")
    public Result<Void> payOrder(@PathVariable Long id,
                                 @RequestAttribute("userId") Long userId,
                                 @RequestBody @Valid PayOrderDTO dto) {
        log.info("支付订单: orderId={}, userId={}", id, userId);
        orderService.payOrder(id, userId, dto.getPaymentMethod());
        return Result.success();
    }
}
