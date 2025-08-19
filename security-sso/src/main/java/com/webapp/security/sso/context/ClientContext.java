package com.webapp.security.sso.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端上下文管理
 * 使用ThreadLocal保存当前请求的clientId，线程安全且便于访问
 */
public class ClientContext {
    
    private static final Logger log = LoggerFactory.getLogger(ClientContext.class);
    
    /**
     * 默认客户端ID
     */
    public static final String DEFAULT_CLIENT_ID = "webapp-client";
    
    /**
     * ThreadLocal存储当前线程的clientId
     */
    private static final ThreadLocal<String> CLIENT_ID_HOLDER = new ThreadLocal<>();
    
    /**
     * 设置当前线程的clientId
     * 
     * @param clientId 客户端ID
     */
    public static void setClientId(String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            clientId = DEFAULT_CLIENT_ID;
            log.debug("ClientId is null or empty, using default: {}", clientId);
        }
        CLIENT_ID_HOLDER.set(clientId.trim());
        log.debug("Set clientId for current thread: {}", clientId);
    }
    
    /**
     * 获取当前线程的clientId
     * 
     * @return 客户端ID，如果未设置则返回默认�?     */
    public static String getClientId() {
        String clientId = CLIENT_ID_HOLDER.get();
        if (clientId == null) {
            clientId = DEFAULT_CLIENT_ID;
            log.debug("No clientId found in current thread, using default: {}", clientId);
        }
        return clientId;
    }
    
    /**
     * 清除当前线程的clientId
     * 重要：请求完成后必须调用此方法，避免内存泄漏
     */
    public static void clear() {
        String clientId = CLIENT_ID_HOLDER.get();
        CLIENT_ID_HOLDER.remove();
        log.debug("Cleared clientId from current thread: {}", clientId);
    }
    
    /**
     * 检查当前线程是否设置了clientId
     * 
     * @return true如果已设置，false如果未设�?     */
    public static boolean hasClientId() {
        return CLIENT_ID_HOLDER.get() != null;
    }
    
    /**
     * 获取当前线程的clientId，如果未设置则抛出异�?     * 
     * @return 客户端ID
     * @throws IllegalStateException 如果未设置clientId
     */
    public static String requireClientId() {
        String clientId = CLIENT_ID_HOLDER.get();
        if (clientId == null) {
            throw new IllegalStateException("ClientId not found in current thread context");
        }
        return clientId;
    }
}

