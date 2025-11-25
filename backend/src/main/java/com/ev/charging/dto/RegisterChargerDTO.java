package com.ev.charging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 代充师傅注册DTO
 */
@Data
@Schema(description = "代充师傅注册请求")
public class RegisterChargerDTO {

    /**
     * 姓名
     */
    @Schema(description = "师傅姓名", example = "王师傅", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "姓名不能为空")
    @Size(min = 2, max = 50, message = "姓名长度必须在2-50个字符之间")
    private String name;

    /**
     * 手机号
     */
    @Schema(description = "联系手机号", example = "13800138000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 身份证号
     */
    @Schema(description = "身份证号", example = "110101199001011234", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;

    /**
     * 服务区域
     */
    @Schema(description = "服务区域", example = "北京市朝阳区", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "服务区域不能为空")
    @Size(max = 100, message = "服务区域长度不能超过100个字符")
    private String serviceArea;

    /**
     * 驾驶证号
     */
    @Schema(description = "驾驶证号（选填）", example = "110101199001011234")
    @Size(max = 255, message = "驾驶证号长度不能超过255个字符")
    private String drivingLicense;

    /**
     * 车牌号
     */
    @Schema(description = "车牌号（选填）", example = "京A12345")
    @Size(max = 20, message = "车牌号长度不能超过20个字符")
    private String vehiclePlate;
}
