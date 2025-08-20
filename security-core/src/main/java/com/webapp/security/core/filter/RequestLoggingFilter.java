package com.webapp.security.core.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求日志过滤器
 * 记录所有请求和响应的详细信息，包括请求体和响应体
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 包装请求和响应以便多次读取
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            // 记录请求开始
            log.info("【请求开始】{} {}", request.getMethod(), request.getRequestURI());

            // 执行请求
            filterChain.doFilter(requestWrapper, responseWrapper);

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // 记录请求详情
            logRequest(requestWrapper);

            // 记录响应详情
            logResponse(responseWrapper, duration);

            // 必须复制响应内容到原始响应
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        // 获取请求头信息
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

        // 记录请求信息
        log.info("【请求信息】方法: {}, URI: {}, 协议: {}",
                request.getMethod(), request.getRequestURI(), request.getProtocol());
        log.info("【请求头】{}", headers);
        log.info("【请求参数】{}", parameters);

        if (requestBody.length() > 0) {
            log.info("【请求体】{}", requestBody);
        }
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }

        String contentEncoding = request.getCharacterEncoding();
        if (contentEncoding == null) {
            contentEncoding = "UTF-8";
        }

        try {
            return new String(content, contentEncoding);
        } catch (UnsupportedEncodingException e) {
            log.warn("读取请求体失败", e);
            return "[无法读取请求体]";
        }
    }

    private void logResponse(ContentCachingResponseWrapper response, long duration) {
        // 获取响应体
        byte[] content = response.getContentAsByteArray();

        // 响应状态
        int status = response.getStatus();

        // 记录响应信息
        log.info("【响应信息】状态码: {}, 处理时间: {}ms", status, duration);

        // 记录响应体（如果不是二进制内容）
        if (content.length > 0) {
            String contentType = response.getContentType();
            if (contentType != null && !contentType.startsWith("image/") && !contentType.startsWith("video/")
                    && !contentType.startsWith("audio/") && !contentType.startsWith("application/octet-stream")) {
                String contentEncoding = response.getCharacterEncoding();
                if (contentEncoding == null) {
                    contentEncoding = "UTF-8";
                }

                try {
                    String responseBody = new String(content, contentEncoding);
                    log.info("【响应体】{}", responseBody);
                } catch (UnsupportedEncodingException e) {
                    log.warn("读取响应体失败", e);
                }
            } else {
                log.info("【响应体】二进制内容，长度: {} bytes", content.length);
            }
        }
    }
}