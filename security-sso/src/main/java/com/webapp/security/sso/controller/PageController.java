package com.webapp.security.sso.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 页面控制器
 * 处理页面路由和模板渲染
 */
@Controller
public class PageController {

    private static final Logger logger = LoggerFactory.getLogger(PageController.class);

    /**
     * 登录页面
     *
     * @param error  错误参数
     * @param logout 登出参数
     * @param model  模型
     * @return 登录页面模板
     */
    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        logger.info("访问登录页面, error={}, logout={}", error, logout);

        if (error != null) {
            model.addAttribute("error", true);
            logger.warn("登录失败，显示错误信息");
        }

        if (logout != null) {
            model.addAttribute("logout", true);
            logger.info("用户已登出");
        }

        return "login";
    }

    /**
     * PKCE 测试页面
     *
     * @param model 模型
     * @return PKCE 测试页面模板
     */
    @GetMapping("/test/pkce")
    public String pkceTest(Model model) {
        logger.info("访问 PKCE 测试页面");

        // 添加客户端配置信息到模型
        model.addAttribute("clientId", "test-client");
        model.addAttribute("redirectUri", "http://localhost:8080/test/callback");
        model.addAttribute("scope", "read write");

        return "test/pkce";
    }

    /**
     * OAuth2 回调页面
     *
     * @param code             授权码
     * @param state            状态参数
     * @param error            错误参数
     * @param errorDescription 错误描述
     * @param model            模型
     * @return 回调页面模板
     */
    @GetMapping("/test/callback")
    public String callback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription,
            Model model) {

        logger.info("OAuth2 回调处理, code={}, state={}, error={}, errorDescription={}",
                code, state, error, errorDescription);

        // 将参数传递给前端页面处理
        model.addAttribute("code", code);
        model.addAttribute("state", state);
        model.addAttribute("error", error);
        model.addAttribute("errorDescription", errorDescription);

        return "test/callback";
    }

    /**
     * 首页根据认证状态智能重定向
     * 已认证用户重定向到测试页面，未认证用户重定向到登录页面
     *
     * @return 相应的重定向页面
     */
    //@GetMapping("/")
    //public String index() {
    //    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    //    boolean isAuthenticated = authentication != null &&
    //            authentication.isAuthenticated() &&
    //            !authentication.getName().equals("anonymousUser");
    //
    //    if (isAuthenticated) {
    //        logger.info("已认证用户访问首页，重定向到测试页面");
    //        return "redirect:/test";
    //    } else {
    //        logger.info("未认证用户访问首页，重定向到登录页面");
    //        return "redirect:/login";
    //    }
    //}

    /**
     * 测试页面首页
     *
     * @param model 模型
     * @return 测试首页模板
     */
    @GetMapping("/test")
    public String testIndex(Model model) {
        logger.info("访问测试首页");
        return "redirect:/test/pkce";
    }

    @GetMapping(value = "/test/oauth-pkce", produces = MediaType.TEXT_HTML_VALUE)
    public String testPage() {
        return "test/oauth-pkce";
    }
}