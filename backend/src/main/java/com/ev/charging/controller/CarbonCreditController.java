package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.vo.CreditRecordVO;
import com.ev.charging.vo.CreditStatisticsVO;
import com.ev.charging.dto.RedeemDTO;
import com.ev.charging.entity.CreditPendingRecord;
import com.ev.charging.repository.CreditPendingRecordRepository;
import com.ev.charging.service.CarbonCreditService;
import com.ev.charging.vo.CreditPendingRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 碳积分控制器
 */
@Tag(name = "碳积分体系", description = "充电获积分、积分查询、签到、兑换等功能")
@RestController
@RequestMapping(value = "/credits", produces = "application/json;charset=UTF-8")
@RequiredArgsConstructor
@Slf4j
public class CarbonCreditController {

    private final CarbonCreditService carbonCreditService;
    private final CreditPendingRecordRepository creditPendingRecordRepository;

    /**
     * 获取积分余额
     *
     * @param userId 用户ID
     * @return 积分余额
     */
    @Operation(
            summary = "获取积分余额",
            description = "查询当前用户的碳积分余额",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping("/balance")
    public Result<Integer> getCreditBalance(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        log.info("查询用户 {} 的积分余额", userId);
        Integer balance = carbonCreditService.getCreditBalance(userId);
        return Result.success(balance);
    }

    /**
     * 获取积分记录
     *
     * @param userId     用户ID
     * @param creditType 积分类型（可选：1-充电 2-签到 3-兑换 4-活动）
     * @return 积分记录列表
     */
    @Operation(
            summary = "获取积分记录",
            description = "查询用户的积分获取和消费记录，可按类型筛选",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping("/history")
    public Result<List<CreditRecordVO>> getCreditHistory(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Parameter(description = "积分类型：1-充电 2-签到 3-兑换 4-活动", example = "1")
            @RequestParam(required = false) Byte creditType) {
        log.info("查询用户 {} 的积分记录，类型: {}", userId, creditType);
        List<CreditRecordVO> records = carbonCreditService.getCreditHistory(userId, creditType);
        return Result.success(records);
    }

    /**
     * 每日签到
     *
     * @param userId 用户ID
     * @return 获得的积分
     */
    @Operation(
            summary = "每日签到",
            description = "用户每日签到获取碳积分奖励",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "签到成功"),
            @ApiResponse(responseCode = "400", description = "今日已签到"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @PostMapping("/checkin")
    public Result<Integer> dailyCheckIn(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        log.info("用户 {} 请求签到", userId);
        Integer credits = carbonCreditService.dailyCheckIn(userId);
        return Result.success("签到成功，获得" + credits + "积分", credits);
    }

    /**
     * 获取积分统计
     *
     * @param userId 用户ID
     * @return 积分统计信息
     */
    @Operation(
            summary = "获取积分统计",
            description = "获取用户的积分累计、消费、排名等统计信息",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping("/statistics")
    public Result<CreditStatisticsVO> getCreditStatistics(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        log.info("查询用户 {} 的积分统计", userId);
        CreditStatisticsVO statistics = carbonCreditService.getCreditStatistics(userId);
        return Result.success(statistics);
    }

    /**
     * 兑换积分
     *
     * @param userId    用户ID
     * @param redeemDTO 兑换信息
     * @return 兑换结果
     */
    @Operation(
            summary = "兑换积分",
            description = "使用碳积分兑换优惠券、充电券等",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "兑换成功"),
            @ApiResponse(responseCode = "400", description = "积分不足"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @PostMapping("/redeem")
    public Result<Void> redeemCredits(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Parameter(description = "兑换信息", required = true)
            @Valid @RequestBody RedeemDTO redeemDTO) {
        log.info("用户 {} 兑换积分: {}", userId, redeemDTO.getAmount());
        carbonCreditService.redeemCredits(userId, redeemDTO.getAmount(), redeemDTO.getDescription());
        return Result.success("兑换成功", null);
    }

    /**
     * 查询用户的待发放积分记录
     * 用户可以查看自己有哪些积分正在发放中或发放失败
     *
     * @param userId 用户ID
     * @return 待发放积分记录列表
     */
    @Operation(
            summary = "查询待发放积分",
            description = "查询正在发放中或发放失败的积分记录",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping("/pending")
    public Result<List<CreditPendingRecordVO>> getPendingCredits(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        log.info("查询用户 {} 的待发放积分记录", userId);

        List<CreditPendingRecord> records = creditPendingRecordRepository
                .findByUserIdOrderByCreateTimeDesc(userId);

        List<CreditPendingRecordVO> voList = records.stream()
                .map(record -> {
                    int creditsToIssue = (int) (record.getChargeAmount().doubleValue() * 0.785 * 10);
                    return CreditPendingRecordVO.builder()
                            .id(record.getId())
                            .orderId(record.getOrderId())
                            .chargeAmount(record.getChargeAmount())
                            .creditsToIssue(creditsToIssue)
                            .retryCount(record.getRetryCount())
                            .status(record.getStatus())
                            .statusText(CreditPendingRecordVO.getStatusText(record.getStatus()))
                            .createTime(record.getCreateTime())
                            .updateTime(record.getUpdateTime())
                            .build();
                })
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 获取可兑换商品列表（模拟数据）
     */
    @Operation(
            summary = "获取可兑换商品列表",
            description = "查询积分商城中可兑换的商品"
    )
    @GetMapping("/products")
    public Result<List<Map<String, Object>>> getCreditProducts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> products = new java.util.ArrayList<>();

        Map<String, Object> p1 = new java.util.HashMap<>();
        p1.put("id", 1L);
        p1.put("name", "5元充电优惠券");
        p1.put("description", "充电时可抵扣5元");
        p1.put("pointsRequired", 500);
        p1.put("category", "coupon");
        p1.put("stock", 100);
        p1.put("imageUrl", null);
        products.add(p1);

        Map<String, Object> p2 = new java.util.HashMap<>();
        p2.put("id", 2L);
        p2.put("name", "充电8折券");
        p2.put("description", "单次充电享8折优惠");
        p2.put("pointsRequired", 800);
        p2.put("category", "discount");
        p2.put("stock", 50);
        p2.put("imageUrl", null);
        products.add(p2);

        Map<String, Object> p3 = new java.util.HashMap<>();
        p3.put("id", 3L);
        p3.put("name", "环保帆布袋");
        p3.put("description", "绿色环保帆布购物袋");
        p3.put("pointsRequired", 1200);
        p3.put("category", "gift");
        p3.put("stock", 30);
        p3.put("imageUrl", null);
        products.add(p3);

        if (category != null && !category.isEmpty()) {
            products = products.stream()
                    .filter(p -> category.equals(p.get("category")))
                    .collect(Collectors.toList());
        }

        return Result.success(products);
    }

    /**
     * 兑换商品
     */
    @Operation(
            summary = "兑换商品",
            description = "使用积分兑换商城商品",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @PostMapping("/exchange")
    public Result<Void> exchangeProduct(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody Map<String, Object> data) {
        if (data.get("productId") == null) {
            return Result.error("商品ID不能为空");
        }
        Long productId;
        int quantity;
        try {
            productId = Long.valueOf(data.get("productId").toString());
            quantity = data.get("quantity") != null ? Integer.parseInt(data.get("quantity").toString()) : 1;
        } catch (NumberFormatException e) {
            return Result.error("参数格式错误");
        }
        if (quantity < 1 || quantity > 99) {
            return Result.error("兑换数量必须在1-99之间");
        }

        // 模拟商品积分需求
        Map<Long, Integer> productCredits = Map.of(1L, 500, 2L, 800, 3L, 1200);
        int requiredCredits = productCredits.getOrDefault(productId, 0) * quantity;

        if (requiredCredits == 0) {
            return Result.error("商品不存在");
        }

        carbonCreditService.redeemCredits(userId, requiredCredits, "兑换商品 #" + productId);
        return Result.success("兑换成功", null);
    }

    /**
     * 获取兑换记录
     */
    @Operation(
            summary = "获取兑换记录",
            description = "查询用户的商品兑换记录",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @GetMapping("/exchange/records")
    public Result<Map<String, Object>> getExchangeRecords(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        // 返回空记录（兑换记录在实际系统中需要专门的表存储）
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("records", List.of());
        result.put("total", 0);
        return Result.success(result);
    }
}
