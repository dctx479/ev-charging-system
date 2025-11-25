package com.ev.charging.service;

/**
 * Token黑名单服务接口
 * 
 * 功能：
 * 1. 管理JWT Token黑名单，实现真正的登出
 * 2. 支持强制用户下线（管理员功能）
 * 3. 自动过期管理（与Token过期时间一致）
 */
public interface TokenBlacklistService {

    /**
     * 将token加入黑名单
     * 
     * @param token JWT token字符串
     * @param expireSeconds 过期时间（秒），应与token剩余有效期一致
     */
    void addToBlacklist(String token, long expireSeconds);

    /**
     * 检查token是否在黑名单中
     * 
     * @param token JWT token字符串
     * @return true-在黑名单中（已失效），false-不在黑名单中
     */
    boolean isBlacklisted(String token);

    /**
     * 将用户的所有token加入黑名单（强制下线所有设备）
     * 
     * @param userId 用户ID
     * @param expireSeconds token最大有效期（秒）
     */
    void blacklistUserTokens(Long userId, long expireSeconds);

    /**
     * 检查用户是否被强制下线
     * 
     * @param userId 用户ID
     * @param currentToken 当前token（用于判断是否在强制下线之前生成）
     * @return true-用户已被强制下线，false-正常
     */
    boolean isUserBlacklisted(Long userId, String currentToken);

    /**
     * 从黑名单移除token（一般不需要，依赖Redis自动过期）
     * 
     * @param token JWT token字符串
     */
    void removeFromBlacklist(String token);

    /**
     * 获取黑名单中token数量（用于监控）
     * 
     * @return 黑名单中token数量
     */
    long getBlacklistCount();
}
