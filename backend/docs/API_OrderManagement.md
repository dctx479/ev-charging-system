# 管理后台订单管理 API 文档

## 概述
管理后台订单管理功能提供了完整的订单查询、状态管理、退款和数据导出功能。

## 基础路径
```
/admin/orders
```

## API 列表

### 1. 分页获取订单列表

**接口**: `GET /admin/orders`

**描述**: 支持多条件筛选的订单列表查询

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orderStatus | Byte | 否 | 订单状态：0-进行中 1-已完成 2-已取消 3-异常 |
| paymentStatus | Byte | 否 | 支付状态：0-未支付 1-已支付 2-已退款 |
| stationId | Long | 否 | 充电站ID |
| pileId | Long | 否 | 充电桩ID |
| keyword | String | 否 | 搜索关键词（订单号） |
| startTime | String | 否 | 开始时间（格式：yyyy-MM-dd HH:mm:ss） |
| endTime | String | 否 | 结束时间（格式：yyyy-MM-dd HH:mm:ss） |
| page | Integer | 否 | 页码，默认0 |
| size | Integer | 否 | 每页大小，默认10 |

**请求示例**:
```http
GET /admin/orders?orderStatus=1&paymentStatus=1&page=0&size=10
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "orderNo": "CO1701234567890ABC",
        "userId": 1,
        "userPhone": "13800138000",
        "userNickname": "张三",
        "stationId": 1,
        "stationName": "北京市朝阳区充电站",
        "pileId": 1,
        "pileNo": "P001-01",
        "startTime": "2025-12-13 10:00:00",
        "endTime": "2025-12-13 11:30:00",
        "chargeDuration": 90,
        "chargeAmount": 45.5,
        "totalFee": 68.25,
        "paymentStatus": 1,
        "paymentStatusText": "已支付",
        "paymentMethod": 1,
        "paymentMethodText": "微信",
        "paymentTime": "2025-12-13 11:35:00",
        "orderStatus": 1,
        "orderStatusText": "已完成",
        "createTime": "2025-12-13 10:00:00"
      }
    ],
    "totalElements": 100,
    "totalPages": 10,
    "size": 10,
    "number": 0
  }
}
```

---

### 2. 获取订单详情

**接口**: `GET /admin/orders/{id}`

**描述**: 根据订单ID获取订单完整信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 订单ID |

**请求示例**:
```http
GET /admin/orders/1
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "orderNo": "CO1701234567890ABC",
    "userId": 1,
    "stationId": 1,
    "pileId": 1,
    "startTime": "2025-12-13 10:00:00",
    "endTime": "2025-12-13 11:30:00",
    "chargeDuration": 90,
    "chargeAmount": 45.5,
    "electricityFee": 45.5,
    "serviceFee": 22.75,
    "totalFee": 68.25,
    "paymentStatus": 1,
    "paymentMethod": 1,
    "paymentTime": "2025-12-13 11:35:00",
    "orderStatus": 1,
    "startSoc": 20,
    "endSoc": 80,
    "chargeMode": 1,
    "targetValue": 80,
    "createTime": "2025-12-13 10:00:00",
    "updateTime": "2025-12-13 11:35:00"
  }
}
```

---

### 3. 更新订单状态

**接口**: `PUT /admin/orders/{id}/status`

**描述**: 管理员更新订单状态（用于处理异常订单）

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 订单ID |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | Byte | 是 | 新状态：0-进行中 1-已完成 2-已取消 3-异常 |

**请求示例**:
```http
PUT /admin/orders/1/status?status=3
```

**响应示例**:
```json
{
  "code": 200,
  "message": "订单状态更新成功",
  "data": null
}
```

**注意事项**:
- 如果订单状态从"进行中"(0)变为其他状态，系统会自动释放占用的充电桩
- 状态值必须在0-3之间

---

### 4. 订单退款

**接口**: `POST /admin/orders/{id}/refund`

**描述**: 管理员对已支付订单进行退款操作

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 订单ID |

**请求体**:
```json
{
  "reason": "用户投诉，充电设备故障",
  "operator": "admin_zhang"
}
```

**字段说明**:
| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| reason | String | 是 | 退款原因 |
| operator | String | 否 | 操作人（管理员） |

**请求示例**:
```http
POST /admin/orders/1/refund
Content-Type: application/json

{
  "reason": "用户投诉，充电设备故障",
  "operator": "admin_zhang"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "退款成功",
  "data": null
}
```

**业务逻辑**:
1. 验证订单支付状态（必须是"已支付"）
2. 更新订单支付状态为"已退款"(2)
3. 更新支付记录状态为"已退款"(3)
4. 如果是余额支付，退还金额到用户余额
5. 记录退款日志

**错误情况**:
- 订单未支付：`"订单未支付，无法退款"`
- 订单已退款：`"订单已退款，请勿重复操作"`
- 订单不存在：`"订单不存在"`

---

### 5. 导出订单Excel

**接口**: `GET /admin/orders/export`

**描述**: 根据查询条件导出订单数据到Excel文件

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| orderStatus | Byte | 否 | 订单状态 |
| paymentStatus | Byte | 否 | 支付状态 |
| stationId | Long | 否 | 充电站ID |
| pileId | Long | 否 | 充电桩ID |
| keyword | String | 否 | 搜索关键词 |
| startTime | String | 否 | 开始时间 |
| endTime | String | 否 | 结束时间 |

**请求示例**:
```http
GET /admin/orders/export?orderStatus=1&paymentStatus=1&startTime=2025-12-01 00:00:00&endTime=2025-12-31 23:59:59
```

**响应**:
- Content-Type: `application/octet-stream`
- Content-Disposition: `attachment; filename="充电订单_20251213_143020.xlsx"`
- 返回Excel文件的二进制数据

**Excel内容**:
| 列名 | 说明 |
|------|------|
| 订单号 | 订单唯一编号 |
| 用户手机号 | 用户手机号 |
| 充电站名称 | 充电站名称 |
| 充电桩编号 | 充电桩编号 |
| 开始时间 | 充电开始时间 |
| 结束时间 | 充电结束时间 |
| 充电时长(分钟) | 实际充电时长 |
| 充电量(kWh) | 实际充电量 |
| 总费用(元) | 订单总费用 |
| 支付状态 | 未支付/已支付/已退款 |
| 支付方式 | 微信/支付宝/余额 |
| 订单状态 | 进行中/已完成/已取消/异常 |
| 创建时间 | 订单创建时间 |

**前端调用示例**:
```javascript
// Axios示例
axios({
  url: '/admin/orders/export',
  method: 'GET',
  params: {
    orderStatus: 1,
    paymentStatus: 1,
    startTime: '2025-12-01 00:00:00',
    endTime: '2025-12-31 23:59:59'
  },
  responseType: 'blob'
}).then(response => {
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', `充电订单_${Date.now()}.xlsx`);
  document.body.appendChild(link);
  link.click();
  link.remove();
});
```

---

## 数据字典

### 订单状态 (orderStatus)
| 值 | 说明 |
|----|------|
| 0 | 进行中 |
| 1 | 已完成 |
| 2 | 已取消 |
| 3 | 异常 |

### 支付状态 (paymentStatus)
| 值 | 说明 |
|----|------|
| 0 | 未支付 |
| 1 | 已支付 |
| 2 | 已退款 |

### 支付方式 (paymentMethod)
| 值 | 说明 |
|----|------|
| 1 | 微信 |
| 2 | 支付宝 |
| 3 | 余额 |

### 充电模式 (chargeMode)
| 值 | 说明 |
|----|------|
| 1 | 充满 |
| 2 | 按金额 |
| 3 | 按电量 |
| 4 | 按时间 |

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 500 | 系统错误 |

**错误响应示例**:
```json
{
  "code": 500,
  "message": "订单不存在",
  "data": null
}
```

---

## 依赖说明

### 新增依赖
在 `pom.xml` 中添加了 Apache POI 用于 Excel 导出：

```xml
<!-- Apache POI for Excel Export -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi</artifactId>
    <version>5.2.5</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

---

## 技术实现

### 动态查询
使用 Spring Data JPA Specification 实现动态查询，支持灵活的多条件组合：

```java
Specification<ChargeOrder> spec = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();

    if (queryDTO.getOrderStatus() != null) {
        predicates.add(cb.equal(root.get("orderStatus"), queryDTO.getOrderStatus()));
    }

    // ... 更多条件

    return cb.and(predicates.toArray(new Predicate[0]));
};
```

### Excel 导出
使用 Apache POI XSSF (Excel 2007+) 格式：

```java
Workbook workbook = new XSSFWorkbook();
Sheet sheet = workbook.createSheet("充电订单");
// 创建标题行和数据行...
```

### 事务管理
退款操作使用 `@Transactional` 确保数据一致性：

```java
@Transactional
public void refundOrder(Long orderId, RefundDTO refundDTO) {
    // 更新订单状态
    // 更新支付记录
    // 退还用户余额（如果适用）
}
```

---

## 测试建议

### 1. 订单列表查询测试
```bash
# 查询所有订单
curl "http://localhost:8080/admin/orders?page=0&size=10"

# 按状态筛选
curl "http://localhost:8080/admin/orders?orderStatus=1&paymentStatus=1"

# 按时间范围筛选
curl "http://localhost:8080/admin/orders?startTime=2025-12-01%2000:00:00&endTime=2025-12-31%2023:59:59"
```

### 2. 订单详情测试
```bash
curl "http://localhost:8080/admin/orders/1"
```

### 3. 更新订单状态测试
```bash
curl -X PUT "http://localhost:8080/admin/orders/1/status?status=3"
```

### 4. 订单退款测试
```bash
curl -X POST "http://localhost:8080/admin/orders/1/refund" \
  -H "Content-Type: application/json" \
  -d '{"reason":"设备故障","operator":"admin"}'
```

### 5. 导出Excel测试
```bash
curl "http://localhost:8080/admin/orders/export?orderStatus=1" \
  -o orders.xlsx
```

---

## 注意事项

1. **权限控制**: 当前接口未实现权限验证，生产环境需要添加管理员权限校验
2. **日志记录**: 所有关键操作都有日志记录，便于审计
3. **事务回滚**: 退款等关键操作使用事务，失败时自动回滚
4. **数据校验**: 使用 `@Valid` 注解进行参数校验
5. **异常处理**: 统一异常处理，返回友好的错误信息
6. **性能优化**: 导出大量数据时建议添加分页或限制导出数量

---

## 更新日志

### v1.0.0 (2025-12-13)
- 实现订单列表分页查询
- 实现多条件动态筛选
- 实现订单状态管理
- 实现订单退款功能
- 实现Excel导出功能
