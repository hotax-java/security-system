package com.webapp.security.sso.third.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
// 移除不再需要的数据库相关导入
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * GitHub用户服务
 * 处理GitHub OAuth2认证和用户信息获取
 */
@Service
@RequiredArgsConstructor
public class GitHubUserService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubUserService.class);

    private final GitHubOAuth2Config githubConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 获取GitHub授权URL
     */
    public String getAuthorizeUrl(String state) {
        return UriComponentsBuilder.fromHttpUrl(githubConfig.getAuthorizeUrl())
                .queryParam("client_id", githubConfig.getAppId())
                .queryParam("redirect_uri", githubConfig.getRedirectUri())
                .queryParam("scope", githubConfig.getScope())
                .queryParam("state", state)
                .build().toUriString();
    }

    /**
     * 使用授权码交换访问令牌
     */
    public String getAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", githubConfig.getAppId());
        body.add("client_secret", githubConfig.getAppSecret());
        body.add("code", code);
        body.add("redirect_uri", githubConfig.getRedirectUri());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    githubConfig.getTokenUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                GitHubAccessToken token = objectMapper.readValue(response.getBody(), GitHubAccessToken.class);
                if (token != null && token.getAccessToken() != null) {
                    return token.getAccessToken();
                } else {
                    logger.error("获取GitHub访问令牌失败: {}", response.getBody());
                }
            }
        } catch (RestClientException | JsonProcessingException e) {
            logger.error("调用GitHub访问令牌接口异常", e);
        }

        return null;
    }

    /**
     * 获取GitHub用户信息
     */
    public GitHubUserInfo getUserInfoByToken(String accessToken) {
        if (accessToken == null) {
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    githubConfig.getUserInfoUrl(),
                    HttpMethod.GET,
                    entity,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                GitHubUserInfo userInfo = objectMapper.readValue(response.getBody(), GitHubUserInfo.class);

                // 如果用户没有公开邮箱，尝试获取邮箱列表
                if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
                    String email = getPrimaryEmail(accessToken);
                    if (email != null) {
                        userInfo.setEmail(email);
                    }
                }

                return userInfo;
            }
        } catch (RestClientException | JsonProcessingException e) {
            logger.error("调用GitHub用户信息接口异常", e);
        }

        return null;
    }

    /**
     * 获取主要邮箱
     */
    private String getPrimaryEmail(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    githubConfig.getUserEmailsUrl(),
                    HttpMethod.GET,
                    entity,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                List<Map<String, Object>> emails = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {
                        });

                for (Map<String, Object> email : emails) {
                    Boolean primary = (Boolean) email.get("primary");
                    Boolean verified = (Boolean) email.get("verified");
                    if (primary != null && primary && verified != null && verified) {
                        return (String) email.get("email");
                    }
                }
            }
        } catch (RestClientException | JsonProcessingException e) {
            logger.error("调用GitHub用户邮箱接口异常", e);
        }

        return null;
    }

    /**
     * 获取用户信息（一次性完成code到用户信息的转换）
     */
    public GitHubUserInfo getUserInfo(String code) {
        String accessToken = getAccessToken(code);
        return getUserInfoByToken(accessToken);
    }



    /**
     * GitHub访问令牌响应
     */
    @Data
    public static class GitHubAccessToken {
        private String accessToken;
        private String tokenType;
        private String scope;

        // Jackson映射字段名
        public void setAccess_token(String accessToken) {
            this.accessToken = accessToken;
        }

        public void setToken_type(String tokenType) {
            this.tokenType = tokenType;
        }
    }

    /**
     * GitHub用户信息响应
     */
    @Data
    public static class GitHubUserInfo {
        private Long id;
        private String login;
        private String name;
        private String email;
        private String avatarUrl;
        private String bio;
        private String location;
        private String company;

        // Jackson映射字段名
        public void setAvatar_url(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }
    }
}