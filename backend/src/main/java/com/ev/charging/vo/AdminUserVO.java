package com.ev.charging.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台用户详情VO
 *
 * 特点：
 * - 仅用于管理后台，包含敏感字段（idCard）供管理员审核
 * - 不包含 password 字段（认证信息不向任何端返回）
 * - 包含审核相关字段
 *
 * 安全说明：
 * - 该VO仅在管理后台接口返回，受权限控制（需要管理员权限）
 * - 敏感字段 idCard 只有管理员可见，用于审核代充师傅
 * - 前端用户接口仍使用 UserVO，不包含 idCard
 *
 * 使用场景：
 * - 管理后台用户列表（受权限保护）
 * - 管理后台用户详情（受权限保护）
 * - 代充师傅审核（受权限保护）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户类型：USER-普通用户，ADMIN-管理员，OPERATOR-运营商，VALET_CHARGER-代充师傅
     */
    private String userType;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 车辆型号
     */
    private String carModel;

    /**
     * 车牌号
     */
    private String carPlate;

    /**
     * 电池容量(kWh)
     */
    private BigDecimal batteryCapacity;

    /**
     * 账户余额
     */
    private BigDecimal balance;

    /**
     * 碳积分余额
     */
    private Integer carbonCredits;

    /**
     * 账户状态：ACTIVE-正常，DISABLED-禁用，PENDING-待审核，REJECTED-已拒绝，SUSPENDED-已停用
     */
    private String status;

    /**
     * 审核拒绝原因
     */
    private String rejectReason;

    /**
     * 服务区域（代充师傅专用）
     * 仅管理员可见
     */
    private String serviceArea;

    /**
     * 企业名称（企业运营商专用）
     * 仅管理员可见
     */
    private String companyName;

    /**
     * 企业统一社会信用代码（企业运营商专用）
     * 仅管理员可见
     */
    private String businessLicense;

    /**
     * 身份证号（代充师傅专用、管理员专用）
     *
     * 安全说明：
     * - 仅在管理后台返回，受权限控制
     * - 前端用户接口完全隐藏
     * - 用于管理员审核代充师傅身份
     */
    private String idCard;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 从 User 实体转换为管理后台VO
     *
     * 说明：该方法仅在管理后台使用，包含 idCard 等敏感字段
     */
    public static AdminUserVO fromUser(com.ev.charging.entity.User user) {
        if (user == null) {
            return null;
        }

        return AdminUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .email(user.getEmail())
                .userType(user.getUserType())
                .avatar(user.getAvatar())
                .carModel(user.getCarModel())
                .carPlate(user.getCarPlate())
                .batteryCapacity(user.getBatteryCapacity())
                .balance(user.getBalance())
                .carbonCredits(user.getCarbonCredits())
                .status(user.getStatus())
                .rejectReason(user.getRejectReason())
                .serviceArea(user.getServiceArea())
                .companyName(user.getCompanyName())
                .businessLicense(user.getBusinessLicense())
                .idCard(user.getIdCard())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }
}
