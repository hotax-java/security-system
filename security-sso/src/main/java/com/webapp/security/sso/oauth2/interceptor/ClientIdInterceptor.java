package com.webapp.security.sso.oauth2.interceptor;

import com.webapp.security.sso.oauth2.context.ClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 客户端ID拦截�? * 从请求头中提取X-Client-Id并存储到ClientContext�? */
@Component
public class ClientIdInterceptor implements HandlerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(ClientIdInterceptor.class);
    
    private static final String CLIENT_ID_HEADER = "X-Client-Id";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientId = request.getHeader(CLIENT_ID_HEADER);
        
        // 设置到ClientContext
        ClientContext.setClientId(clientId);
        
        log.debug("Set clientId to context: {}", ClientContext.getClientId());
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        // 请求完成后清除上下文，避免内存泄
        ClientContext.clear();
        log.debug("Cleared clientId from context");
    }
}

