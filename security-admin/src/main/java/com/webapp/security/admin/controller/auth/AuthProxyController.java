package com.webapp.security.admin.controller.auth;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * OAuth2认证代理控制器
 * 使用标准Spring Security OAuth2客户端机制
 */
@RestController
@RequestMapping("/api/oauth2")
public class AuthProxyController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    /**
     * 代理获取OAuth2令牌
     * 
     * @param tokenRequest 包含授权码和回调URL的请求
     * @return OAuth2令牌响应
     */
    @PostMapping("/token")
    public ResponseEntity<?> exchangeToken(@RequestBody TokenRequest tokenRequest) {
        try {
            // 获取客户端注册信息
            ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(tokenRequest.getClientId());

            if (clientRegistration == null) {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "找不到客户端注册信息"));
            }

            // 设置HTTP请求头，包括Basic认证
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(
                    clientRegistration.getClientId(),
                    clientRegistration.getClientSecret());

            // 设置请求参数
            MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
            formParams.add("grant_type", "authorization_code");
            formParams.add("code", tokenRequest.getCode());
            formParams.add("state", tokenRequest.getState());
            formParams.add("redirect_uri", tokenRequest.getRedirectUri());

            // 发送请求到OAuth2服务器的token端点
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formParams, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    clientRegistration.getProviderDetails().getTokenUri(),
                    HttpMethod.POST,
                    requestEntity,
                    Map.class);

            // 返回结果给前端
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "获取token失败",
                            "message", e.getMessage()));
        }
    }
}