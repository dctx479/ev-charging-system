# AI预测服务使用指南

## 概述
本文档说明如何在项目中使用AI预测服务。

## 服务架构

```
┌─────────────────┐      HTTP      ┌──────────────────┐      HTTP      ┌─────────────┐
│  前端/订单服务   │ ──────────────> │  Spring Boot后端  │ ──────────────> │ Flask AI服务 │
│  (调用方)       │                │ (中间层)         │                │ (AI模型)    │
└─────────────────┘                └──────────────────┘                └─────────────┘
```

## 快速开始

### 1. 在Service中注入AIPredictionService

```java
@Service
public class OrderService {

    @Autowired
    private AIPredictionService aiPredictionService;

    public ChargeOrderVO createOrder(CreateOrderDTO dto) {
        // 调用AI预测充电时长
        DurationPredictRequest predictRequest = DurationPredictRequest.builder()
            .batteryCapacity(dto.getBatteryCapacity())
            .currentSoc(dto.getCurrentSoc())
            .targetSoc(dto.getTargetSoc())
            .chargePower(pile.getPower())
            .temperature(new BigDecimal("25"))
            .build();

        DurationPredictResponse prediction =
            aiPredictionService.predictChargeDuration(predictRequest);

        // 使用预测结果
        order.setEstimatedDuration(prediction.getDuration().intValue());
        order.setEstimatedCost(prediction.getEstimatedCost());

        return order;
    }
}
```

### 2. 在Controller中直接调用

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private AIPredictionService aiPredictionService;

    @PostMapping("/estimate")
    public Result<DurationPredictResponse> estimateCharge(
        @RequestBody DurationPredictRequest request) {

        DurationPredictResponse response =
            aiPredictionService.predictChargeDuration(request);

        return Result.success(response);
    }
}
```

## API详细说明

### 1. 充电时长预测

#### 请求参数 (DurationPredictRequest)

| 字段 | 类型 | 必填 | 说明 | 示例 |
|------|------|------|------|------|
| batteryCapacity | BigDecimal | 是 | 电池容量(kWh) | 75 |
| currentSoc | BigDecimal | 是 | 当前电量(%) | 20 |
| targetSoc | BigDecimal | 是 | 目标电量(%) | 80 |
| chargePower | BigDecimal | 是 | 充电功率(kW) | 120 |
| temperature | BigDecimal | 否 | 环境温度(℃) | 25 |

#### 响应数据 (DurationPredictResponse)

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| duration | BigDecimal | 预测充电时长(分钟) | 32.5 |
| chargeAmount | BigDecimal | 预计充电量(kWh) | 45.0 |
| estimatedCost | BigDecimal | 预计费用(元) | 45.0 |

#### 使用示例

```java
DurationPredictRequest request = DurationPredictRequest.builder()
    .batteryCapacity(new BigDecimal("75"))
    .currentSoc(new BigDecimal("20"))
    .targetSoc(new BigDecimal("80"))
    .chargePower(new BigDecimal("120"))
    .temperature(new BigDecimal("25"))
    .build();

try {
    DurationPredictResponse response =
        aiPredictionService.predictChargeDuration(request);

    System.out.println("预计充电时长: " + response.getDuration() + "分钟");
    System.out.println("预计充电量: " + response.getChargeAmount() + "kWh");
    System.out.println("预计费用: " + response.getEstimatedCost() + "元");

} catch (IllegalArgumentException e) {
    // 参数验证错误
    log.error("参数错误: {}", e.getMessage());
} catch (RuntimeException e) {
    // AI服务调用失败
    log.error("AI服务调用失败: {}", e.getMessage());
}
```

### 2. 故障预测

#### 请求参数 (FaultPredictRequest)

| 字段 | 类型 | 必填 | 说明 | 示例 |
|------|------|------|------|------|
| totalChargeCount | Integer | 是 | 累计充电次数 | 500 |
| totalChargeAmount | BigDecimal | 是 | 累计充电量(kWh) | 15000 |
| daysSinceLastMaintenance | Integer | 是 | 距上次维护天数 | 45 |
| healthScore | Integer | 是 | 健康度评分(0-100) | 65 |
| avgDailyUsage | BigDecimal | 是 | 平均每日使用次数 | 8.5 |
| voltageFluctuation | BigDecimal | 是 | 电压波动 | 2.3 |
| faultHistoryCount | Integer | 是 | 历史故障次数 | 2 |

#### 响应数据 (FaultPredictResponse)

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| faultProbability | BigDecimal | 故障概率(0-100) | 45.8 |
| willFault | Boolean | 是否会故障 | false |
| suggestion | String | 维护建议 | "中风险，建议3天内安排维护" |
| riskLevel | String | 风险等级 | "中风险" |

#### 使用示例

```java
FaultPredictRequest request = FaultPredictRequest.builder()
    .totalChargeCount(500)
    .totalChargeAmount(new BigDecimal("15000"))
    .daysSinceLastMaintenance(45)
    .healthScore(65)
    .avgDailyUsage(new BigDecimal("8.5"))
    .voltageFluctuation(new BigDecimal("2.3"))
    .faultHistoryCount(2)
    .build();

try {
    FaultPredictResponse response =
        aiPredictionService.predictFault(request);

    System.out.println("故障概率: " + response.getFaultProbability() + "%");
    System.out.println("风险等级: " + response.getRiskLevel());
    System.out.println("建议: " + response.getSuggestion());

    // 根据风险等级采取行动
    if ("高风险".equals(response.getRiskLevel())) {
        // 立即安排维护
        maintenanceService.createUrgentPlan(pileId);
    }

} catch (RuntimeException e) {
    log.error("故障预测失败: {}", e.getMessage());
}
```

## 常见使用场景

### 场景1: 用户创建充电订单时预测时长

```java
@Service
public class OrderService {

    @Autowired
    private AIPredictionService aiPredictionService;

    @Autowired
    private ChargingPileRepository pileRepository;

    public ChargeOrderVO createOrder(CreateOrderDTO dto) {
        // 1. 获取充电桩信息
        ChargingPile pile = pileRepository.findById(dto.getPileId())
            .orElseThrow(() -> new RuntimeException("充电桩不存在"));

        // 2. 调用AI预测
        DurationPredictRequest predictRequest = DurationPredictRequest.builder()
            .batteryCapacity(dto.getBatteryCapacity())
            .currentSoc(dto.getCurrentSoc())
            .targetSoc(dto.getTargetSoc())
            .chargePower(pile.getPower())
            .build();

        DurationPredictResponse prediction =
            aiPredictionService.predictChargeDuration(predictRequest);

        // 3. 创建订单
        ChargeOrder order = ChargeOrder.builder()
            .userId(dto.getUserId())
            .pileId(dto.getPileId())
            .batteryCapacity(dto.getBatteryCapacity())
            .currentSoc(dto.getCurrentSoc())
            .targetSoc(dto.getTargetSoc())
            .estimatedDuration(prediction.getDuration().intValue())
            .estimatedCost(prediction.getEstimatedCost())
            .build();

        return orderRepository.save(order);
    }
}
```

### 场景2: 定期检查充电桩健康度并预测故障

```java
@Service
public class MaintenanceService {

    @Autowired
    private AIPredictionService aiPredictionService;

    @Autowired
    private ChargingPileRepository pileRepository;

    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点执行
    public void checkPileHealth() {
        List<ChargingPile> piles = pileRepository.findAll();

        for (ChargingPile pile : piles) {
            // 构建预测请求
            FaultPredictRequest request = FaultPredictRequest.builder()
                .totalChargeCount(pile.getTotalChargeCount())
                .totalChargeAmount(pile.getTotalChargeAmount())
                .daysSinceLastMaintenance(calculateDaysSinceLastMaintenance(pile))
                .healthScore(pile.getHealthScore())
                .avgDailyUsage(calculateAvgDailyUsage(pile))
                .voltageFluctuation(pile.getVoltageFluctuation())
                .faultHistoryCount(pile.getFaultHistoryCount())
                .build();

            try {
                FaultPredictResponse response =
                    aiPredictionService.predictFault(request);

                // 如果是高风险，自动创建维护计划
                if (response.getFaultProbability().doubleValue() > 70) {
                    createMaintenancePlan(pile, response);
                }

            } catch (Exception e) {
                log.error("充电桩{}故障预测失败: {}", pile.getId(), e.getMessage());
            }
        }
    }
}
```

### 场景3: 前端实时查询充电预估

```java
@RestController
@RequestMapping("/api/charging")
public class ChargingController {

    @Autowired
    private AIPredictionService aiPredictionService;

    @PostMapping("/estimate")
    public Result<Map<String, Object>> estimateCharging(
        @RequestBody EstimateRequest request) {

        // 调用AI预测
        DurationPredictRequest predictRequest = DurationPredictRequest.builder()
            .batteryCapacity(request.getBatteryCapacity())
            .currentSoc(request.getCurrentSoc())
            .targetSoc(request.getTargetSoc())
            .chargePower(request.getChargePower())
            .build();

        DurationPredictResponse response =
            aiPredictionService.predictChargeDuration(predictRequest);

        // 组装返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("duration", response.getDuration());
        result.put("cost", response.getEstimatedCost());
        result.put("estimatedEndTime",
            LocalDateTime.now().plusMinutes(response.getDuration().intValue()));

        return Result.success(result);
    }
}
```

## 异常处理

### 1. 参数验证异常

```java
try {
    DurationPredictResponse response =
        aiPredictionService.predictChargeDuration(request);
} catch (IllegalArgumentException e) {
    // 参数验证失败
    log.warn("参数验证失败: {}", e.getMessage());
    return Result.error(400, e.getMessage());
}
```

### 2. AI服务不可用

```java
try {
    DurationPredictResponse response =
        aiPredictionService.predictChargeDuration(request);
} catch (RuntimeException e) {
    if (e.getMessage().contains("AI服务暂时不可用")) {
        // 使用降级策略：简单公式计算
        BigDecimal duration = calculateDurationByFormula(request);
        return Result.success(createFallbackResponse(duration));
    }
    throw e;
}
```

### 3. 降级策略示例

```java
@Service
public class OrderService {

    @Autowired
    private AIPredictionService aiPredictionService;

    public DurationPredictResponse predictWithFallback(
        DurationPredictRequest request) {

        try {
            // 优先使用AI预测
            return aiPredictionService.predictChargeDuration(request);

        } catch (Exception e) {
            log.warn("AI预测失败，使用公式计算: {}", e.getMessage());

            // 降级：使用简单公式
            BigDecimal chargeAmount = request.getBatteryCapacity()
                .multiply(request.getTargetSoc().subtract(request.getCurrentSoc()))
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            BigDecimal duration = chargeAmount
                .divide(request.getChargePower(), 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("60"));

            BigDecimal cost = chargeAmount.multiply(new BigDecimal("1.0"));

            return new DurationPredictResponse(duration, chargeAmount, cost);
        }
    }
}
```

## 性能优化

### 1. 添加缓存（可选）

```java
@Service
public class CachedAIPredictionService {

    @Autowired
    private AIPredictionService aiPredictionService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public DurationPredictResponse predictChargeDuration(
        DurationPredictRequest request) {

        // 生成缓存key
        String cacheKey = "ai:duration:" + request.hashCode();

        // 尝试从缓存获取
        DurationPredictResponse cached =
            (DurationPredictResponse) redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            return cached;
        }

        // 调用AI服务
        DurationPredictResponse response =
            aiPredictionService.predictChargeDuration(request);

        // 缓存5分钟
        redisTemplate.opsForValue().set(cacheKey, response, 5, TimeUnit.MINUTES);

        return response;
    }
}
```

## 配置说明

### application.yml配置

```yaml
ai:
  service:
    url: http://localhost:5000  # AI服务地址
    connect-timeout: 5000       # 连接超时(毫秒)
    read-timeout: 10000         # 读取超时(毫秒)
```

### 生产环境配置

```yaml
ai:
  service:
    url: http://ai-service:5000  # Docker容器名或内网IP
    connect-timeout: 3000
    read-timeout: 8000
```

## 监控和日志

### 日志示例

```
2025-12-08 10:15:23 - 调用AI服务预测充电时长: url=http://localhost:5000/api/ai/predict/duration, request=DurationPredictRequest(batteryCapacity=75, currentSoc=20, targetSoc=80, chargePower=120, temperature=25)
2025-12-08 10:15:23 - 充电时长预测成功: duration=32.5分钟, chargeAmount=45.0kWh, estimatedCost=45.0元
```

### 建议监控指标

- AI服务调用成功率
- AI服务平均响应时间
- AI服务超时次数
- 降级策略触发次数

## 常见问题

### Q1: AI服务调用很慢怎么办？
A: 可以考虑：
1. 增加超时时间配置
2. 使用异步调用
3. 添加Redis缓存
4. 优化AI模型

### Q2: AI服务不稳定怎么办？
A: 建议实现降级策略：
1. 使用简单公式作为备选方案
2. 返回历史平均值
3. 提示用户稍后重试

### Q3: 如何测试AI集成？
A: 参考 `AI_INTEGRATION_TEST.md` 文档

## 下一步

- [ ] 在订单服务中集成充电时长预测
- [ ] 在故障管理中集成故障预测
- [ ] 实现预测结果缓存
- [ ] 添加预测历史记录
- [ ] 实现异步预测接口
