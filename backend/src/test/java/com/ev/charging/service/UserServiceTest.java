package com.ev.charging.service;

import cn.hutool.crypto.digest.BCrypt;
import com.ev.charging.entity.User;
import com.ev.charging.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试
 * 重点测试密码修改功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务测试")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private String rawPassword = "password123";
    private String hashedPassword;

    @BeforeEach
    void setUp() {
        // 准备测试用户数据
        hashedPassword = BCrypt.hashpw(rawPassword);
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword(hashedPassword);
        testUser.setNickname("测试用户");
        testUser.setUserType("USER");
        testUser.setStatus("ACTIVE");
        testUser.setBalance(BigDecimal.valueOf(100.00));
    }

    @Test
    @DisplayName("修改密码 - 成功场景")
    void changePassword_Success() {
        // Given
        String newPassword = "newPassword456";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        assertDoesNotThrow(() -> userService.changePassword(1L, rawPassword, newPassword));

        // Then
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));

        // 验证密码已被加密且可以验证
        assertTrue(BCrypt.checkpw(newPassword, testUser.getPassword()));
        assertFalse(BCrypt.checkpw(rawPassword, testUser.getPassword()));
    }

    @Test
    @DisplayName("修改密码 - 当前密码为空")
    void changePassword_OldPasswordEmpty() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.changePassword(1L, "", "newPassword456")
        );

        assertEquals("当前密码不能为空", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("修改密码 - 当前密码为null")
    void changePassword_OldPasswordNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.changePassword(1L, null, "newPassword456")
        );

        assertEquals("当前密码不能为空", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("修改密码 - 新密码为空")
    void changePassword_NewPasswordEmpty() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.changePassword(1L, rawPassword, "")
        );

        assertEquals("新密码不能为空", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("修改密码 - 新密码为null")
    void changePassword_NewPasswordNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.changePassword(1L, rawPassword, null)
        );

        assertEquals("新密码不能为空", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("修改密码 - 新密码长度不足6位")
    void changePassword_NewPasswordTooShort() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.changePassword(1L, rawPassword, "12345")
        );

        assertEquals("新密码长度不能少于6位", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("修改密码 - 用户不存在")
    void changePassword_UserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.changePassword(999L, rawPassword, "newPassword456")
        );

        assertEquals("用户不存在", exception.getMessage());
        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("修改密码 - 当前密码错误")
    void changePassword_WrongOldPassword() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.changePassword(1L, "wrongPassword", "newPassword456")
        );

        assertEquals("当前密码错误", exception.getMessage());
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("修改密码 - 新密码与当前密码相同")
    void changePassword_SamePassword() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.changePassword(1L, rawPassword, rawPassword)
        );

        assertEquals("新密码不能与当前密码相同", exception.getMessage());
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("修改密码 - 验证密码加密")
    void changePassword_PasswordEncryption() {
        // Given
        String newPassword = "newSecurePassword789";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            // 验证保存的密码不是明文
            assertNotEquals(newPassword, savedUser.getPassword());
            // 验证保存的密码是BCrypt格式
            assertTrue(savedUser.getPassword().startsWith("$2a$"));
            return savedUser;
        });

        // When
        userService.changePassword(1L, rawPassword, newPassword);

        // Then
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("更新用户信息 - 包含车辆信息")
    void updateUser_WithVehicleInfo() {
        // Given
        User updateData = new User();
        updateData.setId(1L);
        updateData.setNickname("新昵称");
        updateData.setPhone("13800138000");
        updateData.setEmail("newemail@test.com");
        updateData.setCarModel("特斯拉 Model 3");
        updateData.setBatteryCapacity(BigDecimal.valueOf(75.0));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUser(updateData);

        // Then
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));

        assertEquals("新昵称", testUser.getNickname());
        assertEquals("13800138000", testUser.getPhone());
        assertEquals("newemail@test.com", testUser.getEmail());
        assertEquals("特斯拉 Model 3", testUser.getCarModel());
        assertEquals(BigDecimal.valueOf(75.0), testUser.getBatteryCapacity());
    }

    @Test
    @DisplayName("更新用户信息 - 部分字段更新")
    void updateUser_PartialUpdate() {
        // Given
        User updateData = new User();
        updateData.setId(1L);
        updateData.setNickname("新昵称");
        // 不设置其他字段

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updateUser(updateData);

        // Then
        assertEquals("新昵称", testUser.getNickname());
        // 其他字段应保持不变
        assertNull(testUser.getPhone());
        assertNull(testUser.getEmail());
    }

    @Test
    @DisplayName("获取用户信息 - 成功")
    void getUserById_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("获取用户信息 - 用户不存在")
    void getUserById_NotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.getUserById(999L)
        );

        assertEquals("用户不存在", exception.getMessage());
        verify(userRepository, times(1)).findById(999L);
    }
}
