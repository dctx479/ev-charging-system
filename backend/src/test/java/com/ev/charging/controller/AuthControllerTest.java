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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户认证控制器单元测试
 *
 * 测试范围：
 * - 用户登录（成功/失败）
 * - 用户注册（成功/重复用户名）
 * - 用户登出（token加入黑名单）
 * - 获取当前用户信息
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("用户认证控制器测试")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private LoginAttemptService loginAttemptService;

    private LoginDTO loginDTO;
    private RegisterDTO registerDTO;
    private User testUser;
    private String testToken;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        loginDTO = new LoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("password123");

        registerDTO = new RegisterDTO();
        registerDTO.setUsername("newuser");
        registerDTO.setPassword("password123");
        registerDTO.setEmail("newuser@example.com");
        registerDTO.setPhone("13800138000");

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("$2a$10$encrypted_password")
                .nickname("测试用户")
                .email("test@example.com")
                .phone("13800138000")
                .userType("USER")
                .balance(new BigDecimal("1000.00"))
                .carbonCredits(0)
                .status("ACTIVE")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
    }

    // ==================== 登录测试 ====================

    @Test
    @DisplayName("登录成功 - 返回token和用户信息")
    void testLoginSuccess() throws Exception {
        // Arrange
        Map<String, Object> loginResult = new HashMap<>();
        loginResult.put("token", testToken);
        loginResult.put("user", UserVO.fromUser(testUser));
        loginResult.put("expiresIn", 86400);

        when(loginAttemptService.isBlocked(anyString(), anyString())).thenReturn(false);
        when(userService.login("testuser", "password123")).thenReturn(loginResult);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.token").value(testToken))
                .andExpect(jsonPath("$.data.user.id").value(1))
                .andExpect(jsonPath("$.data.user.username").value("testuser"));

        // Verify
        verify(userService, times(1)).login("testuser", "password123");
        verify(loginAttemptService, times(1)).loginSucceeded("testuser", any());
    }

    @Test
    @DisplayName("登录失败 - 用户名或密码错误")
    void testLoginFailureWrongPassword() throws Exception {
        // Arrange
        when(loginAttemptService.isBlocked(anyString(), anyString())).thenReturn(false);
        when(userService.login("testuser", "wrongpassword"))
                .thenThrow(new IllegalArgumentException("用户名或密码错误"));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginDTO() {{
                    setUsername("testuser");
                    setPassword("wrongpassword");
                }})))
                .andExpect(status().is4xxClientError());

        // Verify
        verify(loginAttemptService, times(1)).loginFailed("testuser", any());
    }

    @Test
    @DisplayName("登录失败 - 账户被锁定（暴力破解防护）")
    void testLoginFailureAccountLocked() throws Exception {
        // Arrange
        when(loginAttemptService.isBlocked("testuser", "127.0.0.1")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO))
                .header("X-Real-IP", "127.0.0.1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(429))
                .andExpect(jsonPath("$.message").containsString("登录尝试过于频繁"));

        // Verify
        verify(userService, never()).login(anyString(), anyString());
    }

    @Test
    @DisplayName("登录失败 - 缺少必填字段")
    void testLoginFailureValidationError() throws Exception {
        // Arrange
        LoginDTO invalidDTO = new LoginDTO();
        invalidDTO.setUsername(""); // 空用户名

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).login(anyString(), anyString());
    }

    // ==================== 注册测试 ====================

    @Test
    @DisplayName("注册成功 - 返回用户信息")
    void testRegisterSuccess() throws Exception {
        // Arrange
        User newUser = User.builder()
                .id(2L)
                .username("newuser")
                .nickname("新用户")
                .email("newuser@example.com")
                .phone("13800138000")
                .userType("USER")
                .status("ACTIVE")
                .balance(BigDecimal.ZERO)
                .carbonCredits(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        when(userService.register(any(RegisterDTO.class))).thenReturn(newUser);

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("注册成功"))
                .andExpect(jsonPath("$.data.username").value("newuser"))
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"));

        verify(userService, times(1)).register(any(RegisterDTO.class));
    }

    @Test
    @DisplayName("注册失败 - 用户名已存在")
    void testRegisterFailureDuplicateUsername() throws Exception {
        // Arrange
        when(userService.register(any(RegisterDTO.class)))
                .thenThrow(new IllegalArgumentException("用户名已存在"));

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().is4xxClientError());

        verify(userService, times(1)).register(any(RegisterDTO.class));
    }

    @Test
    @DisplayName("注册失败 - 邮箱格式无效")
    void testRegisterFailureInvalidEmail() throws Exception {
        // Arrange
        RegisterDTO invalidDTO = new RegisterDTO();
        invalidDTO.setUsername("newuser");
        invalidDTO.setPassword("password123");
        invalidDTO.setEmail("invalid-email"); // 非法邮箱

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any(RegisterDTO.class));
    }

    @Test
    @DisplayName("注册失败 - 电话号码格式无效")
    void testRegisterFailureInvalidPhone() throws Exception {
        // Arrange
        RegisterDTO invalidDTO = new RegisterDTO();
        invalidDTO.setUsername("newuser");
        invalidDTO.setPassword("password123");
        invalidDTO.setEmail("newuser@example.com");
        invalidDTO.setPhone("123456"); // 非法电话

        // Act & Assert
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any(RegisterDTO.class));
    }

    // ==================== 登出测试 ====================

    @Test
    @DisplayName("登出成功 - token加入黑名单")
    void testLogoutSuccess() throws Exception {
        // Arrange
        when(jwtUtil.extractToken(any())).thenReturn(testToken);
        when(jwtUtil.getExpireSeconds(testToken)).thenReturn(3600L);

        // Act & Assert
        mockMvc.perform(post("/auth/logout")
                .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("退出成功"));

        // Verify
        verify(tokenBlacklistService, times(1)).addToBlacklist(testToken, 3600L);
    }

    @Test
    @DisplayName("登出成功 - 无token的情况")
    void testLogoutSuccessNoToken() throws Exception {
        // Arrange
        when(jwtUtil.extractToken(any())).thenReturn(null);

        // Act & Assert
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("退出成功"));

        // Verify
        verify(tokenBlacklistService, never()).addToBlacklist(anyString(), anyLong());
    }

    // ==================== 获取用户信息测试 ====================

    @Test
    @DisplayName("获取当前用户信息成功")
    void testGetUserInfoSuccess() throws Exception {
        // Arrange
        when(userService.getUserById(1L)).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(get("/auth/user/info")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("测试用户"));

        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    @DisplayName("获取用户信息失败 - 用户不存在")
    void testGetUserInfoNotFound() throws Exception {
        // Arrange
        when(userService.getUserById(999L))
                .thenThrow(new IllegalArgumentException("用户不存在"));

        // Act & Assert
        mockMvc.perform(get("/auth/user/info")
                .requestAttr("userId", 999L))
                .andExpect(status().is4xxClientError());

        verify(userService, times(1)).getUserById(999L);
    }
}
