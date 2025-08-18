package com.webapp.security.core.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.webapp.security.core.context.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus自动填充处理器
 * 用于自动填充创建时间、更新时间、创建者、更新者等字段
 */
public class MyBatisPlusMetaObjectHandler implements MetaObjectHandler {

    private static final Logger log = LoggerFactory.getLogger(MyBatisPlusMetaObjectHandler.class);

    /**
     * 构造函数，打印日志确认初始化
     */
    public MyBatisPlusMetaObjectHandler() {
        log.info("MyBatisPlusMetaObjectHandler initialized");
    }

    /**
     * 插入时的填充策略
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("Start insert fill ...");

        // 自动填充创建时间和更新时间
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        // 设置创建者
        String currentUsername = getCurrentUsername();
        if (currentUsername != null && metaObject.hasSetter("createBy")) {
            this.strictInsertFill(metaObject, "createBy", String.class, currentUsername);
            log.debug("Auto fill createBy with: {}", currentUsername);
        }

        // 同时设置更新者（初始情况下与创建者相同）
        if (currentUsername != null && metaObject.hasSetter("updateBy")) {
            this.strictInsertFill(metaObject, "updateBy", String.class, currentUsername);
            log.debug("Auto fill updateBy with: {}", currentUsername);
        }
    }

    /**
     * 更新时的填充策略
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("Start update fill ...");

        // 自动填充更新时间
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        // 设置更新者
        String currentUsername = getCurrentUsername();
        if (currentUsername != null && metaObject.hasSetter("updateBy")) {
            this.strictUpdateFill(metaObject, "updateBy", String.class, currentUsername);
            log.debug("Auto fill updateBy with: {}", currentUsername);
        }
    }

    /**
     * 获取当前用户名
     * 如果无法获取当前用户（如系统任务），则返回"system"
     */
    private String getCurrentUsername() {
        String username = UserContext.getCurrentUsername();
        return username != null ? username : "system";
    }
}