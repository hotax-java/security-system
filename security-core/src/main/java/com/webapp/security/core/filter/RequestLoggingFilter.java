package com.webapp.security.core.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.FilterConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求日志过滤器
 * 记录所有HTTP请求和响应的详细信息，包括请求头、参数、请求体、响应头、响应体等
 */
@Component
public class RequestLoggingFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.debug("RequestLoggingFilter初始化");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // 包装请求和响应以便多次读取内容
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper((HttpServletRequest) request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(
                (HttpServletResponse) response);

        long startTime = System.currentTimeMillis();
        try {
            // 执行请求
            chain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // 记录请求和响应信息
            if (log.isDebugEnabled()) {
                logRequest(requestWrapper);
                logResponse(responseWrapper, duration);
            }

            // 复制响应内容到原始响应
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 获取请求头
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }

        // 获取请求参数
        Map<String, String[]> parameters = request.getParameterMap();

        // 获取请求体
        String requestBody = getRequestBody(request);

        log.debug("HTTP请求 => {} {} [Headers: {}] [Parameters: {}] [Body: {}]",
                method, uri, headers, parameters, requestBody);
    }

    private void logResponse(ContentCachingResponseWrapper response, long duration) {
        int status = response.getStatus();

        // 获取响应头
        Map<String, String> headers = new HashMap<>();
        Collection<String> headerNames = response.getHeaderNames();
        for (String headerName : headerNames) {
            headers.put(headerName, response.getHeader(headerName));
        }

        // 获取响应体
        String responseBody = getResponseBody(response);

        String responseBodySummary = responseBody;
        if (responseBody != null && responseBody.length() > 500) {
            // 如果响应体太长，只显示摘要
            responseBodySummary = responseBody.substring(0, 500) + "... [截断. 总长度: " + responseBody.length() + "]";
        }

        log.debug("HTTP响应 <= [Status: {}] [Headers: {}] [Body: {}] [耗时: {}ms]",
                status, headers, responseBodySummary, duration);
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }

        try {
            String contentEncoding = request.getCharacterEncoding();
            return new String(content, contentEncoding != null ? contentEncoding : "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.warn("无法解析请求体编码", e);
            return "[无法读取请求体]";
        }
    }

    private String getResponseBody(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper) {
            ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                try {
                    return new String(buf, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    log.warn("无法解析响应体编码", e);
                    return "[无法读取响应体]";
                }
            }
        }
        return null;
    }

    @Override
    public void destroy() {
        log.debug("RequestLoggingFilter销毁");
    }
}