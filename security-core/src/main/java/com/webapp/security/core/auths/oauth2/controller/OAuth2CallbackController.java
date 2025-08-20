package com.webapp.security.core.auths.oauth2.controller;

import com.webapp.security.core.config.ClientIdConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.webapp.security.core.auths.oauth2.OAuth2ClientService;
import com.webapp.security.core.auths.oauth2.model.TokenRequest;
import com.webapp.security.core.auths.oauth2.model.TokenResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2回调控制器
 * 处理从SSO授权服务器返回的授权码，并将token重定向到前端
 */
@RestController
@RequestMapping("/api/oauth2")
public class OAuth2CallbackController {

    private static final Logger log = LoggerFactory.getLogger(OAuth2CallbackController.class);

    @Autowired
    private OAuth2ClientService oauth2ClientService;

    @Value("${webapp.frontend.url:http://localhost:8081}")
    private String frontendUrl;

    /**
     * 处理从SSO服务器返回的授权码或错误
     */
    @GetMapping("/callback")
    public void handleCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 记录所有请求参数
        Map<String, String> params = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            params.put(name, request.getParameter(name));
        }
        log.debug("收到SSO回调: {}", params);

        // 获取关键参数
        String code = request.getParameter("code");
        String error = request.getParameter("error");
        String errorDescription = request.getParameter("error_description");
        String state = request.getParameter("state");
        String clientId = request.getParameter("client_id");

        if (state == null) {
            log.error("SSO回调缺少state参数");
            response.sendRedirect(frontendUrl + "/oauth2/callback?error=missing_state&platform=admin");
            return;
        }

        // 检查是否有错误
        if (error != null) {
            String errorMessage = error;
            if (errorDescription != null) {
                errorMessage += ": " + errorDescription;
            }

            String errorUrl = frontendUrl + "/oauth2/callback?error=" +
                    URLEncoder.encode(errorMessage, StandardCharsets.UTF_8) +
                    "&platform=admin&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

            log.error("SSO授权失败: {}", errorMessage);
            response.sendRedirect(errorUrl);
            return;
        }

        // 检查是否有授权码
        if (code == null) {
            String errorUrl = frontendUrl + "/oauth2/callback?error=missing_code" +
                    "&platform=admin&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

            log.error("SSO回调缺少code参数");
            response.sendRedirect(errorUrl);
            return;
        }

        // 处理授权码
        try {
            // 准备令牌交换请求
            TokenRequest tokenRequest = new TokenRequest();
            tokenRequest.setCode(code);
            tokenRequest.setState(state);
            tokenRequest.setClientId(clientId != null ? clientId : "webapp-client");

            // 构建回调URL
            String callbackUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/oauth2/callback")
                    .build()
                    .toUriString();
            tokenRequest.setRedirectUri(callbackUrl);

            log.debug("准备交换token: code={}, redirectUri={}", code, callbackUrl);

            // 交换令牌
            TokenResponse tokenResponse = oauth2ClientService.exchangeToken(tokenRequest);

            if (tokenResponse != null) {
                // 构建前端回调URL
                String frontendCallback = frontendUrl + "/oauth2/callback";

                // 添加token和admin标识到URL
                String redirectUrl = frontendCallback
                        + "?access_token=" + URLEncoder.encode(tokenResponse.getAccess_token(), StandardCharsets.UTF_8)
                        + "&token_type=" + URLEncoder.encode(tokenResponse.getToken_type(), StandardCharsets.UTF_8)
                        + "&expires_in=" + tokenResponse.getExpires_in();

                if (tokenResponse.getRefresh_token() != null) {
                    redirectUrl += "&refresh_token="
                            + URLEncoder.encode(tokenResponse.getRefresh_token(), StandardCharsets.UTF_8);
                }

                redirectUrl += "&platform=admin"; // 添加admin平台标识

                log.debug("重定向到前端: {}", redirectUrl);
                response.sendRedirect(redirectUrl);
            } else {
                // 令牌交换失败
                String errorUrl = frontendUrl + "/oauth2/callback?error=token_exchange_failed"
                        + "&platform=admin"
                        + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

                log.error("令牌交换失败");
                response.sendRedirect(errorUrl);
            }

        } catch (Exception e) {
            log.error("处理回调时发生错误", e);

            String errorUrl = frontendUrl + "/oauth2/callback?error="
                    + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8)
                    + "&platform=admin"
                    + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

            response.sendRedirect(errorUrl);
        }
    }
}