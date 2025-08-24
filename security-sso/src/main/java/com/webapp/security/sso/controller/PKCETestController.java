package com.webapp.security.sso.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;
import jakarta.servlet.http.HttpServletRequest;

/**
 * PKCE授权流程测试控制器
 * 仅用于测试和调试OAuth2 PKCE流程
 */
//@Controller
//@RequestMapping("/test/pkce")
public class PKCETestController {

    private static final Logger log = LoggerFactory.getLogger(PKCETestController.class);

    /**
     * 主测试页面
     */
    @GetMapping(value = "/test", produces = MediaType.TEXT_HTML_VALUE)
    public String testPage() {
        return "test/pcke/pkce-oauth2";
    }

    /**
     * 回调页面
     */
    @GetMapping(value = "/callback", produces = MediaType.TEXT_HTML_VALUE)
    public String callbackPage() {
        return "test/pcke/pkce-callback";
    }

    /**
     * 登录页面
     */
    @GetMapping(value = "/login")
    public String loginPage() {
        return "login2";
    }

    /**
     * 记录授权请求的参数
     */
    @GetMapping("/log-authorize")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> logAuthorizeRequest(HttpServletRequest request) {
        log.info("收到授权请求，参数如下:");
        log.info("response_type: {}", request.getParameter("response_type"));
        log.info("client_id: {}", request.getParameter("client_id"));
        log.info("redirect_uri: {}", request.getParameter("redirect_uri"));
        log.info("scope: {}", request.getParameter("scope"));
        log.info("code_challenge_method: {}", request.getParameter("code_challenge_method"));
        log.info("code_challenge: {}", request.getParameter("code_challenge"));
        log.info("state: {}", request.getParameter("state"));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "授权请求参数已记录");
        response.put("code_challenge", request.getParameter("code_challenge"));
        response.put("code_challenge_method", request.getParameter("code_challenge_method"));

        return ResponseEntity.ok(response);
    }

    /**
     * 记录令牌请求的参数
     */
    @PostMapping("/log-token")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> logTokenRequest(HttpServletRequest request) {
        log.info("收到令牌请求，参数如下:");
        log.info("grant_type: {}", request.getParameter("grant_type"));
        log.info("client_id: {}", request.getParameter("client_id"));
        log.info("code: {}", request.getParameter("code"));
        log.info("redirect_uri: {}", request.getParameter("redirect_uri"));
        log.info("code_verifier: {}", request.getParameter("code_verifier"));
        log.info("refresh_token: {}", request.getParameter("refresh_token"));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "令牌请求参数已记录");

        if ("authorization_code".equals(request.getParameter("grant_type"))) {
            response.put("code_verifier", request.getParameter("code_verifier"));
        } else if ("refresh_token".equals(request.getParameter("grant_type"))) {
            response.put("refresh_token", request.getParameter("refresh_token"));
        }

        return ResponseEntity.ok(response);
    }
}