package com.ev.charging.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "valet_charge_order")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValetChargeOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 32, unique = true)
    private String orderNo;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "station_id", nullable = false)
    private Long stationId;
    @Column(name = "charger_id")
    private Long chargerId;  // 代充师傅ID
    @Column(name = "pile_id")
    private Long pileId;  // 使用的充电桩ID

    @Column(name = "car_plate", nullable = false, length = 20)
    private String carPlate;
    @Column(name = "parking_location", nullable = false, length = 255)
    private String parkingLocation;
    @Column(name = "target_soc")
    private Byte targetSoc;  // 目标电量%
    @Column(name = "pickup_time")
    private LocalDateTime pickupTime;  // 取车时间

    @Column(name = "service_fee", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal serviceFee = BigDecimal.ZERO;  // 服务费
    @Column(name = "order_status")
    @Builder.Default
    private Byte orderStatus = 1;  // 1待接单 2已接单 3充电中 4已完成 5已取消

    @Column(name = "assign_time")
    private LocalDateTime assignTime;  // 接单时间
    @Column(name = "start_charge_time")
    private LocalDateTime startChargeTime;  // 开始充电时间
    @Column(name = "complete_time")
    private LocalDateTime completeTime;  // 完成时间

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}
