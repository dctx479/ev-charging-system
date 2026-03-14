package com.ev.charging.controller.admin;

import com.ev.charging.common.Result;
import com.ev.charging.dto.AdminOrderQueryDTO;
import com.ev.charging.dto.RefundDTO;
import com.ev.charging.entity.ChargeOrder;
import com.ev.charging.service.AdminOrderService;
import com.ev.charging.vo.AdminOrderListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 管理后台 - 订单管理控制器
 */
@RestController
@RequestMapping(value = "/admin/orders", produces = "application/json;charset=UTF-8")
@Tag(name = "管理后台 - 订单管理", description = "订单查询、状态管理、退款、导出等功能")
@RequiredArgsConstructor
@Slf4j
public class OrderManagementController {

    private final AdminOrderService adminOrderService;

    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * 分页获取订单列表
     */
    @GetMapping
    @Operation(summary = "分页获取订单列表", description = "支持按订单状态、支付状态、站点、充电桩、时间范围、关键词搜索等条件筛选")
    public Result<Page<AdminOrderListVO>> getOrderList(
            @RequestParam(required = false) Byte orderStatus,
            @RequestParam(required = false) Byte paymentStatus,
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) Long pileId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        AdminOrderQueryDTO queryDTO = new AdminOrderQueryDTO();
        queryDTO.setOrderStatus(orderStatus);
        queryDTO.setPaymentStatus(paymentStatus);
        queryDTO.setStationId(stationId);
        queryDTO.setPileId(pileId);
        queryDTO.setKeyword(keyword);
        queryDTO.setPage(page);
        queryDTO.setSize(size);

        // 解析时间范围
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try {
            if (startTime != null && !startTime.isEmpty()) {
                queryDTO.setStartTime(LocalDateTime.parse(startTime, formatter));
            }
            if (endTime != null && !endTime.isEmpty()) {
                queryDTO.setEndTime(LocalDateTime.parse(endTime, formatter));
            }
        } catch (java.time.format.DateTimeParseException e) {
            return Result.error("时间格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
        }

        Page<AdminOrderListVO> result = adminOrderService.getOrderList(queryDTO);

        log.info("查询订单列表: page={}, size={}, total={}", page, size, result.getTotalElements());

        return Result.success(result);
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取订单详情", description = "根据订单ID获取订单完整信息")
    public Result<AdminOrderListVO> getOrderDetail(@PathVariable Long id) {
        AdminOrderListVO order = adminOrderService.getOrderDetail(id);
        return Result.success(order);
    }

    /**
     * 获取订单统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取订单统计", description = "获取订单各状态数量统计")
    public Result<Object> getOrderStatistics() {
        // 简单统计各状态订单数量
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("total", adminOrderService.countAll());
        stats.put("pending", adminOrderService.countByStatus((byte) 1));
        stats.put("charging", adminOrderService.countByStatus((byte) 2));
        stats.put("completed", adminOrderService.countByStatus((byte) 3));
        stats.put("cancelled", adminOrderService.countByStatus((byte) 4));
        stats.put("paid", adminOrderService.countByPaymentStatus((byte) 1));
        stats.put("unpaid", adminOrderService.countByPaymentStatus((byte) 0));
        return Result.success(stats);
    }

    /**
     * 更新订单状态
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新订单状态", description = "管理员更新订单状态（处理异常订单）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam Byte status
    ) {
        // 验证状态值（OrderConstants: 1=待支付, 2=充电中, 3=已完成, 4=已取消, 5=异常）
        if (status < 1 || status > 5) {
            return Result.error("无效的订单状态");
        }

        adminOrderService.updateOrderStatus(id, status);

        log.info("更新订单状态成功: orderId={}, newStatus={}", id, status);

        return Result.success();
    }

    /**
     * 订单退款
     */
    @PostMapping("/{id}/refund")
    @Operation(summary = "订单退款", description = "管理员对已支付订单进行退款操作")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> refundOrder(
            @PathVariable Long id,
            @RequestBody @Valid RefundDTO refundDTO
    ) {
        adminOrderService.refundOrder(id, refundDTO);

        log.info("订单退款成功: orderId={}, reason={}", id, refundDTO.getRefundReason());

        return Result.success();
    }

    /**
     * 导出订单Excel
     */
    @GetMapping("/export")
    @Operation(summary = "导出订单Excel", description = "根据查询条件导出订单数据到Excel文件")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam(required = false) Byte orderStatus,
            @RequestParam(required = false) Byte paymentStatus,
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) Long pileId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        try {
            AdminOrderQueryDTO queryDTO = new AdminOrderQueryDTO();
            queryDTO.setOrderStatus(orderStatus);
            queryDTO.setPaymentStatus(paymentStatus);
            queryDTO.setStationId(stationId);
            queryDTO.setPileId(pileId);
            queryDTO.setKeyword(keyword);

            // 解析时间范围
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            try {
                if (startTime != null && !startTime.isEmpty()) {
                    queryDTO.setStartTime(LocalDateTime.parse(startTime, formatter));
                }
                if (endTime != null && !endTime.isEmpty()) {
                    queryDTO.setEndTime(LocalDateTime.parse(endTime, formatter));
                }
            } catch (java.time.format.DateTimeParseException e) {
                return ResponseEntity.badRequest().build();
            }

            byte[] excelData = adminOrderService.exportOrders(queryDTO);

            // 生成文件名（RFC 5987 编码）
            String fileName = "充电订单_" + LocalDateTime.now().format(FILE_NAME_FORMATTER) + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(
                ContentDisposition.attachment()
                    .filename(fileName, StandardCharsets.UTF_8)
                    .build()
            );
            headers.setContentLength(excelData.length);

            log.info("导出订单Excel成功: fileName={}", fileName);

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("导出订单Excel失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
