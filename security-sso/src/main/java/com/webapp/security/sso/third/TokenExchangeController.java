package com.webapp.security.sso.third;

import com.webapp.security.core.entity.SysUser;
import com.webapp.security.core.model.OAuth2ErrorResponse;
import com.webapp.security.core.service.SysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Token换取控制器
 * 用于前端通过code换取token信息
 */
@RestController
@RequestMapping("/oauth2/token")
public class TokenExchangeController {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenExchangeController.class);
    
    @Autowired
    private AuthorizationCodeService authorizationCodeService;
    
    @Autowired
    private UserLoginService userLoginService;
    
    @Autowired
    private SysUserService sysUserService;
    
    /**
     * 通过token_code换取token信息
     * 
     * @param code token_code（原参数名保持不变以兼容前端）
     * @return token信息
     */
    @PostMapping("/exchange")
    public ResponseEntity<?> exchangeToken(@RequestParam("code") String code) {
        try {
            // 验证并消费token_code
            Long userId = authorizationCodeService.validateAndConsumeTokenCode(code);
            
            if (userId == null) {
                logger.warn("无效的token_code: {}", code);
                return OAuth2ErrorResponse.error(
                    OAuth2ErrorResponse.INVALID_GRANT, 
                    "无效的code或code已过期", 
                    HttpStatus.BAD_REQUEST
                );
            }
            
            // 根据userId获取用户信息
            SysUser user = sysUserService.getById(userId);
            if (user == null) {
                logger.warn("用户不存在, userId: {}", userId);
                return OAuth2ErrorResponse.error(
                    OAuth2ErrorResponse.INVALID_GRANT, 
                    "用户不存在", 
                    HttpStatus.BAD_REQUEST
                );
            }
            
            // 生成token
            Map<String, Object> tokenInfo = userLoginService.generateUserToken(user);
            
            logger.info("成功通过token_code换取token, userId: {}", userId);
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