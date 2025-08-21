package com.webapp.security.admin.auth.oauth2.controller;

import com.webapp.security.admin.auth.oauth2.config.OAuth2ClientProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2配置控制器
 * 提供前端需要的配置信息
 */
@RestController
@RequestMapping("/api/oauth2/config")
public class OAuth2ConfigController {

    @Autowired
    private OAuth2ClientProperties oauth2ClientProperties;

    /**
     * 获取OAuth2配置信息
     * 
     * @return 包含配置参数的Map
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enablePkce", oauth2ClientProperties.isEnablePkce());

        // 未来可扩展更多配置
        // config.put("supportedPlatforms", Arrays.asList("alipay", "wechat",
        // "github"));
        // config.put("otherConfig", "value");

        return ResponseEntity.ok(config);
    }
}