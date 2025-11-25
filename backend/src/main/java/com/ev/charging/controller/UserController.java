package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.entity.User;
import com.ev.charging.repository.UserRepository;
import com.ev.charging.service.UserService;
import com.ev.charging.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 用户管理控制器
 */
@Tag(name = "用户管理", description = "用户信息查询、更新、余额管理、充值提现等功能")
@RestController
@RequestMapping(value = "/users", produces = "application/json;charset=UTF-8")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    /**
     * 获取当前用户信息
     * 通过JWT token获取userId
     * 返回脱敏的用户信息
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
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        User user = userService.getUserById(userId);
        return Result.success(UserVO.fromUser(user));
    }

    /**
     * 更新当前用户信息
     * 返回脱敏的用户信息
     */
    @Operation(
            summary = "更新当前用户信息",
            description = "更新当前登录用户的个人信息（昵称、头像、车辆信息等）",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PutMapping("/me")
    public Result<UserVO> updateCurrentUser(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Parameter(description = "用户信息", required = true)
            @RequestBody User user) {
        user.setId(userId);
        User updatedUser = userService.updateUser(user);
        return Result.success("更新成功", UserVO.fromUser(updatedUser));
    }

    /**
     * 获取账户余额
     */
    @Operation(
            summary = "获取账户余额",
            description = "查询当前用户的账户余额",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping("/balance")
    public Result<BigDecimal> getBalance(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        User user = userService.getUserById(userId);
        return Result.success(user.getBalance());
    }

    /**
     * 充值
     */
    @Operation(
            summary = "账户充值",
            description = "用户账户充值（简化处理，实际应调用支付接口）",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "充值成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping("/recharge")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> recharge(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Parameter(description = "充值参数：amount-充值金额，paymentMethod-支付方式", required = true)
            @RequestBody Map<String, Object> params) {
        // 参数校验
        if (params.get("amount") == null) {
            return Result.error("充值金额不能为空");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(params.get("amount").toString());
        } catch (NumberFormatException e) {
            return Result.error("充值金额格式不正确");
        }

        // 金额校验
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("充值金额必须大于0");
        }

        // 使用原子操作增加余额，防止并发竞态条件
        int updated = userRepository.addBalanceAtomically(userId, amount);
        if (updated == 0) {
            return Result.error("充值失败，用户不存在");
        }

        return Result.success("充值成功", null);
    }

    /**
     * 提现
     */
    @Operation(
            summary = "账户提现",
            description = "用户申请提现到银行账户",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "提现申请成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "400", description = "余额不足或参数错误")
    })
    @PostMapping("/withdraw")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> withdraw(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Parameter(description = "提现参数：amount-提现金额，bankAccount-银行账号", required = true)
            @RequestBody Map<String, Object> params) {
        // 参数校验
        if (params.get("amount") == null) {
            return Result.error("提现金额不能为空");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(params.get("amount").toString());
        } catch (NumberFormatException e) {
            return Result.error("提现金额格式不正确");
        }

        // 金额校验
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("提现金额必须大于0");
        }

        // 使用原子操作扣减余额，同时检查余额是否充足，防止并发竞态条件
        int updated = userRepository.deductBalanceAtomically(userId, amount);
        if (updated == 0) {
            return Result.error("余额不足或用户不存在");
        }

        return Result.success("提现申请已提交", null);
    }

    /**
     * 获取交易记录
     * TODO: 实现完整的交易记录查询
     */
    @Operation(
            summary = "获取交易记录",
            description = "分页查询用户的交易记录（待实现）",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping("/transactions")
    public Result<Map<String, Object>> getTransactions(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "交易类型：recharge-充值，withdraw-提现，charge-充电", example = "charge")
            @RequestParam(required = false) String type) {

        // 这里返回空记录，实际应该查询transaction表
        Map<String, Object> result = Map.of(
            "records", java.util.Collections.emptyList(),
            "total", 0,
            "page", page,
            "size", size
        );

        return Result.success(result);
    }

    /**
     * 修改密码
     */
    @Operation(
            summary = "修改密码",
            description = "用户修改登录密码",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "密码修改成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "400", description = "旧密码错误")
    })
    @PutMapping("/password")
    public Result<Void> changePassword(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Parameter(description = "密码修改参数：oldPassword-旧密码，newPassword-新密码", required = true)
            @RequestBody Map<String, String> params) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        try {
            userService.changePassword(userId, oldPassword, newPassword);
            return Result.success("密码修改成功", null);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("密码修改失败", e);
            return Result.error("密码修改失败，请稍后重试");
        }
    }

    /**
     * 上传头像
     */
    @Operation(
            summary = "上传头像",
            description = "用户上传个人头像（待实现）",
            security = @SecurityRequirement(name = "bearer-jwt")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "上传成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "400", description = "文件格式错误")
    })
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Parameter(description = "头像文件", required = true)
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {

        // TODO: 实现文件上传逻辑
        return Result.success("上传成功", "avatar_url");
    }
}
