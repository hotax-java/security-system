package com.webapp.security.sso.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * 自定义身份验证入口点，用于确保重定向URL使用当前请求的主机名
 */
public class CustomLoginUrlAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

    public CustomLoginUrlAuthenticationEntryPoint(String loginFormUrl) {
        super(loginFormUrl);
    }

    /**
     * 重写构建重定向URL的方法，确保使用当前请求的主机名和协议
     */
    @Override
    protected String buildRedirectUrlToLoginPage(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) {
        String redirectUrl = super.buildRedirectUrlToLoginPage(request, response, authException);

        try {
            // 获取当前请求的主机名和协议
            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();

            // 解析原始重定向URL
            URI originalUri = new URI(redirectUrl);

            // 使用当前请求的主机名和协议构建新的URI
            URI newUri = new URI(
                    scheme,
                    originalUri.getUserInfo(),
                    serverName,
                    serverPort,
                    originalUri.getPath(),
                    originalUri.getQuery(),
                    originalUri.getFragment());

            return newUri.toString();
        } catch (URISyntaxException e) {
            // 如果发生异常，返回原始URL
            return redirectUrl;
        }
    }
}