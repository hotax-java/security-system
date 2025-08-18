package com.webapp.security.sso;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证密码编码器一致性的测试
 */
public class PasswordEncoderTest {

    /**
     * 测试SSO服务的BCryptPasswordEncoder与Admin服务中静态定义的结果兼容
     */
    @Test
    public void testPasswordEncoderCompatibility() {
        // 模拟Admin服务中静态定义的密码编码器
        BCryptPasswordEncoder adminEncoder = new BCryptPasswordEncoder();

        // 模拟SSO服务中Bean配置的密码编码器
        BCryptPasswordEncoder ssoEncoder = new BCryptPasswordEncoder();

        // 测试密码
        String rawPassword = "RD3635OvoWKAlepK_EH5uTDWIIVRCNx0";

        // 使用Admin编码器加密
        String adminEncoded = adminEncoder.encode(rawPassword);

        // 使用SSO编码器验证 - 应该能成功
        boolean matches = ssoEncoder.matches(rawPassword, adminEncoded);

        // 验证结果
        assertTrue(matches, "SSO服务的密码编码器应该能验证Admin服务加密的密码");

        // 反向测试 - SSO加密，Admin验证
        String ssoEncoded = ssoEncoder.encode(rawPassword);
        boolean reverseMatches = adminEncoder.matches(rawPassword, ssoEncoded);

        // 验证结果
        assertTrue(reverseMatches, "Admin服务的密码编码器应该能验证SSO服务加密的密码");
    }
}