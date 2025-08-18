package com.webapp.security.core.context;

import com.webapp.security.core.entity.SysUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * 用户上下文，用于获取当前登录用户信息
 */
public class UserContext {

    private static final String USER_ID_CLAIM = "user_id";
    private static final String USERNAME_CLAIM = "sub";
    private static final String REAL_NAME_CLAIM = "real_name";

    /**
     * 获取当前登录用户ID
     * 
     * @return 用户ID，如果未登录则返回null
     */
    public static Long getCurrentUserId() {
        Jwt jwt = getJwtToken();
        if (jwt != null && jwt.getClaims().containsKey(USER_ID_CLAIM)) {
            return Long.valueOf(jwt.getClaim(USER_ID_CLAIM).toString());
        }
        return null;
    }

    /**
     * 获取当前登录用户名
     * 
     * @return 用户名，如果未登录则返回null
     */
    public static String getCurrentUsername() {
        Jwt jwt = getJwtToken();
        if (jwt != null) {
            return jwt.getSubject();
        }
        return null;
    }

    /**
     * 获取当前登录用户真实姓名
     * 
     * @return 真实姓名，如果未登录或未设置则返回null
     */
    public static String getCurrentRealName() {
        Jwt jwt = getJwtToken();
        if (jwt != null && jwt.getClaims().containsKey(REAL_NAME_CLAIM)) {
            return jwt.getClaim(REAL_NAME_CLAIM).toString();
        }
        return null;
    }

    /**
     * 获取当前登录用户的JWT令牌
     * 
     * @return JWT令牌，如果未登录则返回null
     */
    public static Jwt getJwtToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            return ((JwtAuthenticationToken) authentication).getToken();
        }
        return null;
    }

    /**
     * 判断当前用户是否已登录
     * 
     * @return 是否已登录
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() &&
                !(authentication.getPrincipal().equals("anonymousUser"));
    }

    /**
     * 创建简化的用户对象，包含基本信息
     * 
     * @return 用户对象，如果未登录则返回null
     */
    public static SysUser getCurrentUser() {
        if (!isAuthenticated()) {
            return null;
        }

        Long userId = getCurrentUserId();
        String username = getCurrentUsername();
        String realName = getCurrentRealName();

        if (userId == null || username == null) {
            return null;
        }

        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUsername(username);
        user.setRealName(realName);
        return user;
    }
}