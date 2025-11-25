package com.ev.charging.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 登录尝试限制服务测试
 *
 * 测试场景:
 * 1. 记录登录失败次数（Redis）
 * 2. 超过阈值后锁定账户
 * 3. 登录成功后清除记录
 * 4. 分布式环境下状态一致性
 */
@SpringBootTest
@DisplayName("登录尝试限制服务测试")
class LoginAttemptServiceTest {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_IP = "192.168.1.100";
    private static final int MAX_ATTEMPTS = 5;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        cleanupTestData();
    }

    @Test
    @DisplayName("测试单次登录失败记录")
    void testSingleLoginFailure() {
        // 执行
        loginAttemptService.loginFailed(TEST_USERNAME, TEST_IP);

        // 验证：应该没有被锁定
        assertFalse(loginAttemptService.isBlocked(TEST_USERNAME, TEST_IP),
                    "第一次失败不应该被锁定");

        // 验证：剩余尝试次数
        assertEquals(MAX_ATTEMPTS - 1, loginAttemptService.getRemainingAttempts(TEST_USERNAME),
                     "剩余尝试次数应该是4");
    }

    @Test
    @DisplayName("测试多次登录失败达到阈值被锁定")
    void testLoginAttemptsExceedThreshold() {
        // 执行：失败5次（达到阈值）
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            loginAttemptService.loginFailed(TEST_USERNAME, TEST_IP);
        }

        // 验证：应该被锁定
        assertTrue(loginAttemptService.isBlocked(TEST_USERNAME, TEST_IP),
                   "失败5次后应该被锁定");

        // 验证：剩余尝试次数为0
        assertEquals(0, loginAttemptService.getRemainingAttempts(TEST_USERNAME),
                     "剩余尝试次数应该是0");
    }

    @Test
    @DisplayName("测试用户名和IP分别计数")
    void testUserAndIpSeparateAttempts() {
        String username2 = "testuser2";
        String ip2 = "192.168.1.101";

        // 执行：username1失败5次，username2失败2次
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            loginAttemptService.loginFailed(TEST_USERNAME, TEST_IP);
        }

        for (int i = 0; i < 2; i++) {
            loginAttemptService.loginFailed(username2, ip2);
        }

        // 验证：username1被锁定，username2未被锁定
        assertTrue(loginAttemptService.isBlocked(TEST_USERNAME, TEST_IP),
                   "username1应该被锁定");
        assertFalse(loginAttemptService.isBlocked(username2, ip2),
                    "username2不应该被锁定");
    }

    @Test
    @DisplayName("测试登录成功清除失败记录")
    void testSuccessfulLoginClearAttempts() {
        // 执行：失败3次，然后成功
        for (int i = 0; i < 3; i++) {
            loginAttemptService.loginFailed(TEST_USERNAME, TEST_IP);
        }

        // 登录成功
        loginAttemptService.loginSucceeded(TEST_USERNAME, TEST_IP);

        // 验证：失败计数应该被清除
        assertEquals(MAX_ATTEMPTS, loginAttemptService.getRemainingAttempts(TEST_USERNAME),
                     "成功登录后，剩余尝试次数应该重置为5");
        assertFalse(loginAttemptService.isBlocked(TEST_USERNAME, TEST_IP),
                    "成功登录后，应该不被锁定");
    }

    @Test
    @DisplayName("测试IP被锁定阻止登录")
    void testIpBlockPreventsLogin() {
        // 执行：IP失败5次
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            loginAttemptService.loginFailed(TEST_USERNAME, TEST_IP);
        }

        // 验证：即使用户名不同，相同IP也应该被锁定
        assertTrue(loginAttemptService.isBlocked("otheruser", TEST_IP),
                   "相同IP的其他用户也应该被锁定");
    }

    @Test
    @DisplayName("测试Redis数据一致性（分布式）")
    void testRedisDataConsistency() {
        // 执行
        for (int i = 0; i < 3; i++) {
            loginAttemptService.loginFailed(TEST_USERNAME, TEST_IP);
        }

        // 直接从Redis读取，验证数据一致性
        String userKey = "login:attempt:user:" + TEST_USERNAME;
        String userAttemptsStr = redisTemplate.opsForValue().get(userKey);

        assertNotNull(userAttemptsStr, "Redis中应该存储失败次数");
        assertEquals("3", userAttemptsStr, "失败次数应该是3");

        String ipKey = "login:attempt:ip:" + TEST_IP;
        String ipAttemptsStr = redisTemplate.opsForValue().get(ipKey);

        assertNotNull(ipAttemptsStr, "Redis中应该存储IP失败次数");
        assertEquals("3", ipAttemptsStr, "IP失败次数应该是3");
    }

    @Test
    @DisplayName("测试原子递增（INCR命令）")
    void testAtomicIncrement() {
        // 执行：快速失败3次
        loginAttemptService.loginFailed(TEST_USERNAME, TEST_IP);
        loginAttemptService.loginFailed(TEST_USERNAME, TEST_IP);
        loginAttemptService.loginFailed(TEST_USERNAME, TEST_IP);

        // 验证：计数应该准确为3（不是因为并发问题而出错）
        assertEquals(MAX_ATTEMPTS - 3, loginAttemptService.getRemainingAttempts(TEST_USERNAME),
                     "INCR操作应该是原子的，计数应该准确");
    }

    @Test
    @DisplayName("测试TTL自动过期（15分钟）")
    void testRedisKeyTTL() {
        // 执行
        loginAttemptService.loginFailed(TEST_USERNAME, TEST_IP);

        // 验证：Redis Key应该有TTL
        String userKey = "login:attempt:user:" + TEST_USERNAME;
        Long ttl = redisTemplate.getExpire(userKey, TimeUnit.SECONDS);

        assertNotNull(ttl, "Redis Key应该有TTL");
        assertTrue(ttl > 0, "TTL应该大于0");
        assertTrue(ttl <= 15 * 60, "TTL应该小于等于15分钟（900秒）");
    }

    @Test
    @DisplayName("测试未被锁定时的剩余次数")
    void testRemainingAttemptsWhenNotBlocked() {
        // 未失败过
        assertEquals(MAX_ATTEMPTS, loginAttemptService.getRemainingAttempts(TEST_USERNAME),
                     "未失败时，剩余次数应该是5");

        // 失败1次
        loginAttemptService.loginFailed(TEST_USERNAME, TEST_IP);
        assertEquals(MAX_ATTEMPTS - 1, loginAttemptService.getRemainingAttempts(TEST_USERNAME),
                     "失败1次后，剩余次数应该是4");

        // 失败2次
        loginAttemptService.loginFailed(TEST_USERNAME, TEST_IP);
        assertEquals(MAX_ATTEMPTS - 2, loginAttemptService.getRemainingAttempts(TEST_USERNAME),
                     "失败2次后，剩余次数应该是3");
    }

    @Test
    @DisplayName("测试锁定后清空返回0")
    void testLockedStatusReturnsZeroAttempts() {
        // 执行：失败5次被锁定
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            loginAttemptService.loginFailed(TEST_USERNAME, TEST_IP);
        }

        // 验证：剩余次数为0
        assertEquals(0, loginAttemptService.getRemainingAttempts(TEST_USERNAME),
                     "被锁定后，剩余次数应该是0");
    }

    /**
     * 清理测试数据
     */
    private void cleanupTestData() {
        redisTemplate.delete("login:attempt:user:" + TEST_USERNAME);
        redisTemplate.delete("login:attempt:ip:" + TEST_IP);
        redisTemplate.delete("login:attempt:user:testuser2");
        redisTemplate.delete("login:attempt:ip:192.168.1.101");
        redisTemplate.delete("login:attempt:ip:192.168.1.100");
        redisTemplate.delete("login:attempt:user:otheruser");
    }
}
