# 峰谷平电价计算修复说明

## 修复概述

修复了订单系统中的电价计算逻辑，从简单的"按开始时刻计费"改为精确的"跨时段分段计费"。

## 修改文件

### 1. 新增 VO 类
**文件**: `backend/src/main/java/com/ev/charging/vo/ChargeFeeDetail.java`

这是一个详细的费用明细类，包含：
- 各时段电量分配（谷时、平时、峰时）
- 各时段费用明细
- 各时段充电分钟数
- 总电费、服务费、总费用

### 2. 修改服务类
**文件**: `backend/src/main/java/com/ev/charging/service/OrderService.java`

#### 修改的方法：

**`calculateElectricityFee()`** - 核心计费方法
- 旧逻辑：按开始时刻所在时段的电价 × 总电量
- 新逻辑：按分钟遍历充电时段，按各时段占比分配电量后分别计费

**`getPeriodByTime()`** - 时段判断方法
- 旧逻辑：返回 BigDecimal 电价
- 新逻辑：返回 String 时段标识（"valley", "flat", "peak"）

#### 新增的方法：

**`calculateChargeFeeDetail()`** - 公开方法
- 返回详细的费用明细对象
- 可用于前端展示详细账单

## 计算逻辑详解

### 电价时段划分

```
谷时（23:00-07:00）: 0.4元/kWh
平时（07:00-10:00, 15:00-18:00, 21:00-23:00）: 0.8元/kWh
峰时（10:00-15:00, 18:00-21:00）: 1.2元/kWh
```

### 计算步骤

#### 1. 计算总充电时长（分钟）
```java
long totalMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
```

#### 2. 按分钟遍历，统计各时段分钟数
```java
LocalDateTime currentTime = startTime;
for (long i = 0; i < totalMinutes; i++) {
    String period = getPeriodByTime(currentTime.toLocalTime());
    periodMinutes.put(period, periodMinutes.get(period) + 1);
    currentTime = currentTime.plusMinutes(1);
}
```

#### 3. 按时段分钟数占比分配电量
```java
BigDecimal valleyAmount = totalAmount.multiply(BigDecimal.valueOf(valleyMinutes))
    .divide(BigDecimal.valueOf(totalMinutes), 4, RoundingMode.HALF_UP);
```

#### 4. 分别计算各时段费用并累加
```java
BigDecimal valleyFee = valleyAmount.multiply(BigDecimal.valueOf(0.4))
    .setScale(2, RoundingMode.HALF_UP);
BigDecimal totalFee = valleyFee.add(flatFee).add(peakFee);
```

## 计算示例

### 示例 1: 跨谷时和平时充电

**充电信息**:
- 开始时间: 2025-12-14 06:30
- 结束时间: 2025-12-14 08:30
- 充电时长: 120分钟
- 充电量: 60 kWh

**时段分布**:
- 谷时（06:30-07:00）: 30分钟
- 平时（07:00-08:30）: 90分钟

**电量分配**:
- 谷时电量: 60 × (30/120) = 15 kWh
- 平时电量: 60 × (90/120) = 45 kWh

**费用计算**:
- 谷时电费: 15 × 0.4 = 6.00元
- 平时电费: 45 × 0.8 = 36.00元
- **总电费**: 42.00元
- 服务费: 60 × 0.5 = 30.00元
- **总费用**: 72.00元

**旧算法计算**（仅供对比）:
- 开始时刻06:30属于谷时，按谷时电价计算
- 总电费: 60 × 0.4 = 24.00元
- **误差**: 多收了 18.00元！

### 示例 2: 跨谷时、平时、峰时充电

**充电信息**:
- 开始时间: 2025-12-14 22:00
- 结束时间: 2025-12-15 11:00
- 充电时长: 780分钟（13小时）
- 充电量: 90 kWh

**时段分布**:
- 平时（22:00-23:00）: 60分钟
- 谷时（23:00-07:00）: 480分钟
- 平时（07:00-10:00）: 180分钟
- 峰时（10:00-11:00）: 60分钟

**电量分配**:
- 谷时电量: 90 × (480/780) = 55.38 kWh
- 平时电量: 90 × (240/780) = 27.69 kWh
- 峰时电量: 90 × (60/780) = 6.92 kWh

**费用计算**:
- 谷时电费: 55.38 × 0.4 = 22.15元
- 平时电费: 27.69 × 0.8 = 22.15元
- 峰时电费: 6.92 × 1.2 = 8.31元
- **总电费**: 52.61元
- 服务费: 90 × 0.5 = 45.00元
- **总费用**: 97.61元

**旧算法计算**（仅供对比）:
- 开始时刻22:00属于平时，按平时电价计算
- 总电费: 90 × 0.8 = 72.00元
- **误差**: 少收了 19.39元！

## 日志输出示例

修复后的代码会输出详细的计费日志：

```
INFO  电费计算明细: 总时长=780分钟, 总电量=90.00kWh |
      谷时: 480分钟, 55.38kWh, 22.15元 |
      平时: 240分钟, 27.69kWh, 22.15元 |
      峰时: 60分钟, 6.92kWh, 8.31元 |
      总电费: 52.61元
```

## 前端集成建议

### 1. 订单详情页面可以调用详细计费接口

```java
// Controller 示例（需要添加）
@GetMapping("/orders/{orderId}/fee-detail")
public Result<ChargeFeeDetail> getOrderFeeDetail(@PathVariable Long orderId) {
    ChargeOrder order = orderService.getOrderById(orderId);
    ChargeFeeDetail detail = orderService.calculateChargeFeeDetail(
        order.getStartTime(),
        order.getEndTime(),
        order.getChargeAmount()
    );
    return Result.success(detail);
}
```

### 2. 前端展示费用明细

```vue
<el-descriptions title="费用明细" :column="2" border>
  <el-descriptions-item label="充电时长">
    {{ feeDetail.totalMinutes }}分钟
  </el-descriptions-item>
  <el-descriptions-item label="充电量">
    {{ order.chargeAmount }}kWh
  </el-descriptions-item>

  <el-descriptions-item label="谷时电量">
    {{ feeDetail.valleyAmount }}kWh ({{ feeDetail.valleyMinutes }}分钟)
  </el-descriptions-item>
  <el-descriptions-item label="谷时电费">
    {{ feeDetail.valleyFee }}元 (0.4元/kWh)
  </el-descriptions-item>

  <el-descriptions-item label="平时电量">
    {{ feeDetail.flatAmount }}kWh ({{ feeDetail.flatMinutes }}分钟)
  </el-descriptions-item>
  <el-descriptions-item label="平时电费">
    {{ feeDetail.flatFee }}元 (0.8元/kWh)
  </el-descriptions-item>

  <el-descriptions-item label="峰时电量">
    {{ feeDetail.peakAmount }}kWh ({{ feeDetail.peakMinutes }}分钟)
  </el-descriptions-item>
  <el-descriptions-item label="峰时电费">
    {{ feeDetail.peakFee }}元 (1.2元/kWh)
  </el-descriptions-item>

  <el-descriptions-item label="总电费">
    {{ feeDetail.totalElectricityFee }}元
  </el-descriptions-item>
  <el-descriptions-item label="服务费">
    {{ feeDetail.serviceFee }}元 (0.5元/kWh)
  </el-descriptions-item>

  <el-descriptions-item label="总费用" :span="2">
    <span style="color: #e6a23c; font-size: 18px; font-weight: bold;">
      {{ feeDetail.totalAmount }}元
    </span>
  </el-descriptions-item>
</el-descriptions>
```

## 性能考虑

### 潜在性能问题
对于超长充电时长（如几天的充电），按分钟遍历可能会有性能问题。

### 优化方案（可选）
如果发现性能瓶颈，可以优化为按小时遍历后再补充边界分钟：

```java
// 优化思路（仅供参考，当前实现已足够）
// 1. 整小时遍历 (startHour to endHour)
// 2. 处理开始时刻的零头分钟
// 3. 处理结束时刻的零头分钟
```

但对于一般充电场景（几小时内），当前实现的性能完全足够。

## 测试建议

### 单元测试用例

```java
@Test
public void testCrossTimePeriodCalculation() {
    // 测试跨谷时和平时
    LocalDateTime start = LocalDateTime.of(2025, 12, 14, 6, 30);
    LocalDateTime end = LocalDateTime.of(2025, 12, 14, 8, 30);
    BigDecimal amount = BigDecimal.valueOf(60);

    BigDecimal fee = orderService.calculateElectricityFee(start, end, amount);

    // 预期: 15*0.4 + 45*0.8 = 42.00元
    assertEquals(BigDecimal.valueOf(42.00).setScale(2), fee);
}

@Test
public void testFullDayCrossAllPeriods() {
    // 测试跨所有时段
    LocalDateTime start = LocalDateTime.of(2025, 12, 14, 22, 0);
    LocalDateTime end = LocalDateTime.of(2025, 12, 15, 11, 0);
    BigDecimal amount = BigDecimal.valueOf(90);

    ChargeFeeDetail detail = orderService.calculateChargeFeeDetail(start, end, amount);

    assertEquals(780L, detail.getTotalMinutes().longValue());
    assertEquals(480L, detail.getValleyMinutes().longValue());
    assertEquals(240L, detail.getFlatMinutes().longValue());
    assertEquals(60L, detail.getPeakMinutes().longValue());
}
```

## 回归测试检查清单

- [ ] 现有订单结算功能正常
- [ ] 费用计算结果准确
- [ ] 日志输出正常
- [ ] 前端订单详情页面显示正常
- [ ] 跨午夜充电计费正确
- [ ] 边界时刻（如07:00, 10:00等）计费正确

## 总结

此次修复实现了准确的跨时段电价计费，主要改进：

1. **准确性**: 从粗糙的单一时段计费改为精确的分段计费
2. **透明性**: 提供详细的费用明细，用户可以看到各时段的电量和费用分配
3. **可扩展性**: 新增的 `ChargeFeeDetail` 类可以方便地支持前端展示
4. **可维护性**: 代码逻辑清晰，易于理解和维护

**修复日期**: 2025-12-14
**修复版本**: v1.1.0
