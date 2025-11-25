package com.ev.charging.service;

import com.ev.charging.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Token黑名单服务测试
 * 
 * 测试场景:
 * 1. 添加token到黑名单
 * 2. 检查token是否在黑名单中
 * 3. 黑名单自动过期
 * 4. 强制用户下线
 * 5. 检查用户是否被强制下线
 */
@SpringBootTest
@DisplayName("Token黑名单服务测试")
class TokenBlacklistServiceTest {

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private String testToken;
    private Long testUserId = 1001L;

    @BeforeEach
    void setUp() {
        // 生成测试token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", testUserId);
        claims.put("username", "testuser");
        claims.put("userType", "USER");
        testToken = jwtUtil.generateToken(claims);

        // 清理测试数据
        cleanupTestData();
    }

    @Test
    @DisplayName("测试添加token到黑名单")
    void testAddToBlacklist() {
        // 执行
        tokenBlacklistService.addToBlacklist(testToken, 60);

        // 验证
        assertTrue(tokenBlacklistService.isBlacklisted(testToken), 
                   "Token应该在黑名单中");
    }

    @Test
    @DisplayName("测试检查token不在黑名单")
    void testTokenNotInBlacklist() {
        // 验证
        assertFalse(tokenBlacklistService.isBlacklisted(testToken), 
                    "Token不应该在黑名单中");
    }

    @Test
    @DisplayName("测试黑名单自动过期")
    void testBlacklistAutoExpire() throws InterruptedException {
        // 添加token到黑名单，2秒过期
        tokenBlacklistService.addToBlacklist(testToken, 2);

        // 立即检查：应该在黑名单中
        assertTrue(tokenBlacklistService.isBlacklisted(testToken), 
                   "Token应该在黑名单中");

        // 等待3秒
        TimeUnit.SECONDS.sleep(3);

        // 再次检查：应该已过期
        assertFalse(tokenBlacklistService.isBlacklisted(testToken), 
                    "Token应该已从黑名单中过期");
    }

    @Test
    @DisplayName("测试强制用户下线")
    void testBlacklistUserTokens() {
        // 执行：强制用户下线
        tokenBlacklistService.blacklistUserTokens(testUserId, 3600);

        // 验证：用户应该被标记为下线
        assertTrue(tokenBlacklistService.isUserBlacklisted(testUserId, testToken), 
                   "用户应该被强制下线");
    }

    @Test
    @DisplayName("测试用户未被强制下线")
    void testUserNotBlacklisted() {
        // 验证
        assertFalse(tokenBlacklistService.isUserBlacklisted(testUserId, testToken), 
                    "用户不应该被强制下线");
    }

    @Test
    @DisplayName("测试强制下线后新token仍有效")
    void testNewTokenAfterBlacklist() throws InterruptedException {
        // 强制用户下线
        tokenBlacklistService.blacklistUserTokens(testUserId, 3600);

        // 等待1秒确保时间差
        TimeUnit.SECONDS.sleep(1);

        // 生成新token（在强制下线之后）
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", testUserId);
        claims.put("username", "testuser");
        claims.put("userType", "USER");
        String newToken = jwtUtil.generateToken(claims);

        // 验证：新token应该不受影响
        assertFalse(tokenBlacklistService.isUserBlacklisted(testUserId, newToken), 
                    "强制下线后生成的新token应该有效");
    }

    @Test
    @DisplayName("测试从黑名单移除token")
    void testRemoveFromBlacklist() {
        // 添加到黑名单
        tokenBlacklistService.addToBlacklist(testToken, 60);
        assertTrue(tokenBlacklistService.isBlacklisted(testToken));

        // 从黑名单移除
        tokenBlacklistService.removeFromBlacklist(testToken);

        // 验证：应该不在黑名单中
        assertFalse(tokenBlacklistService.isBlacklisted(testToken), 
                    "Token应该已从黑名单中移除");
    }

    @Test
    @DisplayName("测试获取黑名单数量")
    void testGetBlacklistCount() {
        // 初始数量
        long initialCount = tokenBlacklistService.getBlacklistCount();

        // 添加两个token
        tokenBlacklistService.addToBlacklist(testToken, 60);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1002L);
        claims.put("username", "testuser2");
        claims.put("userType", "USER");
        String token2 = jwtUtil.generateToken(claims);
        tokenBlacklistService.addToBlacklist(token2, 60);

        // 验证：数量应该增加2
        long newCount = tokenBlacklistService.getBlacklistCount();
        assertEquals(initialCount + 2, newCount, 
                     "黑名单数量应该增加2");
    }

    /**
     * 清理测试数据
     */
    private void cleanupTestData() {
        // 清理token黑名单
        redisTemplate.delete("token:blacklist:" + testToken);
        
        // 清理用户黑名单
        redisTemplate.delete("user:blacklist:" + testUserId);
    }
}
