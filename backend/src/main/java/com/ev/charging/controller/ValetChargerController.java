package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.dto.CompleteValetOrderDTO;
import com.ev.charging.dto.CreateValetOrderDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ev.charging.dto.RegisterChargerDTO;
import com.ev.charging.entity.ValetChargeOrder;
import com.ev.charging.entity.ValetCharger;
import com.ev.charging.exception.BusinessException;
import com.ev.charging.repository.ValetChargeOrderRepository;
import com.ev.charging.repository.ValetChargerRepository;
import com.ev.charging.vo.ValetChargeOrderVO;
import com.ev.charging.vo.ValetChargerVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "代充服务", description = "代充师傅注册、订单管理等代充服务接口")
@RestController
@RequestMapping("/valet")
@RequiredArgsConstructor
public class ValetChargerController {

    private final ValetChargerRepository valetChargerRepository;
    private final ValetChargeOrderRepository valetChargeOrderRepository;
    private final com.ev.charging.repository.ChargingStationRepository chargingStationRepository;

    @PostMapping("/charger/register")
    public Result<Long> registerCharger(
        @RequestAttribute("userId") Long userId,
        @RequestBody @Valid RegisterChargerDTO dto) {
        // 1. 检查用户是否已注册为代充师傅（防止重复注册）
        if (valetChargerRepository.findByUserId(userId).isPresent()) {
            return Result.error("您已注册为代充师傅，不可重复注册");
        }

        // 2. 创建代充师傅账号
        ValetCharger charger = ValetCharger.builder()
            .userId(userId)  // 绑定用户ID，用于所有权验证
            .name(dto.getName())
            .phone(dto.getPhone())
            .idCard(maskIdCard(dto.getIdCard()))  // 脱敏存储
            .serviceArea(dto.getServiceArea())
            .drivingLicense(dto.getDrivingLicense())
            .vehiclePlate(dto.getVehiclePlate())
            .status((byte) 3)  // 离线
            .isCertified((byte) 0)  // 待认证
            .build();

        valetChargerRepository.save(charger);

        return Result.success(charger.getId());
    }

    @GetMapping("/charger/list")
    public Result<List<ValetChargerVO>> getChargerList(
        @RequestParam(required = false) String serviceArea,
        @RequestParam(required = false) Byte status
    ) {
        List<ValetCharger> chargers;

        if (serviceArea != null && status != null) {
            chargers = valetChargerRepository.findByServiceAreaAndStatus(serviceArea, status);
        } else if (status != null) {
            chargers = valetChargerRepository.findByStatus(status);
        } else {
            chargers = valetChargerRepository.findAll();
        }

        return Result.success(chargers.stream()
            .map(this::convertToChargerVO)
            .collect(Collectors.toList())
        );
    }

    /**
     * 获取当前用户的代充师傅信息
     * 用于代充工作台初始化，根据JWT userId查找对应charger记录
     */
    @GetMapping("/charger/my")
    public Result<ValetChargerVO> getMyCharger(
        @RequestAttribute("userId") Long userId
    ) {
        ValetCharger charger = valetChargerRepository.findByUserId(userId)
            .orElseThrow(() -> new BusinessException("您尚未注册为代充师傅"));
        return Result.success(convertToChargerVO(charger));
    }

    @PutMapping("/charger/{chargerId}/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> auditCharger(
        @PathVariable Long chargerId,
        @RequestParam Byte isCertified  // 1已认证 2已拒绝
    ) {
        ValetCharger charger = valetChargerRepository.findById(chargerId)
            .orElseThrow(() -> new BusinessException("代充师傅不存在"));

        charger.setIsCertified(isCertified);
        valetChargerRepository.save(charger);

        return Result.success();
    }

    @PutMapping("/charger/{chargerId}/status")
    public Result<Void> updateChargerStatus(
        @PathVariable Long chargerId,
        @RequestAttribute("userId") Long userId,
        @RequestParam Byte status  // 1在线 2忙碌 3离线
    ) {
        ValetCharger charger = valetChargerRepository.findById(chargerId)
            .orElseThrow(() -> new BusinessException("代充师傅不存在"));

        // IDOR防护：验证当前用户是否为该师傅的所有者
        if (!charger.getUserId().equals(userId)) {
            return Result.error("无权修改该师傅的状态");
        }

        charger.setStatus(status);
        valetChargerRepository.save(charger);

        return Result.success();
    }

    @PostMapping("/order/create")
    public Result<String> createValetOrder(
            @RequestAttribute("userId") Long userId,
            @RequestBody @Valid CreateValetOrderDTO dto) {
        // 1. 生成订单号（时间戳+UUID片段防止并发碰撞）
        String orderNo = "VO" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6);

        // 2. 创建代充订单（userId 来自 JWT，防止 IDOR）
        ValetChargeOrder order = ValetChargeOrder.builder()
            .orderNo(orderNo)
            .userId(userId)
            .stationId(dto.getStationId())
            .pileId(dto.getPileId())
            .chargerId(dto.getChargerId())
            .carPlate(dto.getCarPlate())
            .parkingLocation(dto.getParkingLocation())
            .targetSoc(dto.getTargetSoc())
            .pickupTime(dto.getPickupTime())
            .serviceFee(BigDecimal.valueOf(30.0))  // 固定服务费30元
            .orderStatus(dto.getChargerId() != null ? (byte) 2 : (byte) 1)  // 指定师傅则直接为已接单，否则待接单
            .assignTime(dto.getChargerId() != null ? LocalDateTime.now() : null)
            .build();

        valetChargeOrderRepository.save(order);

        // 3. 推送订单到在线的代充师傅
        List<ValetCharger> onlineChargers = valetChargerRepository.findByStatus((byte) 1);

        for (ValetCharger charger : onlineChargers) {
            // 发送推送通知(实际应该用推送服务)
            // sendPushNotification(charger.getPhone(), "新的代充订单", orderNo);
        }

        return Result.success(orderNo);
    }

    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/order/{orderNo}/accept")
    public Result<Void> acceptOrder(
        @PathVariable String orderNo,
        @RequestAttribute("userId") Long userId
    ) {
        // IDOR防护：验证当前用户是否已注册为代充师傅
        ValetCharger charger = valetChargerRepository.findByUserId(userId)
            .orElseThrow(() -> new BusinessException("您未注册为代充师傅，无法接单"));

        Long chargerId = charger.getId();

        // 验证师傅已认证且在线
        if (charger.getIsCertified() != 1) {
            return Result.error("您的资质未认证，无法接单");
        }

        // 原子性接单（CAS：orderStatus=1 才更新，防止并发竞态条件）
        int updated = valetChargeOrderRepository.acceptOrderAtomically(orderNo, chargerId, LocalDateTime.now());
        if (updated == 0) {
            return Result.error("操作失败，订单不存在或已被接单");
        }

        // 更新师傅状态为"忙碌"
        charger.setStatus((byte) 2);
        valetChargerRepository.save(charger);

        return Result.success();
    }

    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/order/{orderNo}/complete")
    public Result<Void> completeOrder(
        @PathVariable String orderNo,
        @RequestBody @Valid CompleteValetOrderDTO dto,
        @RequestAttribute("userId") Long userId
    ) {
        // 1. 查询订单
        ValetChargeOrder order = valetChargeOrderRepository.findByOrderNo(orderNo)
            .orElseThrow(() -> new BusinessException("订单不存在"));

        // 检查订单状态：只允许已接单(2)或充电中(3)的订单被完成
        Byte currentStatus = order.getOrderStatus();
        if (currentStatus == null || (currentStatus != 2 && currentStatus != 3)) {
            return Result.error("订单状态不允许完成操作，当前状态：" + currentStatus);
        }

        // IDOR防护：验证当前用户是否为该订单的指定师傅
        if (!order.getChargerId().equals(valetChargerRepository.findByUserId(userId)
            .map(ValetCharger::getId)
            .orElse(null))) {
            return Result.error("无权完成该订单，您不是该订单的指定师傅");
        }

        // 2. 完成订单
        order.setOrderStatus((byte) 4);  // 已完成
        order.setCompleteTime(LocalDateTime.now());
        order.setPileId(dto.getPileId());
        valetChargeOrderRepository.save(order);

        // 3. 更新师傅状态和统计
        ValetCharger charger = valetChargerRepository.findById(order.getChargerId())
            .orElseThrow(() -> new BusinessException("代充师傅不存在"));
        charger.setStatus((byte) 1);  // 在线
        charger.setCompletedOrders(charger.getCompletedOrders() + 1);
        valetChargerRepository.save(charger);

        // 4. 通知用户
        // sendPushNotification(order.getUserId(), "代充服务已完成", "请查收您的车辆");

        return Result.success();
    }

    @GetMapping("/order/my")
    public Result<List<ValetChargeOrderVO>> getMyOrders(
            @RequestAttribute("userId") Long userId) {
        List<ValetChargeOrder> orders = valetChargeOrderRepository
            .findByUserIdOrderByCreateTimeDesc(userId);

        return Result.success(convertToOrderVOBatch(orders));
    }

    @GetMapping("/order/charger/{chargerId}")
    public Result<List<ValetChargeOrderVO>> getChargerOrders(
        @PathVariable Long chargerId,
        @RequestAttribute("userId") Long userId
    ) {
        // IDOR防护：验证当前用户是否为该师傅的所有者
        ValetCharger charger = valetChargerRepository.findById(chargerId)
            .orElseThrow(() -> new BusinessException("代充师傅不存在"));

        if (!charger.getUserId().equals(userId)) {
            return Result.error("无权查看该师傅的订单");
        }

        List<ValetChargeOrder> orders = valetChargeOrderRepository
            .findByChargerIdOrderByCreateTimeDesc(chargerId);

        return Result.success(convertToOrderVOBatch(orders));
    }

    /**
     * 管理端获取所有代充订单
     */
    @GetMapping("/order/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<ValetChargeOrderVO>> adminGetAllOrders() {
        List<ValetChargeOrder> orders = valetChargeOrderRepository.findAllByOrderByCreateTimeDesc();
        return Result.success(convertToOrderVOBatch(orders));
    }

    /**
     * 获取代充业务统计数据（需要ADMIN或OPERATOR角色）
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        // 师傅总数
        long chargerCount = valetChargerRepository.count();
        statistics.put("chargerCount", chargerCount);

        // 今日订单数和今日收入
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        List<ValetChargeOrder> allOrders = valetChargeOrderRepository.findAllByOrderByCreateTimeDesc();
        long todayOrders = allOrders.stream()
            .filter(o -> o.getCreateTime() != null
                && o.getCreateTime().isAfter(todayStart)
                && o.getCreateTime().isBefore(todayEnd))
            .count();
        BigDecimal todayIncome = allOrders.stream()
            .filter(o -> o.getCreateTime() != null
                && o.getCreateTime().isAfter(todayStart)
                && o.getCreateTime().isBefore(todayEnd)
                && o.getOrderStatus() != null && o.getOrderStatus() == 4)
            .map(o -> o.getServiceFee() != null ? o.getServiceFee() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        statistics.put("todayOrders", todayOrders);
        statistics.put("todayIncome", todayIncome);
        statistics.put("satisfaction", 98); // TODO: 从评价数据计算

        return Result.success(statistics);
    }

    /**
     * 脱敏身份证号（非加密，不可逆）
     */
    private String maskIdCard(String idCard) {
        // 身份证脱敏处理：保留前6位和后4位
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }

    private ValetChargerVO convertToChargerVO(ValetCharger charger) {
        ValetChargerVO vo = new ValetChargerVO();
        BeanUtils.copyProperties(charger, vo);
        vo.setOrderCount(charger.getCompletedOrders());  // 前端使用orderCount字段
        vo.setServicePrice(charger.getServicePrice());   // 确保设置服务价格
        vo.setAvatar(charger.getAvatar());               // 确保设置头像
        return vo;
    }

    /**
     * 批量转换为VO（优化N+1查询）
     * 用于订单列表查询，一次性批量加载所有师傅和站点数据
     * Fix: 改为批量查询，避免循环内的逐条 findById 导致的 N+1 问题
     */
    private List<ValetChargeOrderVO> convertToOrderVOBatch(List<ValetChargeOrder> orders) {
        if (orders.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        // 1. 收集所有 chargerId 和 stationId
        java.util.Set<Long> chargerIds = orders.stream()
                .map(ValetChargeOrder::getChargerId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Set<Long> stationIds = orders.stream()
                .map(ValetChargeOrder::getStationId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        // 2. 批量查询所有师傅和站点（只执行2次查询，而不是 N 次）
        java.util.Map<Long, ValetCharger> chargerMap = valetChargerRepository.findAllById(chargerIds).stream()
                .collect(java.util.stream.Collectors.toMap(ValetCharger::getId, c -> c));

        java.util.Map<Long, com.ev.charging.entity.ChargingStation> stationMap =
                chargingStationRepository.findAllById(stationIds).stream()
                .collect(java.util.stream.Collectors.toMap(com.ev.charging.entity.ChargingStation::getId, s -> s));

        // 3. 转换为VO（使用预加载的数据）
        return orders.stream()
                .map(order -> convertToOrderVOWithData(order, chargerMap, stationMap))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 单个订单转换为VO（使用预加载的数据）
     */
    private ValetChargeOrderVO convertToOrderVOWithData(
            ValetChargeOrder order,
            java.util.Map<Long, ValetCharger> chargerMap,
            java.util.Map<Long, com.ev.charging.entity.ChargingStation> stationMap) {
        ValetChargeOrderVO vo = new ValetChargeOrderVO();
        BeanUtils.copyProperties(order, vo);

        // 设置兼容字段
        vo.setStatus(order.getOrderStatus());
        vo.setServicePrice(order.getServiceFee());
        vo.setCarLocation(order.getParkingLocation());
        vo.setEvaluated(false);  // TODO: 从评价表查询

        // 总费用 = 服务费（代充订单DDL中无chargeFee字段，totalFee即serviceFee）
        BigDecimal totalFee = order.getServiceFee() != null ? order.getServiceFee() : BigDecimal.ZERO;
        vo.setTotalFee(totalFee);

        // 填充师傅信息（使用预加载的数据，不再查询数据库）
        if (order.getChargerId() != null && chargerMap.containsKey(order.getChargerId())) {
            ValetCharger charger = chargerMap.get(order.getChargerId());
            vo.setChargerName(charger.getName());
            vo.setChargerPhone(charger.getPhone());
            vo.setChargerAvatar(charger.getAvatar());
        }

        // 填充站点信息（使用预加载的数据，不再查询数据库）
        if (order.getStationId() != null && stationMap.containsKey(order.getStationId())) {
            com.ev.charging.entity.ChargingStation station = stationMap.get(order.getStationId());
            vo.setStationName(station.getName());
        }

        return vo;
    }

    /**
     * 单个订单转换为VO（仅用于单个订单查询）
     * 如果需要查询单个订单，应该批量查询后使用 convertToOrderVOBatch
     */
}
