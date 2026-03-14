package com.ev.charging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求DTO
 */
@Data
@Schema(description = "修改密码请求")
public class ChangePasswordDTO {

    @Schema(description = "旧密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "旧密码不能为空")
    @Size(max = 128, message = "密码长度不能超过128个字符")
    private String oldPassword;

    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度必须在6-32位之间")
    private String newPassword;
}
