package com.ev.charging.controller;

import com.alibaba.fastjson2.JSON;
import com.ev.charging.common.Result;
import com.ev.charging.dto.DischargeStrategyDTO;
import com.ev.charging.dto.StartDischargeDTO;
import com.ev.charging.dto.StopDischargeDTO;
import com.ev.charging.service.ElectricityPriceService;
import com.ev.charging.service.V2GService;
import com.ev.charging.vo.ElectricityPriceVO;
import com.ev.charging.vo.V2GRecordVO;
import com.ev.charging.vo.V2GStatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@Tag(name = "V2G双向充电", description = "V2G车辆向电网放电功能，包括放电控制、收益统计、电价预测等接口")
@RestController
@RequestMapping("/v2g")
@RequiredArgsConstructor
public class V2GController {

    private final V2GService v2gService;
    private final ElectricityPriceService electricityPriceService;
    private final RedisTemplate<String, String> redisTemplate;

    @Operation(
        summary = "开始放电",
        description = "用户启动V2G放电，将车辆电池电能反向输送到电网，赚取电价收益。系统会验证充电桩是否支持V2G、当前电价是否满足用户设定的最低价格等条件",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "放电启动成功"),
        @ApiResponse(responseCode = "400", description = "充电桩不支持V2G或不满足放电条件"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "404", description = "充电桩或用户不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/discharge/start")
    public Result<V2GRecordVO> startDischarge(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Parameter(description = "放电启动参数", required = true)
            @RequestBody @Valid StartDischargeDTO dto) {
        V2GRecordVO record = v2gService.startDischarge(userId, dto);
        return Result.success(record);
    }

    @Operation(
        summary = "停止放电",
        description = "用户停止V2G放电，系统计算本次放电量和收益（扣除10%平台手续费），并更新用户余额",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "放电停止成功"),
        @ApiResponse(responseCode = "400", description = "记录不存在或不是放电记录"),
        @ApiResponse(responseCode = "404", description = "记录不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/discharge/stop")
    public Result<V2GRecordVO> stopDischarge(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Parameter(description = "停止放电参数", required = true)
            @RequestBody @Valid StopDischargeDTO dto) {
        V2GRecordVO record = v2gService.stopDischarge(userId, dto);
        return Result.success(record);
    }

    @Operation(
        summary = "获取V2G记录",
        description = "查询当前用户的所有V2G充放电记录，按创建时间倒序排列",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/records")
    public Result<List<V2GRecordVO>> getRecords(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        List<V2GRecordVO> records = v2gService.getRecords(userId);
        return Result.success(records);
    }

    @Operation(
        summary = "获取V2G统计",
        description = "查询当前用户的V2G收益统计，包括总收益、总放电量、平均电价等信息",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/statistics")
    public Result<V2GStatisticsVO> getStatistics(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        V2GStatisticsVO statistics = v2gService.getStatistics(userId);
        return Result.success(statistics);
    }

    @Operation(
        summary = "管理端：获取全平台V2G统计",
        description = "管理后台专用接口，统计全平台今日放电量、今日收益、放电次数和参与用户数，不需要 userId",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "403", description = "无权限（需要ADMIN或OPERATOR角色）")
    })
    @GetMapping("/admin/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<V2GStatisticsVO> getAdminStatistics() {
        V2GStatisticsVO statistics = v2gService.getAdminStatistics();
        return Result.success(statistics);
    }

    @Operation(
        summary = "管理端：获取全部V2G放电记录",
        description = "管理后台专用接口，获取全平台所有V2G放电记录",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "403", description = "无权限（需要ADMIN或OPERATOR角色）")
    })
    @GetMapping("/admin/records")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<List<V2GRecordVO>> getAdminRecords() {
        List<V2GRecordVO> records = v2gService.getAllRecords();
        return Result.success(records);
    }

    @Operation(
        summary = "获取24小时电价预测",
        description = "获取未来24小时的电价预测数据，帮助用户选择最佳放电时段",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/price/forecast")
    public Result<List<ElectricityPriceVO>> getPriceForecast() {
        return Result.success(electricityPriceService.get24HourForecast());
    }

    @Operation(
        summary = "设置自动放电策略",
        description = "用户设置自动放电策略，例如当电价高于指定值时自动放电，当电池健康度低于指定值时停止放电",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "设置成功"),
        @ApiResponse(responseCode = "400", description = "策略参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/strategy/set")
    public Result<Void> setDischargeStrategy(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Parameter(description = "放电策略参数", required = true)
            @RequestBody @Valid DischargeStrategyDTO dto) {
        // 设置自动放电策略
        // 例如:当电价 > 1.0元/度时自动放电
        // 当电池健康度 < 70%时停止放电
        // 实际应该存储到数据库,这里简化处理

        redisTemplate.opsForValue().set(
            "v2g:strategy:" + userId,
            JSON.toJSONString(dto),
            Duration.ofDays(30)
        );

        return Result.success();
    }

    @Operation(
        summary = "获取放电收益预估",
        description = "根据当前电量、目标电量、电池容量和放电功率预估本次V2G放电收益",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "预估成功"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效")
    })
    @GetMapping("/profit-estimate")
    public Result<Map<String, Object>> getProfitEstimate(
            @Parameter(description = "当前电量(%)", example = "80") @RequestParam double currentSoc,
            @Parameter(description = "目标电量(%)", example = "20") @RequestParam double targetSoc,
            @Parameter(description = "电池容量(kWh)", example = "60") @RequestParam double batteryCapacity,
            @Parameter(description = "最大放电功率(kW)", example = "10") @RequestParam double maxPower,
            @Parameter(description = "计划放电时长(分钟)", example = "60") @RequestParam(defaultValue = "60") double duration) {

        // 按SOC计算可放电量
        double socDiff = Math.max(0, currentSoc - targetSoc);
        double energyBySoc = batteryCapacity * socDiff / 100.0;
        // 按功率×时间计算
        double energyByPower = maxPower * duration / 60.0;
        // 实际放电量取两者较小值
        double energyAmount = Math.min(energyBySoc, energyByPower);

        BigDecimal price = electricityPriceService.getRealTimePrice();
        double priceDouble = price.doubleValue();
        double revenue = energyAmount * priceDouble * 0.9; // 扣除10%平台手续费
        double platformFee = energyAmount * priceDouble * 0.1;

        Map<String, Object> result = new HashMap<>();
        result.put("profit", BigDecimal.valueOf(revenue).setScale(2, RoundingMode.HALF_UP));
        result.put("energyAmount", BigDecimal.valueOf(energyAmount).setScale(3, RoundingMode.HALF_UP));
        result.put("price", price);
        result.put("platformFee", BigDecimal.valueOf(platformFee).setScale(2, RoundingMode.HALF_UP));

        return Result.success(result);
    }
}
