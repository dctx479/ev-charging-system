package com.ev.charging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册DTO
 */
@Data
@Schema(description = "用户注册请求")
public class RegisterDTO {

    /**
     * 用户名
     */
    @Schema(description = "用户名（3-20个字符，仅字母数字下划线）", example = "testuser", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /**
     * 密码
     */
    @Schema(description = "密码（前端SHA256加密后发送，64字符哈希串）", example = "sha256_hash", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度必须在6-128个字符之间")
    private String password;

    /**
     * 确认密码
     */
    @Schema(description = "确认密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    /**
     * 昵称
     */
    @Schema(description = "昵称", example = "测试用户")
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    /**
     * 手机号
     */
    @Schema(description = "手机号", example = "13800138000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱", example = "test@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 用户类型：USER-个人用户, OPERATOR-企业运营商, VALET_CHARGER-代充师傅
     */
    @Schema(description = "用户类型", example = "USER", allowableValues = {"USER", "OPERATOR", "VALET_CHARGER"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户类型不能为空")
    @Pattern(regexp = "^(USER|OPERATOR|VALET_CHARGER)$", message = "用户类型不正确")
    private String userType;

    /**
     * 企业名称（企业用户必填）
     */
    @Schema(description = "企业名称（企业用户必填）", example = "XX充电运营公司")
    @Size(max = 100, message = "企业名称长度不能超过100个字符")
    private String companyName;

    /**
     * 企业统一社会信用代码（企业用户必填）
     */
    @Schema(description = "企业统一社会信用代码（企业用户必填）", example = "91110000000000000X")
    @Pattern(regexp = "^[0-9A-Z]{18}$", message = "社会信用代码格式不正确（18位数字或字母）")
    private String businessLicense;

    /**
     * 车型（个人用户选填）
     */
    @Schema(description = "车型（个人用户选填）", example = "特斯拉Model 3")
    private String carModel;

    /**
     * 电池容量（个人用户选填）
     */
    @Schema(description = "电池容量（kWh，个人用户选填）", example = "75")
    private Integer batteryCapacity;

    /**
     * 服务区域（代充师傅必填）
     */
    @Schema(description = "服务区域（代充师傅必填）", example = "北京市朝阳区")
    @Size(max = 100, message = "服务区域长度不能超过100个字符")
    private String serviceArea;

    /**
     * 身份证号（代充师傅必填）
     */
    @Schema(description = "身份证号（代充师傅必填）", example = "110101199001011234")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;
}
