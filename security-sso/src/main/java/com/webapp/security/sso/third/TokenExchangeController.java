package com.webapp.security.sso.third;

import com.webapp.security.core.model.OAuth2ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Token换取控制器
 * 用于前端通过code换取token信息
 */
@RestController
@RequestMapping("/oauth2/token")
public class TokenExchangeController {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenExchangeController.class);
    
    @Autowired
    private RedisCodeService redisCodeService;
    
    /**
     * 通过code换取token信息
     * 
     * @param code 一次性code
     * @return token信息
     */
    @PostMapping("/exchange")
    public ResponseEntity<?> exchangeToken(@RequestParam("code") String code) {
        try {
            // 验证并消费code
            Object tokenInfo = redisCodeService.validateAndConsumeTokenCode(code);
            
            if (tokenInfo == null) {
                logger.warn("无效的token code: {}", code);
                return OAuth2ErrorResponse.error(
                    OAuth2ErrorResponse.INVALID_GRANT, 
                    "无效的code或code已过期", 
                    HttpStatus.BAD_REQUEST
                );
            }
            
            logger.info("成功通过code换取token: {}", code);
            return ResponseEntity.ok(tokenInfo);
            
        } catch (Exception e) {
            logger.error("token换取失败, code: {}", code, e);
            return OAuth2ErrorResponse.error(
                OAuth2ErrorResponse.SERVER_ERROR, 
                "token换取失败: " + e.getMessage(), 
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}