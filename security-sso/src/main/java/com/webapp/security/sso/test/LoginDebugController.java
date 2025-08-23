package com.webapp.security.sso.test;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录调试控制器
 * 用于测试和调试登录过程
 */
@Controller
@RequestMapping("/test/debug")
public class LoginDebugController {

    private static final Logger log = LoggerFactory.getLogger(LoginDebugController.class);

    /**
     * 显示当前认证信息
     */
    @GetMapping("/auth")
    @ResponseBody
    public Map<String, Object> getAuthInfo(HttpServletRequest request) {
        Map<String, Object> info = new HashMap<>();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null &&
                auth.isAuthenticated() &&
                !(auth instanceof AnonymousAuthenticationToken);

        info.put("isAuthenticated", isAuthenticated);

        if (isAuthenticated) {
            info.put("username", auth.getName());
            info.put("authorities", auth.getAuthorities());
            info.put("details", auth.getDetails());
        }

        // 记录日志
        log.info("认证信息请求: 已认证={}", isAuthenticated);
        if (isAuthenticated) {
            log.info("用户: {}, 权限: {}", auth.getName(), auth.getAuthorities());
        }

        return info;
    }

    /**
     * 显示认证失败信息
     */
    @GetMapping("/login-failure")
    public String loginFailureInfo(Model model, HttpServletRequest request) {
        // 获取登录失败的会话属性
        Object exception = request.getSession().getAttribute("SPRING_SECURITY_LAST_EXCEPTION");
        model.addAttribute("exception", exception != null ? exception.toString() : "无异常信息");

        // 记录日志
        log.info("登录失败信息请求: {}", exception);

        return "test/debug-login";
    }
}