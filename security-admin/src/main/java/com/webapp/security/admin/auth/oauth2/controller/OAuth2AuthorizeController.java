package com.webapp.security.admin.auth.oauth2.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.webapp.security.admin.auth.oauth2.OAuth2ClientService;
import com.webapp.security.admin.auth.oauth2.model.TokenRequest;
import com.webapp.security.admin.auth.oauth2.model.TokenResponse;

import java.util.Map;

/**
 * OAuth2令牌控制器
 * 提供OAuth2令牌交换端点
 */
@RestController
@RequestMapping("/api/oauth2")
public class OAuth2AuthorizeController {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthorizeController.class);

    @Autowired
    private OAuth2ClientService oauth2ClientService;

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

            // 如果有codeVerifier参数，记录PKCE流程
            if (request.getCodeVerifier() != null && !request.getCodeVerifier().isEmpty()) {
                log.debug("检测到PKCE流程，code_verifier: {}",
                        request.getCodeVerifier().substring(0, 5) + "...(已省略)");
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