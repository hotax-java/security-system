package com.webapp.security.sso.auths.third;

import com.webapp.security.core.config.ClientIdConfig;
import com.webapp.security.core.entity.SysUser;
import com.webapp.security.core.service.SysUserService;
import com.webapp.security.sso.context.SpringContextHolder;
import com.webapp.security.sso.context.ClientContext;
import com.webapp.security.sso.auths.oauth2.service.OAuth2Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户登录绑定服务
 * 通用的第三方登录用户绑定服务
 */
@Service
public class UserLoginService {

    private static final Logger logger = LoggerFactory.getLogger(UserLoginService.class);

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private OAuth2Service oAuth2Service;

    @Autowired
    private ClientIdConfig clientIdConfig;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private OAuth2AuthorizationService authorizationService;

    /********** OAuth2 授权码生成方法 **********/
    
    /**
     * 生成OAuth2授权码并重定向
     * 
     * @param user 系统用户
     * @param frontendCallbackUrl 前端回调URL
     * @param state OAuth2 state参数
     * @return 重定向响应
     */
    public String generateAuthorizationCodeAndRedirect(SysUser user, String frontendCallbackUrl, String state) {
        try {
            // 获取webapp客户端配置
            RegisteredClient registeredClient = oAuth2Service.getRegisteredClient(clientIdConfig.getWebappClientId());
            
            // 创建用户认证对象
            UserDetailsService userDetailsService = SpringContextHolder.getBean(UserDetailsService.class);
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            
            // 创建OAuth2授权构建器
            OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization
                    .withRegisteredClient(registeredClient)
                    .principalName(authentication.getName())
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizedScopes(registeredClient.getScopes());
            
            // 生成OAuth2授权码
            OAuth2AuthorizationCode authorizationCode = oAuth2Service.generateAuthorizationCode(
                    authentication, registeredClient, authorizationBuilder);
            
            // 保存授权信息
            OAuth2Authorization authorization = authorizationBuilder.build();
            authorizationService.save(authorization);
            
            // 构建重定向URL
            return UriComponentsBuilder.fromUriString(frontendCallbackUrl)
                    .queryParam("code", authorizationCode.getTokenValue())
                    .queryParam("state", state)
                    .build()
                    .toUriString();
                    
        } catch (Exception e) {
            logger.error("生成OAuth2授权码失败", e);
            throw new RuntimeException("生成授权码失败: " + e.getMessage());
        }
    }
    
    /********** Token 生成相关方法 **********/
    
    /**
     * 生成用户令牌
     * 
     * @param user 系统用户
     * @return 令牌信息
     */
    public Map<String, Object> generateUserToken(SysUser user) {
        String clientId = ClientContext.getClientId();
        if (clientId == null || clientId.trim().isEmpty()) {
            clientId = clientIdConfig.getWebappClientId();
        }
        // 获取客户端
        RegisteredClient registeredClient = oAuth2Service.getRegisteredClient(clientId);
        String username = user.getUsername();

        UserDetailsService userDetailsService = SpringContextHolder.getBean(UserDetailsService.class);

        UserDetails userDetails = userDetailsService
                .loadUserByUsername(username);

        // 创建认证对象
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        // 创建授权构建器
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization
                .withRegisteredClient(registeredClient)
                .principalName(user.getUsername())
                .authorizationGrantType(new AuthorizationGrantType("password"))
                .authorizedScopes(registeredClient.getScopes());

        // 生成访问令牌
        OAuth2AccessToken accessToken = oAuth2Service.generateAccessToken(
                authentication,
                registeredClient,
                authorizationBuilder);

        // 生成刷新令牌
        OAuth2RefreshToken refreshToken = oAuth2Service.generateRefreshToken(
                authentication,
                registeredClient,
                authorizationBuilder);

        OAuth2Authorization authorization = authorizationBuilder.build();
        authorizationService.save(authorization);

        long expiresIn = 0;
        if (accessToken.getExpiresAt() != null) {
            expiresIn = Duration.between(Instant.now(), accessToken.getExpiresAt()).getSeconds();
        }
        // 构建响应
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", accessToken.getTokenValue());
        response.put("token_type", accessToken.getTokenType().getValue());
        response.put("expires_in", expiresIn);
        response.put("scope", String.join(" ", accessToken.getScopes()));
        response.put("username", authentication.getName());
        // response.put("client_id", clientId);
        if (accessToken.getExpiresAt() != null) {
            response.put("expires_in",
                    Instant.now().until(accessToken.getExpiresAt(), ChronoUnit.SECONDS));
        }

        response.put("refresh_token", refreshToken != null ? refreshToken.getTokenValue() : null);

        return response;
    }

    /**
     * 创建新用户
     * 
     * @param username 用户名
     * @param password 密码
     * @param email    邮箱
     * @param realName 真实姓名
     * @return 创建的用户
     */
    @Transactional
    public SysUser createUser(String username, String password, String email, String realName) {
        // 检查用户名是否已存在
        SysUser existingUser = sysUserService.getByUsername(username);
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建新用户
        SysUser newUser = new SysUser();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password)); // 使用密码加密器加密密码
        newUser.setEmail(email);
        newUser.setRealName(realName);
        newUser.setStatus(1); // 启用状态

        // 保存用户
        sysUserService.save(newUser);
        return newUser;
    }

    /**
     * 验证密码
     * 
     * @param rawPassword     原始密码
     * @param encodedPassword 加密后的密码
     * @return 密码是否匹配
     */
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}