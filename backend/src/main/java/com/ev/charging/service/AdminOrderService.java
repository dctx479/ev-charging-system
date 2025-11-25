package com.ev.charging.service;

import com.ev.charging.constant.OrderConstants;
import com.ev.charging.dto.AdminOrderQueryDTO;
import com.ev.charging.dto.RefundDTO;
import com.ev.charging.entity.*;
import com.ev.charging.repository.*;
import com.ev.charging.service.CacheService;
import com.ev.charging.vo.AdminOrderListVO;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.ev.charging.constant.OrderConstants.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理后台订单服务
 */
@Service
@Slf4j
public class AdminOrderService {

    @Autowired
    private ChargeOrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChargingPileRepository pileRepository;

    @Autowired
    private ChargingStationRepository stationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CacheService cacheService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 分页查询订单列表（支持多条件筛选）
     * 优化：批量查询关联数据，避免N+1查询
     */
    public Page<AdminOrderListVO> getOrderList(AdminOrderQueryDTO queryDTO) {
        // 创建分页对象
        Pageable pageable = PageRequest.of(
                queryDTO.getPage(),
                queryDTO.getSize(),
                Sort.by(Sort.Direction.DESC, "createTime")
        );

        // 构建动态查询条件
        Specification<ChargeOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 订单状态
            if (queryDTO.getOrderStatus() != null) {
                predicates.add(cb.equal(root.get("orderStatus"), queryDTO.getOrderStatus()));
            }

            // 支付状态
            if (queryDTO.getPaymentStatus() != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), queryDTO.getPaymentStatus()));
            }

            // 充电站ID
            if (queryDTO.getStationId() != null) {
                predicates.add(cb.equal(root.get("stationId"), queryDTO.getStationId()));
            }

            // 充电桩ID
            if (queryDTO.getPileId() != null) {
                predicates.add(cb.equal(root.get("pileId"), queryDTO.getPileId()));
            }

            // 时间范围
            if (queryDTO.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), queryDTO.getStartTime()));
            }
            if (queryDTO.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"), queryDTO.getEndTime()));
            }

            // 关键词搜索（订单号）— 转义LIKE通配符防注入
            if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().trim().isEmpty()) {
                String keyword = queryDTO.getKeyword().trim();
                String escaped = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                predicates.add(cb.like(root.get("orderNo"), "%" + escaped + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 执行查询
        Page<ChargeOrder> orderPage = orderRepository.findAll(spec, pageable);

        // 批量转换为VO，避免N+1查询
        return convertToAdminListVOBatch(orderPage);
    }

    /**
     * 获取订单详情（含用户、充电站、充电桩信息）
     * 优化：批量查询关联数据
     */
    public AdminOrderListVO getOrderDetail(Long orderId) {
        ChargeOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 批量查询关联数据
        User user = userRepository.findById(order.getUserId()).orElse(null);
        ChargingPile pile = pileRepository.findById(order.getPileId()).orElse(null);
        ChargingStation station = stationRepository.findById(order.getStationId()).orElse(null);

        return convertToAdminListVO(order, user, pile, station);
    }

    /**
     * 更新订单状态（处理异常订单）
     */
    @Transactional
    public void updateOrderStatus(Long orderId, Byte newStatus) {
        ChargeOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        Byte oldStatus = order.getOrderStatus();
        order.setOrderStatus(newStatus);
        orderRepository.save(order);

        log.info("管理员更新订单状态: orderId={}, oldStatus={}, newStatus={}", orderId, oldStatus, newStatus);

        // 如果订单状态从"进行中"变为其他状态，需要释放充电桩
        if (oldStatus != null && oldStatus == ORDER_STATUS_CHARGING
                && (newStatus == null || newStatus != ORDER_STATUS_CHARGING)) {
            ChargingPile pile = pileRepository.findById(order.getPileId()).orElse(null);
            if (pile != null && pile.getStatus() != null && pile.getStatus() == 2) {
                pile.setStatus((byte) 1); // 设为空闲
                pileRepository.save(pile);
                cacheService.evictPileCache(pile.getId());
                log.info("释放充电桩: pileId={}", pile.getId());
            }
        }
    }

    /**
     * 订单退款
     */
    @Transactional
    public void refundOrder(Long orderId, RefundDTO refundDTO) {
        // 1. 查询订单
        ChargeOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 2. 校验订单状态（先检查已退款/退款中，再检查是否已支付）
        if (order.getPaymentStatus() == PAYMENT_STATUS_REFUNDING || order.getPaymentStatus() == PAYMENT_STATUS_REFUNDED) {
            throw new RuntimeException("订单已退款，请勿重复操作");
        }

        if (order.getPaymentStatus() != PAYMENT_STATUS_PAID) {
            throw new RuntimeException("订单未支付，无法退款");
        }

        // 3. 查询支付记录
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("支付记录不存在"));

        // 4. 更新订单支付状态为"已退款" — 处理乐观锁冲突
        order.setPaymentStatus(PAYMENT_STATUS_REFUNDED);
        try {
            orderRepository.save(order);
        } catch (OptimisticLockingFailureException e) {
            log.warn("订单保存时发生乐观锁冲突: orderId={}", orderId);
            throw new RuntimeException("操作冲突，订单状态已被其他操作修改，请刷新后重试");
        }

        // 5. 更新支付记录状态为"已退款" — 处理乐观锁冲突
        payment.setPaymentStatus(PAYMENT_STATUS_REFUNDED);
        try {
            paymentRepository.save(payment);
        } catch (OptimisticLockingFailureException e) {
            log.warn("支付记录保存时发生乐观锁冲突: orderId={}", orderId);
            throw new RuntimeException("操作冲突，支付记录已被其他操作修改，请刷新后重试");
        }

        // 6. 退还用户余额（如果是余额支付）— 使用原子操作防止并发竞态
        if (order.getPaymentMethod() != null && order.getPaymentMethod() == 3) {
            BigDecimal refundAmount = order.getTotalFee() != null ? order.getTotalFee() : BigDecimal.ZERO;
            if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                int updated = userRepository.addBalanceAtomically(order.getUserId(), refundAmount);
                if (updated > 0) {
                    log.info("退还用户余额: userId={}, amount={}", order.getUserId(), refundAmount);
                } else {
                    log.warn("退还用户余额失败（用户不存在）: userId={}, amount={}", order.getUserId(), refundAmount);
                }
            }
        }

        log.info("订单退款成功: orderId={}, amount={}, reason={}, operator={}",
                orderId, refundDTO.getRefundAmount(), refundDTO.getRefundReason(), refundDTO.getOperator());
    }

    /**
     * 导出订单Excel
     * 优化：批量查询关联数据，避免N+1查询
     */
    public byte[] exportOrders(AdminOrderQueryDTO queryDTO) throws IOException {
        // 查询所有符合条件的订单（不分页）
        Specification<ChargeOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (queryDTO.getOrderStatus() != null) {
                predicates.add(cb.equal(root.get("orderStatus"), queryDTO.getOrderStatus()));
            }
            if (queryDTO.getPaymentStatus() != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), queryDTO.getPaymentStatus()));
            }
            if (queryDTO.getStationId() != null) {
                predicates.add(cb.equal(root.get("stationId"), queryDTO.getStationId()));
            }
            if (queryDTO.getPileId() != null) {
                predicates.add(cb.equal(root.get("pileId"), queryDTO.getPileId()));
            }
            if (queryDTO.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), queryDTO.getStartTime()));
            }
            if (queryDTO.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"), queryDTO.getEndTime()));
            }
            if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().trim().isEmpty()) {
                String keyword = queryDTO.getKeyword().trim();
                String escaped = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                predicates.add(cb.like(root.get("orderNo"), "%" + escaped + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<ChargeOrder> orders = orderRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createTime"));

        // 批量查询所有关联数据（优化N+1查询）
        java.util.Set<Long> userIds = orders.stream()
                .map(ChargeOrder::getUserId)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Set<Long> pileIds = orders.stream()
                .map(ChargeOrder::getPileId)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Set<Long> stationIds = orders.stream()
                .map(ChargeOrder::getStationId)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        java.util.Map<Long, ChargingPile> pileMap = pileRepository.findAllById(pileIds).stream()
                .collect(java.util.stream.Collectors.toMap(ChargingPile::getId, p -> p));

        java.util.Map<Long, ChargingStation> stationMap = stationRepository.findAllById(stationIds).stream()
                .collect(java.util.stream.Collectors.toMap(ChargingStation::getId, s -> s));

        // 创建Excel工作簿
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("充电订单");

        // 创建标题行样式
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "订单号", "用户手机号", "充电站名称", "充电桩编号", "开始时间", "结束时间",
                "充电时长(分钟)", "充电量(kWh)", "总费用(元)", "支付状态", "支付方式",
                "订单状态", "创建时间"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 4000); // 设置列宽
        }

        // 填充数据（使用批量查询的结果，避免N+1查询）
        int rowNum = 1;
        for (ChargeOrder order : orders) {
            Row row = sheet.createRow(rowNum++);

            User user = userMap.get(order.getUserId());
            ChargingPile pile = pileMap.get(order.getPileId());
            ChargingStation station = stationMap.get(order.getStationId());

            row.createCell(0).setCellValue(order.getOrderNo());
            row.createCell(1).setCellValue(user != null ? user.getPhone() : "");
            row.createCell(2).setCellValue(station != null ? station.getName() : "");
            row.createCell(3).setCellValue(pile != null ? pile.getPileNo() : "");
            row.createCell(4).setCellValue(order.getStartTime() != null ? order.getStartTime().format(DATE_TIME_FORMATTER) : "");
            row.createCell(5).setCellValue(order.getEndTime() != null ? order.getEndTime().format(DATE_TIME_FORMATTER) : "");
            row.createCell(6).setCellValue(order.getActualDuration() != null ? order.getActualDuration() : 0);
            row.createCell(7).setCellValue(order.getChargeAmount() != null ? order.getChargeAmount().doubleValue() : 0);
            row.createCell(8).setCellValue(order.getTotalFee() != null ? order.getTotalFee().doubleValue() : 0);
            row.createCell(9).setCellValue(getPaymentStatusText(order.getPaymentStatus()));
            row.createCell(10).setCellValue(getPaymentMethodText(order.getPaymentMethod()));
            row.createCell(11).setCellValue(getOrderStatusText(order.getOrderStatus()));
            row.createCell(12).setCellValue(order.getCreateTime() != null ? order.getCreateTime().format(DATE_TIME_FORMATTER) : "");
        }

        // 将工作簿写入字节数组
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        log.info("导出订单Excel: 共{}条记录", orders.size());

        return outputStream.toByteArray();
    }

    /**
     * 批量转换为管理后台列表VO（优化N+1查询）
     * 用于订单列表查询，一次性批量加载所有关联数据
     */
    private Page<AdminOrderListVO> convertToAdminListVOBatch(Page<ChargeOrder> orderPage) {
        if (orderPage.isEmpty()) {
            return Page.empty();
        }

        // 1. 收集所有userId、pileId和stationId
        java.util.Set<Long> userIds = orderPage.getContent().stream()
                .map(ChargeOrder::getUserId)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Set<Long> pileIds = orderPage.getContent().stream()
                .map(ChargeOrder::getPileId)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Set<Long> stationIds = orderPage.getContent().stream()
                .map(ChargeOrder::getStationId)
                .collect(java.util.stream.Collectors.toSet());

        // 2. 批量查询所有关联的user、pile和station（只执行3次查询，而不是N次）
        java.util.Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        java.util.Map<Long, ChargingPile> pileMap = pileRepository.findAllById(pileIds).stream()
                .collect(java.util.stream.Collectors.toMap(ChargingPile::getId, p -> p));

        java.util.Map<Long, ChargingStation> stationMap = stationRepository.findAllById(stationIds).stream()
                .collect(java.util.stream.Collectors.toMap(ChargingStation::getId, s -> s));

        // 3. 转换为VO
        return orderPage.map(order -> {
            User user = userMap.get(order.getUserId());
            ChargingPile pile = pileMap.get(order.getPileId());
            ChargingStation station = stationMap.get(order.getStationId());
            return convertToAdminListVO(order, user, pile, station);
        });
    }

    /**
     * 转换为管理后台列表VO（带关联数据）
     * 用于单个订单查询，避免N+1问题
     */
    private AdminOrderListVO convertToAdminListVO(ChargeOrder order, User user, ChargingPile pile, ChargingStation station) {
        // 用户姓名优先使用昵称，如果没有昵称则使用用户名
        String userName = "";
        if (user != null) {
            userName = (user.getNickname() != null && !user.getNickname().isEmpty())
                    ? user.getNickname()
                    : user.getUsername();
        }

        return AdminOrderListVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .userPhone(user != null ? user.getPhone() : "")
                .userNickname(user != null ? user.getNickname() : "")
                .userName(userName)
                .stationId(order.getStationId())
                .stationName(station != null ? station.getName() : "未知站点")
                .pileId(order.getPileId())
                .pileNo(pile != null ? pile.getPileNo() : "未知充电桩")
                .startTime(order.getStartTime())
                .endTime(order.getEndTime())
                .chargeDuration(order.getActualDuration())
                .chargeAmount(order.getChargeAmount())
                .electricityFee(order.getElectricityFee())
                .serviceFee(order.getServiceFee())
                .totalFee(order.getTotalFee())
                .paymentStatus(order.getPaymentStatus())
                .paymentStatusText(getPaymentStatusText(order.getPaymentStatus()))
                .paymentMethod(order.getPaymentMethod())
                .paymentMethodText(getPaymentMethodText(order.getPaymentMethod()))
                .paymentTime(order.getPaymentTime())
                .orderStatus(order.getOrderStatus())
                .orderStatusText(getOrderStatusText(order.getOrderStatus()))
                .createTime(order.getCreateTime())
                .build();
    }

    /**
     * 获取订单状态文本
     */
    private String getOrderStatusText(Byte status) {
        return OrderConstants.getOrderStatusText(status);
    }

    /**
     * 获取支付状态文本
     */
    private String getPaymentStatusText(Byte status) {
        return OrderConstants.getPaymentStatusText(status);
    }

    /**
     * 获取支付方式文本
     */
    private String getPaymentMethodText(Byte method) {
        return OrderConstants.getPaymentMethodText(method);
    }

    /**
     * 统计所有订单数量
     */
    public long countAll() {
        return orderRepository.count();
    }

    /**
     * 按订单状态统计数量
     */
    public long countByStatus(Byte status) {
        return orderRepository.countByOrderStatus(status);
    }

    /**
     * 按支付状态统计数量
     */
    public long countByPaymentStatus(Byte status) {
        return orderRepository.countByPaymentStatus(status);
    }
}
