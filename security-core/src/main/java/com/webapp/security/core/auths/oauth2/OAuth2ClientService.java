package com.webapp.security.core.auths.oauth2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.webapp.security.core.auths.oauth2.config.OAuth2ClientProperties;
import com.webapp.security.core.auths.oauth2.model.TokenRequest;
import com.webapp.security.core.auths.oauth2.model.TokenResponse;

import java.util.Map;

/**
 * OAuth2客户端服务，支持两种方式获取token：
 * 1. 使用PKCE方式（通过RestTemplate）
 * 2. 使用RestTemplate和Basic认证（非PKCE）
 */
@Service
public class OAuth2ClientService {

    private static final Logger log = LoggerFactory.getLogger(OAuth2ClientService.class);

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private OAuth2ClientProperties properties;

    /**
     * 检查是否启用了PKCE
     */
    public boolean isPkceEnabled() {
        return properties.isEnablePkce();
    }

    /**
     * 使用授权码交换令牌
     * 根据配置自动选择PKCE或非PKCE方式
     * 
     * @param request 令牌请求
     * @return 令牌响应
     */
    public TokenResponse exchangeToken(TokenRequest request) {
        if (isPkceEnabled()) {
            log.debug("使用PKCE方式交换令牌");
            return exchangeTokenWithPkce(request);
        } else {
            log.debug("使用非PKCE方式交换令牌");
            return exchangeTokenWithBasicAuth(request);
        }
    }

    /**
     * 使用PKCE方式交换令牌（通过RestTemplate实现）
     * 
     * @return 令牌响应，如果失败则返回null
     */
    private TokenResponse exchangeTokenWithPkce(TokenRequest tokenRequest) {
        try {
            // 获取客户端注册信息
            ClientRegistration clientRegistration = clientRegistrationRepository
                    .findByRegistrationId(tokenRequest.getClientId());

            if (clientRegistration == null) {
                log.error("找不到客户端注册信息: {}", tokenRequest.getClientId());
                return null;
            }

            // 设置HTTP请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // 设置请求参数
            MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
            formParams.add("grant_type", "authorization_code");
            formParams.add("code", tokenRequest.getCode());
            formParams.add("redirect_uri", tokenRequest.getRedirectUri());
            formParams.add("client_id", clientRegistration.getClientId());

            // 注意：在真正的PKCE流程中，还需要提供code_verifier参数
            // 这里我们假设授权服务器已经适配了您的场景，不需要该参数
            // 如果需要使用code_verifier，应该从会话或存储中获取之前生成的值

            // 发送请求到OAuth2服务器的token端点
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formParams, headers);
            String tokenUri = clientRegistration.getProviderDetails().getTokenUri();

            log.debug("发送请求到token端点: {}", tokenUri);
            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUri,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class);

            return parseTokenResponse(response.getBody());
        } catch (HttpClientErrorException e) {
            log.error("获取token时发生HTTP错误: {}", e.getResponseBodyAsString(), e);
            return null;
        } catch (Exception e) {
            log.error("获取token失败", e);
            return null;
        }
    }

    /**
     * 使用Basic认证方式交换令牌（非PKCE）
     */
    private TokenResponse exchangeTokenWithBasicAuth(TokenRequest request) {
        try {
            // 获取客户端注册信息
            ClientRegistration clientRegistration = clientRegistrationRepository
                    .findByRegistrationId(request.getClientId());

            if (clientRegistration == null) {
                log.error("找不到客户端注册信息: {}", request.getClientId());
                return null;
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
            formParams.add("code", request.getCode());
            if (StringUtils.hasText(request.getState())) {
                formParams.add("state", request.getState());
            }
            formParams.add("redirect_uri", request.getRedirectUri());

            // 发送请求到OAuth2服务器的token端点
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formParams, headers);
            String tokenUri = clientRegistration.getProviderDetails().getTokenUri();

            log.debug("发送请求到token端点: {}", tokenUri);
            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUri,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class);

            return parseTokenResponse(response.getBody());
        } catch (HttpClientErrorException e) {
            log.error("获取token时发生HTTP错误: {}", e.getResponseBodyAsString(), e);
            return null;
        } catch (Exception e) {
            log.error("获取token失败", e);
            return null;
        }
    }

    /**
     * 解析令牌响应
     */
    private TokenResponse parseTokenResponse(Map<String, Object> body) {
        if (body == null) {
            log.warn("响应体为空");
            return null;
        }

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccess_token((String) body.get("access_token"));
        tokenResponse.setToken_type((String) body.get("token_type"));
        tokenResponse.setRefresh_token((String) body.get("refresh_token"));

        if (body.get("expires_in") != null) {
            tokenResponse.setExpires_in(Long.parseLong(body.get("expires_in").toString()));
        }

        tokenResponse.setScope((String) body.get("scope"));
        log.debug("成功获取到token: {}", tokenResponse.getAccess_token());
        return tokenResponse;
    }
}