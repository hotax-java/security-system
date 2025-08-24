package com.webapp.security.sso.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    /**
     * 登录页面
     * 支持接收所有URL参数并传递到前端页面
     *
     * @param error   错误参数
     * @param logout  登出参数
     * @param request HTTP请求对象，用于获取所有参数
     * @param model   模型
     * @return 登录页面模板
     */
    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            HttpServletRequest request,
            Model model) {

        // 获取所有URL参数
        Map<String, String[]> parameterMap = request.getParameterMap();
        logger.info("访问登录页面，接收到的所有参数: {}", parameterMap.keySet());

        // 将所有参数添加到模型中，供前端页面使用
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();
            if (values != null && values.length > 0) {
                // 如果有多个值，取第一个；通常OAuth2参数都是单值的
                String value = values[0];
                model.addAttribute(key, value);
                logger.debug("添加参数到模型: {}={}", key, value);
            }
        }

        // 特殊处理error和logout参数（保持原有逻辑）
        if (error != null) {
            model.addAttribute("hasError", true);
            logger.warn("登录失败，显示错误信息");
        }

        if (logout != null) {
            model.addAttribute("hasLogout", true);
            logger.info("用户已登出");
        }

        return "login";
    }
}
