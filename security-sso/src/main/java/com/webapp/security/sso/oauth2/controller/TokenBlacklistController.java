package com.webapp.security.sso.oauth2.controller;

import com.webapp.security.core.model.ResponseResult;
import com.webapp.security.sso.oauth2.service.TokenBlacklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 令牌黑名单控制器
 * 提供令牌黑名单管理API
 */
@Slf4j
@RestController
@RequestMapping("/api/token-blacklist")
public class TokenBlacklistController {

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    /**
     * 将令牌添加到黑名单
     * 
     * @param request 包含JWT ID和过期时间的请求
     * @return 操作结果
     */
    @PostMapping("/add")
    public ResponseResult<?> addToBlacklist(@RequestBody BlacklistRequest request) {
        try {
            tokenBlacklistService.blacklistToken(request.getJti(), request.getExpirationTime());
            return ResponseResult.success("令牌已加入黑名单");
        } catch (Exception e) {
            log.error("添加令牌到黑名单失败", e);
            return ResponseResult.failed("添加令牌到黑名单失败: " + e.getMessage());
        }
    }

    /**
     * 检查令牌是否在黑名单中
     * 
     * @param jti JWT ID
     * @return 检查结果
     */
    @GetMapping("/check/{jti}")
    public ResponseResult<?> checkBlacklist(@PathVariable String jti) {
        try {
            boolean isBlacklisted = tokenBlacklistService.isBlacklisted(jti);
            Map<String, Object> result = new HashMap<>();
            result.put("jti", jti);
            result.put("isBlacklisted", isBlacklisted);
            return ResponseResult.success(result);
        } catch (Exception e) {
            log.error("检查令牌黑名单状态失败", e);
            return ResponseResult.failed("检查令牌黑名单状态失败: " + e.getMessage());
        }
    }

    /**
     * 从黑名单中移除令牌
     * 
     * @param jti JWT ID
     * @return 操作结果
     */
    @DeleteMapping("/remove/{jti}")
    public ResponseResult<?> removeFromBlacklist(@PathVariable String jti) {
        try {
            tokenBlacklistService.removeFromBlacklist(jti);
            return ResponseResult.success("令牌已从黑名单移除");
        } catch (Exception e) {
            log.error("从黑名单移除令牌失败", e);
            return ResponseResult.failed("从黑名单移除令牌失败: " + e.getMessage());
        }
    }

    /**
     * 获取黑名单统计信息
     * 
     * @return 黑名单统计信息
     */
    @GetMapping("/stats")
    public ResponseResult<?> getBlacklistStats() {
        try {
            long blacklistSize = tokenBlacklistService.getBlacklistSize();
            Map<String, Object> stats = new HashMap<>();
            stats.put("blacklistSize", blacklistSize);
            stats.put("timestamp", System.currentTimeMillis());
            return ResponseResult.success(stats);
        } catch (Exception e) {
            log.error("获取黑名单统计信息失败", e);
            return ResponseResult.failed("获取黑名单统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 清理过期的黑名单条目
     * 
     * @return 操作结果
     */
    @PostMapping("/cleanup")
    public ResponseResult<?> cleanupExpiredEntries() {
        try {
            tokenBlacklistService.cleanupExpiredEntries();
            return ResponseResult.success("黑名单清理完成");
        } catch (Exception e) {
            log.error("清理黑名单失败", e);
            return ResponseResult.failed("清理黑名单失败: " + e.getMessage());
        }
    }

    /**
     * 黑名单请求对象
     */
    public static class BlacklistRequest {
        private String jti;
        private long expirationTime;

        public String getJti() {
            return jti;
        }

        public void setJti(String jti) {
            this.jti = jti;
        }

        public long getExpirationTime() {
            return expirationTime;
        }

        public void setExpirationTime(long expirationTime) {
            this.expirationTime = expirationTime;
        }
    }
}