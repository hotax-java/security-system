package com.webapp.security.sso.auths.third;

import com.webapp.security.core.config.ClientIdConfig;
import com.webapp.security.core.entity.SysUser;
import com.webapp.security.core.service.SysUserService;
import com.webapp.security.sso.context.SpringContextHolder;
import com.webapp.security.sso.context.ClientContext;
import com.webapp.security.sso.auths.oauth2.service.OAuth2Service;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

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
        private PasswordEncoder passwordEncoder;

        @Autowired
        private OAuth2AuthorizationService authorizationService;

        @Value("${oauth2.server.base-url:https://885ro126ov70.vicp.fun}")
        private String issuerUri;

        @Autowired(required = false)
        private HttpServletRequest request;

        /********** OAuth2 授权码生成方法 **********/

        /**
         * 生成OAuth2授权码并重定向 (非PKCE模式)
         * 
         * @param user                系统用户
         * @param frontendCallbackUrl 前端回调URL
         * @param platform            平台标识
         * @return 重定向URL
         */
        public String generateAuthorizationCodeAndRedirect(SysUser user, String frontendCallbackUrl, String platform) {
                try {

                        // 获取webapp客户端配置
                        RegisteredClient registeredClient = oAuth2Service
                                        .getRegisteredClient(ClientContext.getClientId());
                        // 创建用户认证对象
                        Authentication authentication = getAuthentication(user);

                        // 创建OAuth2授权构建器
                        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization
                                        .withRegisteredClient(registeredClient)
                                        .principalName(authentication.getName())
                                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                                        .authorizedScopes(registeredClient.getScopes());

                        // 创建并添加OAuth2AuthorizationRequest到授权构建器
                        // 这是解决第三方授权码问题的关键 - 创建模拟的授权请求
                        Map<String, Object> additionalParameters = new HashMap<>();
                        // 生成随机的state值
                        String state = UUID.randomUUID().toString().replaceAll("-", "");
                        // 可以将平台信息和随机值结合
                        String combinedState = platform + ":" + state;
                        additionalParameters.put(OAuth2ParameterNames.STATE, combinedState);

                        // 构建授权URL
                        String authorizationUri = UriComponentsBuilder.fromUriString(issuerUri)
                                        .path("/oauth2/authorize")
                                        .build()
                                        .toUriString();

                        // 创建带有必要authorizationUri的授权请求对象
                        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                                        .authorizationUri(authorizationUri) // 使用配置的授权端点
                                        .clientId(registeredClient.getClientId())
                                        .redirectUri(frontendCallbackUrl)
                                        .scopes(registeredClient.getScopes())
                                        .state(state)
                                        .additionalParameters(additionalParameters)
                                        .build();

                        // 添加OAuth2AuthorizationRequest到属性中，解决authorizationRequest为null的问题
                        authorizationBuilder.attribute(OAuth2AuthorizationRequest.class.getName(),
                                        authorizationRequest);

                        // 添加Principal到属性中，解决token生成时principal为null的问题
                        authorizationBuilder.attribute(java.security.Principal.class.getName(), authentication);

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
                                        .queryParam("clientId", ClientContext.getClientId())
                                        .queryParam("platform", platform)
                                        .build()
                                        .toUriString();

                } catch (Exception e) {
                        logger.error("生成OAuth2授权码失败", e);
                        throw new RuntimeException("生成授权码失败: " + e.getMessage());
                }
        }

        /**
         * 生成OAuth2授权码并重定向 (PKCE模式)
         * 
         * @param user                系统用户
         * @param frontendCallbackUrl 前端回调URL
         * @param platform            平台标识
         * @param codeChallenge       PKCE码挑战
         * @param codeChallengeMethod PKCE码挑战方法
         * @return 重定向URL
         */
        public String generateAuthorizationCodeAndRedirectWithPkce(SysUser user, String frontendCallbackUrl,
                        String platform, String codeChallenge, String codeChallengeMethod) {
                try {
                        // 获取webapp客户端配置
                        RegisteredClient registeredClient = oAuth2Service
                                        .getRegisteredClient(ClientContext.getClientId());

                        // 创建用户认证对象
                        Authentication authentication = getAuthentication(user);

                        // 创建OAuth2授权构建器
                        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization
                                        .withRegisteredClient(registeredClient)
                                        .principalName(authentication.getName())
                                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                                        .authorizedScopes(registeredClient.getScopes());

                        // 生成随机的state值
                        String state = UUID.randomUUID().toString().replaceAll("-", "");
                        // 可以将平台信息和随机值结合
                        String combinedState = platform + ":" + state;

                        // 构建授权URL
                        String authorizationUri = UriComponentsBuilder.fromUriString(issuerUri)
                                        .path("/oauth2/authorize")
                                        .build()
                                        .toUriString();

                        // 添加PKCE相关参数到additionalParameters
                        Map<String, Object> additionalParameters = new HashMap<>();
                        additionalParameters.put(OAuth2ParameterNames.STATE, combinedState);
                        additionalParameters.put("code_challenge", codeChallenge);
                        additionalParameters.put("code_challenge_method",
                                        codeChallengeMethod != null ? codeChallengeMethod : "S256");

                        // 创建带有必要authorizationUri和PKCE参数的授权请求对象
                        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                                        .authorizationUri(authorizationUri)
                                        .clientId(registeredClient.getClientId())
                                        .redirectUri(frontendCallbackUrl)
                                        .scopes(registeredClient.getScopes())
                                        .state(state)
                                        .additionalParameters(additionalParameters)
                                        .build();

                        // 添加OAuth2AuthorizationRequest到属性中，解决authorizationRequest为null的问题
                        authorizationBuilder.attribute(OAuth2AuthorizationRequest.class.getName(),
                                        authorizationRequest);

                        // 添加Principal到属性中，解决token生成时principal为null的问题
                        authorizationBuilder.attribute(java.security.Principal.class.getName(), authentication);

                        // 生成OAuth2授权码
                        OAuth2AuthorizationCode authorizationCode = oAuth2Service.generateAuthorizationCode(
                                        authentication, registeredClient, authorizationBuilder);

                        // 保存授权信息
                        OAuth2Authorization authorization = authorizationBuilder.build();
                        authorizationService.save(authorization);

                        logger.info("使用PKCE模式生成授权码，codeChallenge: {}, codeChallengeMethod: {}",
                                        codeChallenge, codeChallengeMethod);

                        // 构建重定向URL
                        return UriComponentsBuilder.fromUriString(frontendCallbackUrl)
                                        .queryParam("code", authorizationCode.getTokenValue())
                                        .queryParam("state", state)
                                        .queryParam("clientId", ClientContext.getClientId())
                                        .queryParam("platform", platform)
                                        .build()
                                        .toUriString();

                } catch (Exception e) {
                        logger.error("生成OAuth2授权码(PKCE)失败", e);
                        throw new RuntimeException("生成授权码失败: " + e.getMessage());
                }
        }

        /**
         * 获取用户认证对象并设置到安全上下文
         */
        private Authentication getAuthentication(SysUser user) {
                UserDetailsService userDetailsService = SpringContextHolder.getBean(UserDetailsService.class);
                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (request != null) {
                        HttpSession session = request.getSession(true);
                        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());
                }

                return authentication;
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