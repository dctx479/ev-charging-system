package com.ev.charging.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名（唯一）
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 密码（加密存储）
     */
    @JsonIgnore
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    @Column(nullable = false)
    private String password;

    /**
     * 昵称
     */
    @Size(max = 50, message = "昵称长度不能超过50")
    @Column(length = 50)
    private String nickname;

    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Column(length = 11)
    private String phone;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    @Column(length = 100)
    private String email;

    /**
     * 用户类型：USER-普通用户，ADMIN-管理员
     */
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String userType = "USER";

    /**
     * 头像URL
     */
    @Column(length = 255)
    private String avatar;

    /**
     * 车辆型号
     */
    @Column(name = "car_model", length = 100)
    private String carModel;

    /**
     * 车牌号
     */
    @Column(name = "car_plate", length = 20)
    private String carPlate;

    /**
     * 电池容量(kWh)
     */
    @Column(name = "battery_capacity", precision = 5, scale = 2)
    private java.math.BigDecimal batteryCapacity;

    /**
     * 账户余额
     */
    @Min(value = 0, message = "账户余额不能为负数")
    @Builder.Default
    @Column(nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal balance = java.math.BigDecimal.ZERO;

    /**
     * 碳积分余额
     */
    @Min(value = 0, message = "碳积分不能为负数")
    @Builder.Default
    @Column(nullable = false)
    private Integer carbonCredits = 0;

    /**
     * 账户状态：ACTIVE-正常，DISABLED-禁用，PENDING-待审核，REJECTED-已拒绝，SUSPENDED-已停用
     */
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    /**
     * 审核拒绝原因（当status为REJECTED时填写）
     */
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    /**
     * 服务区域（代充师傅专用）
     */
    @Column(name = "service_area", length = 200)
    private String serviceArea;

    /**
     * 企业名称（企业运营商专用）
     */
    @Column(name = "company_name", length = 100)
    private String companyName;

    /**
     * 企业统一社会信用代码（企业运营商专用）
     */
    @Column(name = "business_license", length = 18)
    private String businessLicense;

    /**
     * 身份证号（代充师傅专用）
     * 隐私敏感字段，API响应中被隐藏
     */
    @JsonIgnore
    @Column(name = "id_card", length = 18)
    private String idCard;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateTime;
}
