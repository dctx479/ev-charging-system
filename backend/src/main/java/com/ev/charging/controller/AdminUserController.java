package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.entity.User;
import com.ev.charging.service.UserService;
import com.ev.charging.vo.AdminUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台 - 用户管理控制器
 */
@Tag(name = "管理后台 - 用户管理", description = "用户列表查询、代充师傅管理、企业运营商审核等功能（需要管理员权限）")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    private final UserService userService;

    /**
     * 获取用户列表（分页）
     *
     * @param page     页码（从0开始）
     * @param size     每页数量
     * @param userType 用户类型筛选（可选）
     * @param status   状态筛选（可选）
     * @param keyword  搜索关键词（用户名、手机号）
     * @return 用户列表（返回AdminUserVO，包含身份证号用于审核）
     */
    @Operation(
        summary = "获取用户列表（分页）",
        description = "管理员查询所有用户列表，支持按用户类型、状态、关键词筛选，支持分页查询",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要管理员权限"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/list")
    public Result<Page<AdminUserVO>> getUserList(
            @Parameter(description = "页码（从0开始）", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "用户类型（NORMAL-普通用户, VALET_CHARGER-代充师傅, OPERATOR-运营商）", example = "NORMAL")
            @RequestParam(required = false) String userType,
            @Parameter(description = "用户状态（ACTIVE-激活, SUSPENDED-停用, PENDING-待审核, REJECTED-已拒绝）", example = "ACTIVE")
            @RequestParam(required = false) String status,
            @Parameter(description = "搜索关键词（用户名、手机号）", example = "张三")
            @RequestParam(required = false) String keyword) {

        log.info("获取用户列表: page={}, size={}, userType={}, status={}, keyword={}",
                page, size, userType, status, keyword);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createTime").descending());
            Page<User> users = userService.getUserList(pageable, userType, status, keyword);

            // 转换为 AdminUserVO
            Page<AdminUserVO> adminUserVOPage = users.map(AdminUserVO::fromUser);

            return Result.success(adminUserVOPage);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return Result.error("获取用户列表失败，请稍后重试");
        }
    }

    /**
     * 获取代充师傅列表
     *
     * @param page   页码
     * @param size   每页数量
     * @param status 状态筛选（PENDING-待审核, ACTIVE-已激活, REJECTED-已拒绝, SUSPENDED-已停用）
     * @return 代充师傅列表（返回AdminUserVO，包含身份证号用于审核）
     */
    @Operation(
        summary = "获取代充师傅列表",
        description = "管理员查询所有代充师傅，支持按状态筛选（待审核、已激活、已拒绝、已停用）",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要管理员权限"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/valet-chargers")
    public Result<Page<AdminUserVO>> getValetChargerList(
            @Parameter(description = "页码（从0开始）", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "状态筛选", example = "PENDING",
                schema = @Schema(allowableValues = {"PENDING", "ACTIVE", "REJECTED", "SUSPENDED"}))
            @RequestParam(required = false) String status,
            @Parameter(description = "关键词搜索（用户名/昵称）")
            @RequestParam(required = false) String keyword) {

        log.info("获取代充师傅列表: page={}, size={}, status={}, keyword={}", page, size, status, keyword);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createTime").descending());
            Page<User> chargers = userService.getUserList(pageable, "VALET_CHARGER", status, keyword);

            // 转换为 AdminUserVO
            Page<AdminUserVO> adminUserVOPage = chargers.map(AdminUserVO::fromUser);

            return Result.success(adminUserVOPage);
        } catch (Exception e) {
            log.error("获取代充师傅列表失败", e);
            return Result.error("获取代充师傅列表失败，请稍后重试");
        }
    }

    /**
     * 获取企业运营商列表
     *
     * @param page   页码
     * @param size   每页数量
     * @param status 状态筛选
     * @return 企业运营商列表（返回AdminUserVO）
     */
    @Operation(
        summary = "获取企业运营商列表",
        description = "管理员查询所有企业运营商，支持按状态筛选",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要管理员权限"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/operators")
    public Result<Page<AdminUserVO>> getOperatorList(
            @Parameter(description = "页码（从0开始）", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "状态筛选", example = "ACTIVE",
                schema = @Schema(allowableValues = {"PENDING", "ACTIVE", "REJECTED", "SUSPENDED"}))
            @RequestParam(required = false) String status,
            @Parameter(description = "关键词搜索（用户名/手机号/企业名称）")
            @RequestParam(required = false) String keyword) {

        log.info("获取企业运营商列表: page={}, size={}, status={}, keyword={}", page, size, status, keyword);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createTime").descending());
            Page<User> operators = userService.getUserList(pageable, "OPERATOR", status, keyword);

            // 转换为 AdminUserVO
            Page<AdminUserVO> adminUserVOPage = operators.map(AdminUserVO::fromUser);

            return Result.success(adminUserVOPage);
        } catch (Exception e) {
            log.error("获取企业运营商列表失败", e);
            return Result.error("获取企业运营商列表失败，请稍后重试");
        }
    }

    /**
     * 审核用户（代充师傅、企业运营商）
     *
     * @param userId 用户ID
     * @param params 审核参数 {approved: true/false, reason: "拒绝原因"}
     * @return 审核结果
     */
    @Operation(
        summary = "审核用户",
        description = "管理员审核代充师傅或企业运营商的注册申请，可通过或拒绝并提供原因",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "审核成功"),
        @ApiResponse(responseCode = "400", description = "审核参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要管理员权限"),
        @ApiResponse(responseCode = "404", description = "用户不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/{userId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> reviewUser(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long userId,
            @Parameter(description = "审核参数：{approved: true/false, reason: '拒绝原因'}", required = true)
            @RequestBody Map<String, Object> params) {

        Boolean approved = (Boolean) params.get("approved");
        String reason = (String) params.get("reason");

        if (approved == null) {
            return Result.error("审核结果不能为空（approved 字段必填）");
        }

        log.info("审核用户: userId={}, approved={}, reason={}", userId, approved, reason);

        try {
            userService.reviewUser(userId, approved, reason);
            return Result.success(approved ? "审核通过" : "审核拒绝", null);
        } catch (IllegalArgumentException e) {
            log.warn("审核用户参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("审核用户失败", e);
            return Result.error("审核用户失败，请稍后重试");
        }
    }

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param params 状态参数 {status: "ACTIVE/SUSPENDED"}
     * @return 更新结果
     */
    @Operation(
        summary = "更新用户状态",
        description = "管理员更新用户状态（激活或停用），用于封禁/解封用户",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "状态参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要管理员权限"),
        @ApiResponse(responseCode = "404", description = "用户不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PutMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateUserStatus(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long userId,
            @Parameter(description = "状态参数：{status: 'ACTIVE' 或 'SUSPENDED'}", required = true)
            @RequestBody Map<String, String> params) {

        String status = params.get("status");

        log.info("更新用户状态: userId={}, status={}", userId, status);

        try {
            userService.updateUserStatus(userId, status);
            return Result.success("状态更新成功", null);
        } catch (IllegalArgumentException e) {
            log.warn("更新用户状态参数错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新用户状态失败", e);
            return Result.error("更新用户状态失败，请稍后重试");
        }
    }

    /**
     * 获取用户详情
     *
     * @param userId 用户ID
     * @return 用户详情（返回AdminUserVO，包含身份证号用于审核）
     */
    @Operation(
        summary = "获取用户详情",
        description = "管理员查看指定用户的详细信息",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要管理员权限"),
        @ApiResponse(responseCode = "404", description = "用户不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/{userId}")
    public Result<AdminUserVO> getUserDetail(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long userId) {
        log.info("获取用户详情: userId={}", userId);

        try {
            User user = userService.getUserById(userId);
            return Result.success(AdminUserVO.fromUser(user));
        } catch (Exception e) {
            log.error("获取用户详情失败", e);
            return Result.error("获取用户详情失败，请稍后重试");
        }
    }

    /**
     * 获取用户统计信息
     *
     * @return 统计信息
     */
    @Operation(
        summary = "获取用户统计信息",
        description = "管理员查看用户总体统计数据，包括各类型用户数量、各状态用户数量等",
        security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或token失效"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要管理员权限"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getUserStatistics() {
        log.info("获取用户统计信息");

        try {
            Map<String, Object> statistics = userService.getUserStatistics();
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取用户统计信息失败", e);
            return Result.error("获取用户统计信息失败，请稍后重试");
        }
    }
}
