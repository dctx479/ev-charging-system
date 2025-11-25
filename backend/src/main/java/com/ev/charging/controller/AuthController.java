package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.dto.LoginDTO;
import com.ev.charging.entity.User;
import com.ev.charging.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return 登录结果（包含token和用户信息）
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO) {
        Map<String, Object> result = userService.login(loginDTO.getUsername(), loginDTO.getPassword());
        return Result.success("登录成功", result);
    }

    /**
     * 用户注册
     *
     * @param user 用户信息
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody User user) {
        User registeredUser = userService.register(user);
        return Result.success("注册成功", registeredUser);
    }

    /**
     * 获取当前用户信息
     * 注意：实际项目中应从JWT token中解析用户ID
     *
     * @param userId 用户ID（从请求头的token中获取）
     * @return 用户信息
     */
    @GetMapping("/user/info")
    public Result<User> getUserInfo(@RequestParam Long userId) {
        User user = userService.getUserById(userId);
        return Result.success(user);
    }

    /**
     * 退出登录
     * 注意：JWT是无状态的，实际项目中可以将token加入黑名单
     *
     * @return 退出结果
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("退出成功", null);
    }
}
