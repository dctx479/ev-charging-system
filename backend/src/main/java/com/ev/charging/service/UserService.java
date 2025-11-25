package com.ev.charging.service;

import cn.hutool.crypto.digest.BCrypt;
import com.ev.charging.common.ResultCode;
import com.ev.charging.entity.User;
import com.ev.charging.repository.UserRepository;
import com.ev.charging.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务层
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 包含token和用户信息的Map
     */
    public Map<String, Object> login(String username, String password) {
        // 查找用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(ResultCode.USER_NOT_EXIST.getMessage()));

        // 验证密码
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new IllegalArgumentException(ResultCode.PASSWORD_ERROR.getMessage());
        }

        // 检查账户状态
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("账户已被禁用");
        }

        // 生成Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("userType", user.getUserType());
        String token = jwtUtil.generateToken(claims);

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", buildUserInfo(user));

        return result;
    }

    /**
     * 用户注册
     *
     * @param user 用户信息
     * @return 注册后的用户
     */
    public User register(User user) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException(ResultCode.USER_ALREADY_EXIST.getMessage());
        }

        // 检查手机号是否已存在
        if (user.getPhone() != null && userRepository.existsByPhone(user.getPhone())) {
            throw new IllegalArgumentException("手机号已被注册");
        }

        // 密码加密
        user.setPassword(BCrypt.hashpw(user.getPassword()));

        // 设置默认值
        if (user.getUserType() == null) {
            user.setUserType("USER");
        }
        if (user.getStatus() == null) {
            user.setStatus("ACTIVE");
        }
        if (user.getBalance() == null) {
            user.setBalance(0.0);
        }

        return userRepository.save(user);
    }

    /**
     * 根据ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(ResultCode.USER_NOT_EXIST.getMessage()));
    }

    /**
     * 更新用户信息
     *
     * @param user 用户信息
     * @return 更新后的用户
     */
    public User updateUser(User user) {
        User existingUser = getUserById(user.getId());

        // 更新允许修改的字段
        if (user.getNickname() != null) {
            existingUser.setNickname(user.getNickname());
        }
        if (user.getPhone() != null) {
            existingUser.setPhone(user.getPhone());
        }
        if (user.getEmail() != null) {
            existingUser.setEmail(user.getEmail());
        }

        return userRepository.save(existingUser);
    }

    /**
     * 构建用户信息（不包含敏感信息）
     *
     * @param user 用户实体
     * @return 用户信息Map
     */
    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("phone", user.getPhone());
        userInfo.put("email", user.getEmail());
        userInfo.put("userType", user.getUserType());
        userInfo.put("balance", user.getBalance());
        userInfo.put("status", user.getStatus());
        return userInfo;
    }
}
