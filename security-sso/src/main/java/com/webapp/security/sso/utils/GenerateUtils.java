package com.webapp.security.sso.utils;

import java.security.SecureRandom;

public class GenerateUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 生成16进制格式的令牌值
     * 格式类似: 5a89faa7b4fe1ba7537679c0d7c94039
     */
    public static String generateShortToken() {
        byte[] randomBytes = new byte[16]; // 16字节会生成32个16进制字符
        SECURE_RANDOM.nextBytes(randomBytes);
        StringBuilder hexString = new StringBuilder();
        for (byte b : randomBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }
}
