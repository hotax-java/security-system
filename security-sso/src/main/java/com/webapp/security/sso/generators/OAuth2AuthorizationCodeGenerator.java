package com.webapp.security.sso.generators;

import com.webapp.security.sso.utils.GenerateUtils;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 授权码生成器
 * 自定义实现授权码生成逻辑
 */
public class OAuth2AuthorizationCodeGenerator implements OAuth2TokenGenerator<OAuth2AuthorizationCode> {

        @Override
        public OAuth2AuthorizationCode generate(OAuth2TokenContext context) {
                if (context.getTokenType() == null ||
                                !OAuth2ParameterNames.CODE.equals(context.getTokenType().getValue())) {
                        return null;
                }

                // 生成随机授权码值
                String tokenValue = GenerateUtils.generateShortToken();
                Instant issuedAt = Instant.now();
                // 授权码通常较短有效期，如10分钟
                Instant expiresAt = issuedAt.plus(10, ChronoUnit.MINUTES);

                return new OAuth2AuthorizationCode(tokenValue, issuedAt, expiresAt);
        }

}