package com.webapp.security.sso.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.webapp.security.sso.auths.oauth2.expand.MybatisOAuth2RegisteredClientService;
import com.webapp.security.sso.generators.ShortOpaqueTokenGenerator;

import com.webapp.security.sso.generators.OAuth2AuthorizationCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.webapp.security.sso.entity.OAuth2Jwk;
import com.webapp.security.sso.auths.oauth2.service.JwkService;
import com.webapp.security.sso.auths.oauth2.expand.UserDetailsServiceImpl;
import com.webapp.security.sso.auths.oauth2.expand.MyBatisOAuth2AuthorizationService;
import com.webapp.security.sso.mapper.OAuth2AuthorizationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * Spring Security配置
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

        private final UserDetailsServiceImpl userDetailsService;
        private final MybatisOAuth2RegisteredClientService registeredClientService;
        private final JwkService jwkService;

        /**
         * OAuth2授权服务器安全过滤器链
         */
        @Bean
        @Order(1)
        public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
                OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

                http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                                .registeredClientRepository(registeredClientService)
                                .oidc(Customizer.withDefaults()); // 启用OpenID Connect 1.0

                http
                                // 重定向到登录页面，当未认证的用户尝试访问受保护的端点时
                                .exceptionHandling((exceptions) -> exceptions
                                                .defaultAuthenticationEntryPointFor(
                                                                new LoginUrlAuthenticationEntryPoint("/login"),
                                                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                                // 接受access tokens进行用户信息和客户端注册
                                .oauth2ResourceServer((resourceServer) -> resourceServer
                                                .jwt(Customizer.withDefaults()));

                return http.build();
        }

        @Bean
        @Order(2)
        public SecurityFilterChain openApiSecurityFilterChain(
                        HttpSecurity http) throws Exception {
                http
                                // 使用securityMatcher替代requestMatchers+antMatchers
                                .securityMatcher("/api/v1/**", "/oauth2/introspect")
                                .authorizeHttpRequests(auth -> auth
                                                // 为自省端点添加特定的认证要求
                                                .requestMatchers("/oauth2/introspect").authenticated()
                                                // 其他API路径保持不变
                                                .anyRequest().authenticated())
                                // 为自省端点启用HTTP Basic认证
                                .httpBasic(Customizer.withDefaults())
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));
                return http.build();
        }

        /**
         * 默认安全过滤器链
         */
        @Bean
        @Order(99)
        public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests((authorize) -> authorize
                                                .requestMatchers("/login", "/logout", "/oauth2/**", "/v1/oauth2/**",
                                                                "/.well-known/jwks.json",
                                                                "/api/token-blacklist/**", "/favicon.ico",
                                                                "/css/**", "/js/**", "/images/**", "/webjars/**",
                                                                "/error", "/oauth2/**/**")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutSuccessUrl("/login?logout")
                                                .permitAll())
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

                return http.build();
        }

        /**
         * 密码编码器
         */
        @Bean
        @Primary
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        /**
         * 认证管理器
         */
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        /**
         * 认证提供者
         */
        @Bean
        public AuthenticationProvider authenticationProvider(
                        @Qualifier("passwordEncoder") PasswordEncoder passwordEncoder) {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder);
                return authProvider;
        }

        /**
         * JWK源
         */
        @Bean
        @DependsOn("flywayInitializer")
        public JWKSource<SecurityContext> jwkSource() {
                // 获取当前有效的JWK
                OAuth2Jwk currentJwk = jwkService.getCurrentJwk();
                RSAKey rsaKey = jwkService.toRSAKey(currentJwk);

                JWKSet jwkSet = new JWKSet(rsaKey);
                return new ImmutableJWKSet<>(jwkSet);
        }

        /**
         * JWT解码器
         */
        @Bean
        public JwtDecoder jwtDecoder(@Value("${oauth2.server.base-url}") String baseUrl) {
                return NimbusJwtDecoder.withJwkSetUri(baseUrl + "/oauth2/jwks").build();
        }

        /**
         * JWT编码器
         */
        @Bean
        public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
                return new NimbusJwtEncoder(jwkSource);
        }

        /**
         * OAuth2令牌生成器配置
         * 配置JWT、访问令牌、刷新令牌和短令牌生成器
         */
        @Bean
        public OAuth2TokenGenerator<?> tokenGenerator(JwtEncoder jwtEncoder,
                        OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer) {
                JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
                jwtGenerator.setJwtCustomizer(jwtCustomizer);

                OAuth2AuthorizationCodeGenerator authorizationCodeGenerator = new OAuth2AuthorizationCodeGenerator();

                OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
                ShortOpaqueTokenGenerator shortOpaqueTokenGenerator = new ShortOpaqueTokenGenerator();

                // Spring Authorization Server内置授权码生成
                return new DelegatingOAuth2TokenGenerator(
                                jwtGenerator,
                                shortOpaqueTokenGenerator,
                                refreshTokenGenerator,
                                authorizationCodeGenerator);
        }

        /**
         * OAuth2授权服务 - MyBatis实现（生产环境）
         * 授权记录持久化到oauth2_authorization表
         */
        @Bean
        @DependsOn("flywayInitializer")
        public OAuth2AuthorizationService authorizationService(OAuth2AuthorizationMapper authorizationMapper,
                        RegisteredClientRepository registeredClientRepository) {
                return new MyBatisOAuth2AuthorizationService(authorizationMapper, registeredClientRepository);
        }

        /**
         * 授权服务器设置
         */
        @Bean
        public AuthorizationServerSettings authorizationServerSettings(
                        @Value("${oauth2.server.base-url}") String baseUrl) {
                return AuthorizationServerSettings.builder()
                                .issuer(baseUrl)
                                .build();
        }
}
