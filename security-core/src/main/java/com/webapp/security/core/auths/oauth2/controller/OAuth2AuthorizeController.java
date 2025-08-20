package com.webapp.security.core.auths.oauth2.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.webapp.security.core.auths.oauth2.OAuth2ClientService;
import com.webapp.security.core.auths.oauth2.model.TokenRequest;
import com.webapp.security.core.auths.oauth2.model.TokenResponse;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * OAuth2授权控制器
 * 提供OAuth2授权端点，将请求转发到SSO服务器
 */
@RestController
@RequestMapping("/api/oauth2")
public class OAuth2AuthorizeController {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthorizeController.class);

    @Autowired
    private OAuth2ClientService oauth2ClientService;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:https://885ro126ov70.vicp.fun}")
    private String ssoServerUrl;

    /**
     * 授权端点，重定向到SSO授权页面
     * 
     * @param request  授权请求参数
     * @param response HTTP响应对象
     * @throws IOException 重定向异常
     */
    @PostMapping("/authorize")
    public void authorize(@RequestBody TokenRequest request, HttpServletResponse response) throws IOException {
        try {
            log.debug("收到授权请求: {}", request);

            if (request.getClientId() == null || request.getClientId().trim().isEmpty()) {
                request.setClientId("webapp-client"); // 默认客户端ID
            }

            // 使用ServletUriComponentsBuilder构建Admin的回调地址
            String adminCallbackUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/oauth2/callback")
                    .build()
                    .toUriString();

            // 构建SSO授权URL
            String authorizeUrl = String.format(
                    "%s/oauth2/authorize?response_type=code&client_id=%s&redirect_uri=%s&state=%s&scope=read+write",
                    ssoServerUrl,
                    request.getClientId(),
                    URLEncoder.encode(adminCallbackUrl, StandardCharsets.UTF_8.toString()),
                    URLEncoder.encode(request.getState(), StandardCharsets.UTF_8.toString()));

            log.debug("重定向到SSO授权端点: {}", authorizeUrl);

            // 重定向到SSO授权端点
            response.sendRedirect(authorizeUrl);

        } catch (Exception e) {
            log.error("处理授权请求时发生错误", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.getWriter().write("{\"error\":\"授权请求失败\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    /**
     * 使用授权码获取令牌
     * 
     * @param request 令牌请求参数
     * @return 令牌响应
     */
    @PostMapping("/token")
    public ResponseEntity<?> exchangeToken(@RequestBody TokenRequest request) {
        try {
            log.debug("收到令牌交换请求: {}", request);

            if (request.getClientId() == null || request.getClientId().trim().isEmpty()) {
                request.setClientId("webapp-client"); // 默认客户端ID
            }

            TokenResponse tokenResponse = oauth2ClientService.exchangeToken(request);

            if (tokenResponse != null) {
                return ResponseEntity.ok(tokenResponse);
            } else {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "获取token失败"));
            }
        } catch (Exception e) {
            log.error("处理令牌请求时发生错误", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "获取token失败",
                            "message", e.getMessage()));
        }
    }
}