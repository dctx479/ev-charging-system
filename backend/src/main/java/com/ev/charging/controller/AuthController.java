package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.dto.LoginDTO;
import com.ev.charging.dto.RegisterDTO;
import com.ev.charging.entity.User;
import com.ev.charging.security.LoginAttemptService;
import com.ev.charging.service.TokenBlacklistService;
import com.ev.charging.service.UserService;
import com.ev.charging.util.JwtUtil;
import com.ev.charging.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户认证控制器
 */
@Tag(name = "用户认证", description = "用户登录、注册、登出等认证相关接口")
@RestController
@RequestMapping(value = "/auth", produces = "application/json;charset=UTF-8")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;

    @Value("${jwt.expiration:86400000}")
    private Long jwtExpiration;

    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return 登录结果（包含token和用户信息）
     */
    @Operation(
            summary = "用户登录",
            description = "用户通过用户名和密码登录系统，成功后返回JWT token和用户信息"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功",
                    content = @Content(schema = @Schema(implementation = Result.class))),
            @ApiResponse(responseCode = "400", description = "用户名或密码错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/login")
    public Result<Map<String, Object>> login(
            @Parameter(description = "登录信息", required = true)
            @Valid @RequestBody LoginDTO loginDTO,
            HttpServletRequest request) {
        String ip = getClientIp(request);

        // 暴力破解防护：检查账号或IP是否被锁定
        if (loginAttemptService.isBlocked(loginDTO.getUsername(), ip)) {
            return Result.error(429, "登录尝试过于频繁，请15分钟后再试");
        }

        try {
            Map<String, Object> result = userService.login(loginDTO.getUsername(), loginDTO.getPassword());
            loginAttemptService.loginSucceeded(loginDTO.getUsername(), ip);
            return Result.success("登录成功", result);
        } catch (IllegalArgumentException e) {
            loginAttemptService.loginFailed(loginDTO.getUsername(), ip);
            throw e;
        }
    }

    /**
     * 获取客户端真实IP（兼容反向代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }

    /**
     * 用户注册（支持个人用户、企业运营商、代充师傅）
     *
     * @param registerDTO 注册信息
     * @return 注册结果（返回脱敏的用户信息）
     */
    @Operation(
            summary = "用户注册",
            description = "支持三种用户类型注册：个人用户(USER)、企业运营商(OPERATOR)、代充师傅(VALET_CHARGER)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "注册成功"),
            @ApiResponse(responseCode = "400", description = "参数校验失败或用户名已存在"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/register")
    public Result<UserVO> register(
            @Parameter(description = "注册信息", required = true)
            @Valid @RequestBody RegisterDTO registerDTO) {
        User registeredUser = userService.register(registerDTO);
        return Result.success("注册成功", UserVO.fromUser(registeredUser));
    }

    /**
     * 获取当前用户信息
     * 安全修复: 从JWT token的请求属性中获取用户ID，防止越权访问
     * 隐私保护: 返回脱敏的 UserVO 对象，不包含敏感字段
     *
     * @param userId 用户ID（由JWT拦截器从token中解析并注入到请求属性）
     * @return 用户信息（脱敏后）
     */
    @Operation(
            summary = "获取当前用户信息",
            description = "根据JWT token获取当前登录用户的详细信息",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录或token无效"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping("/user/info")
    public Result<UserVO> getUserInfo(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        User user = userService.getUserById(userId);
        return Result.success(UserVO.fromUser(user));
    }

    /**
     * 用户登出
     * 将token加入黑名单，实现真正的登出
     *
     * @param request HTTP请求对象
     * @return 退出结果
     */
    @Operation(
            summary = "用户登出",
            description = "用户退出登录，将当前token加入黑名单，实现真正的登出",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "退出成功")
    })
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        // 从请求头提取token
        String token = jwtUtil.extractToken(request);

        if (token != null) {
            // 获取token剩余有效期
            long expireSeconds = jwtUtil.getExpireSeconds(token);

            // 加入黑名单
            tokenBlacklistService.addToBlacklist(token, expireSeconds);
        }

        return Result.success("退出成功", null);
    }

    /**
     * 强制用户下线（管理员功能）
     * 将用户的所有token失效，用户需要重新登录
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @Operation(
            summary = "强制用户下线",
            description = "管理员强制指定用户下线（所有设备），用户需要重新登录",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "403", description = "无权限操作")
    })
    @PostMapping("/admin/users/{userId}/force-logout")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> forceLogout(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        // 计算token最大有效期（秒）
        long expireSeconds = jwtExpiration / 1000;

        // 将用户所有token加入黑名单
        tokenBlacklistService.blacklistUserTokens(userId, expireSeconds);

        return Result.success("用户已被强制下线", null);
    }

    /**
     * 获取黑名单统计信息（管理员功能）
     * 用于监控黑名单数量
     *
     * @return 黑名单统计信息
     */
    @Operation(
            summary = "获取黑名单统计",
            description = "管理员查看黑名单中token数量，用于系统监控",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "403", description = "无权限操作")
    })
    @GetMapping("/admin/blacklist/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getBlacklistStats() {
        long count = tokenBlacklistService.getBlacklistCount();
        return Result.success(Map.of(
            "blacklistCount", count,
            "message", "黑名单token数量"
        ));
    }
}
