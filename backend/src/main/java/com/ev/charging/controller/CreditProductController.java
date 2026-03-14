package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.entity.CarbonCreditRecord;
import com.ev.charging.entity.CreditExchangeRecord;
import com.ev.charging.entity.CreditProduct;
import com.ev.charging.entity.User;
import com.ev.charging.exception.BusinessException;
import com.ev.charging.repository.CarbonCreditRecordRepository;
import com.ev.charging.repository.CreditExchangeRecordRepository;
import com.ev.charging.repository.CreditProductRepository;
import com.ev.charging.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "积分商城", description = "积分商品浏览、兑换记录查询等功能")
@RestController
@RequestMapping(value = "/credit-products", produces = "application/json;charset=UTF-8")
@RequiredArgsConstructor
public class CreditProductController {

    private final CreditProductRepository productRepository;
    private final CreditExchangeRecordRepository exchangeRecordRepository;
    private final UserRepository userRepository;
    private final CarbonCreditRecordRepository carbonCreditRecordRepository;

    /**
     * 获取商品列表
     */
    @Operation(
            summary = "获取积分商品列表",
            description = "获取可兑换的积分商品列表，支持按分类筛选"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/products")
    public Result<List<CreditProduct>> getProducts(
            @Parameter(description = "商品分类（all-全部, voucher-充电券, coupon-优惠券, gift-实物礼品）", example = "all")
            @RequestParam(required = false) String category
    ) {
        List<CreditProduct> products;
        if (category != null && !category.equals("all")) {
            products = productRepository.findByCategoryAndStatusOrderByCreateTimeDesc(category, (byte) 1);
        } else {
            products = productRepository.findByStatusOrderByCreateTimeDesc((byte) 1);
        }
        return Result.success(products);
    }

    /**
     * 获取商品详情
     */
    @Operation(
            summary = "获取商品详情",
            description = "根据商品ID获取商品的详细信息"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "商品不存在")
    })
    @GetMapping("/products/{productId}")
    public Result<CreditProduct> getProductDetail(
            @Parameter(description = "商品ID", required = true, example = "1")
            @PathVariable Long productId) {
        CreditProduct product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return Result.error("商品不存在");
        }
        return Result.success(product);
    }

    /**
     * 兑换商品
     */
    @Operation(
            summary = "兑换商品",
            description = "使用积分兑换商品，支持充电券、优惠券、实物礼品等",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "兑换成功"),
            @ApiResponse(responseCode = "400", description = "积分不足或商品库存不足或已达兑换上限"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "404", description = "商品不存在")
    })
    @PostMapping("/exchange")
    @Transactional(rollbackFor = Exception.class)
    public Result<CreditExchangeRecord> exchangeProduct(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Parameter(description = "兑换请求", required = true)
            @RequestBody @Valid ExchangeRequest request
    ) {
        // 1. 查询商品
        CreditProduct product = productRepository.findById(request.getProductId()).orElse(null);
        if (product == null) {
            return Result.error("商品不存在");
        }

        // 2. 检查商品状态和库存
        if (product.getStatus() != 1) {
            return Result.error("商品已下架");
        }
        if (product.getStock() <= 0) {
            return Result.error("商品库存不足");
        }

        // 3. 检查用户兑换限制
        if (product.getLimitPerUser() != null && product.getLimitPerUser() > 0) {
            Long exchangedCount = exchangeRecordRepository.countByUserIdAndProductId(userId, product.getId());
            if (exchangedCount >= product.getLimitPerUser()) {
                return Result.error("您已达到该商品的兑换上限");
            }
        }

        // 4. 查询用户积分（预取用于计算预期余额，不用于扣减决策）
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (user.getCarbonCredits() < product.getPointsRequired()) {
            return Result.error("积分不足");
        }

        // 5. 原子扣减积分（WHERE carbonCredits >= credits 防止并发超扣）
        int expectedBalanceAfter = user.getCarbonCredits() - product.getPointsRequired();
        int updated = userRepository.deductCarbonCreditsAtomically(userId, product.getPointsRequired());
        if (updated == 0) {
            return Result.error("积分扣减失败，积分可能不足");
        }

        // 6. 创建积分消费记录
        CarbonCreditRecord creditRecord = CarbonCreditRecord.builder()
            .userId(userId)
            .changeType((byte) 3)  // 3-兑换消耗
            .changeAmount(-product.getPointsRequired())  // 负数表示消耗
            .balanceAfter(expectedBalanceAfter)
            .description("兑换商品：" + product.getName())
            .build();
        carbonCreditRecordRepository.save(creditRecord);

        // 7. 原子扣减库存（防止并发超卖）
        // 注意：必须抛异常而非return Result.error，否则@Transactional不会回滚已扣减的积分
        int stockUpdated = productRepository.decrementStockAtomically(product.getId());
        if (stockUpdated == 0) {
            throw new BusinessException("商品库存不足");
        }

        // 8. 创建兑换记录
        CreditExchangeRecord record = CreditExchangeRecord.builder()
            .userId(userId)
            .productId(product.getId())
            .productName(product.getName())
            .pointsUsed(product.getPointsRequired())
            .quantity(1)
            .status((byte) 1)
            .address(request.getAddress())
            .receiverName(request.getReceiverName())
            .receiverPhone(request.getReceiverPhone())
            .build();

        exchangeRecordRepository.save(record);

        return Result.success(record);
    }

    /**
     * 获取兑换记录
     */
    @Operation(
            summary = "获取兑换记录",
            description = "查询用户的积分商品兑换记录",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping("/exchange/records")
    public Result<List<CreditExchangeRecord>> getExchangeRecords(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId
    ) {
        List<CreditExchangeRecord> records = exchangeRecordRepository.findByUserIdOrderByCreateTimeDesc(userId);
        return Result.success(records);
    }

    @Data
    public static class ExchangeRequest {
        private Long productId;
        private String address;
        private String receiverName;
        private String receiverPhone;
    }
}
