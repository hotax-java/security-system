package com.webapp.security.sso.third.wechat;

import com.webapp.security.core.entity.SysUser;
import com.webapp.security.core.entity.SysWechatUser;
import com.webapp.security.core.model.OAuth2ErrorResponse;
import com.webapp.security.core.service.SysUserService;
import com.webapp.security.core.service.SysWechatUserService;
import com.webapp.security.sso.third.UserLoginService;
import com.webapp.security.sso.third.wechat.WechatUserService.WechatUserInfo;
import com.webapp.security.sso.third.RedisCodeService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 微信OAuth2控制器
 * 处理微信登录授权流程
 */
@RestController
@RequestMapping("/oauth2/wechat")
public class WechatOAuth2Controller {
    @Value("${wechat.oauth2.token:wechat2025}")
    private String token;

    private static final Logger logger = LoggerFactory.getLogger(WechatOAuth2Controller.class);
    private static final String ENCRYPTION_KEY = "WechatOAuth2KeyWechatOAuth2Key"; // 32字节密钥
    private static final String ENCRYPTION_ALGORITHM = "AES";

    @Autowired
    private WechatUserService wechatUserService;

    @Autowired
    private WechatOAuth2StateService stateService;

    @Autowired
    private UserLoginService userLoginService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysWechatUserService sysWechatUserService;

    @Autowired
    private WechatOAuth2Config wechatOAuth2Config;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisCodeService redisCodeService;

    /**
     * 发起微信授权请求
     */
    @GetMapping("/authorize")
    public RedirectView authorize() {
        // 生成并存储state，用于验证回调请求
        String state = stateService.generateAndSaveState();

        // 获取微信授权URL
        String authorizeUrl = wechatUserService.getAuthorizeUrl(state);

        return new RedirectView(authorizeUrl);
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam String code, @RequestParam String state)
            throws UnsupportedEncodingException {
        // 验证state，防止CSRF攻击
        if (!stateService.validateState(state)) {
            // 重定向到前端错误页面
            String redirectUrl = UriComponentsBuilder.fromUriString(wechatOAuth2Config.getFrontendCallbackUrl())
                    .queryParam("error", URLEncoder.encode("无效的state参数，可能是CSRF攻击", StandardCharsets.UTF_8.name()))
                    .build()
                    .toUriString();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, redirectUrl)
                    .build();
        }

        try {
            // 获取微信用户信息
            WechatUserInfo userInfo = wechatUserService.getUserInfo(code);
            if (userInfo == null) {
                // 重定向到前端错误页面
                String redirectUrl = UriComponentsBuilder.fromUriString(wechatOAuth2Config.getFrontendCallbackUrl())
                        .queryParam("error", URLEncoder.encode("获取微信用户信息失败", StandardCharsets.UTF_8.name()))
                        .build()
                        .toUriString();

                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, redirectUrl)
                        .build();
            }

            // 查询是否已关联系统用户
            Optional<Long> userId = sysWechatUserService.processWechatUser(
                    userInfo.getOpenid(),
                    userInfo.getUnionid(),
                    userInfo.getNickname(),
                    userInfo.getHeadimgurl(),
                    null, // 这里可以传入accessToken
                    null // 这里可以传入refreshToken
            );

            if (userId.isPresent()) {
                // 已关联，直接生成令牌
                SysUser user = sysUserService.getById(userId.get());
                if (user != null) {
                    // 生成访问令牌
                    Map<String, Object> tokenInfo = userLoginService.generateUserToken(user);

                    // 生成token code并重定向到前端
                    String tokenCode = redisCodeService.generateTokenCode(tokenInfo);
                    String redirectUrl = UriComponentsBuilder.fromUriString(wechatOAuth2Config.getFrontendCallbackUrl())
                            .queryParam("token_code", tokenCode)
                            .build()
                            .toUriString();

                    return ResponseEntity.status(HttpStatus.FOUND)
                            .header(HttpHeaders.LOCATION, redirectUrl)
                            .build();
                }
                // 如果用户不存在，返回错误
                String errorUrl = UriComponentsBuilder.fromUriString(wechatOAuth2Config.getFrontendCallbackUrl())
                        .queryParam("error", URLEncoder.encode("用户不存在", StandardCharsets.UTF_8.name()))
                        .build()
                        .toUriString();
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, errorUrl)
                        .build();
            } else {
                // 未关联，生成绑定code并重定向到前端选择页面
                String encryptedOpenId = encryptOpenId(userInfo.getOpenid());
                String bindCode = redisCodeService.generateBindCode(encryptedOpenId, "wechat");

                // 重定向到前端，附带必要的参数
                String redirectUrl = UriComponentsBuilder.fromUriString(wechatOAuth2Config.getFrontendCallbackUrl())
                        .queryParam("platform", "wechat")
                        .queryParam("code", bindCode)
                        .queryParam("nickname",
                                URLEncoder.encode(userInfo.getNickname(), StandardCharsets.UTF_8.name()))
                        .queryParam("headimgurl", userInfo.getHeadimgurl())
                        .build()
                        .toUriString();

                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, redirectUrl)
                        .build();
            }
        } catch (Exception e) {
            logger.error("微信OAuth2回调处理失败", e);
            // 重定向到前端错误页面
            String redirectUrl = UriComponentsBuilder.fromUriString(wechatOAuth2Config.getFrontendCallbackUrl())
                    .queryParam("error",
                            URLEncoder.encode("处理微信登录时发生错误: " + e.getMessage(), StandardCharsets.UTF_8.name()))
                    .build()
                    .toUriString();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, redirectUrl)
                    .build();
        }
    }

    /**
     * 绑定已有账号
     */
    @PostMapping("/bind")
    public ResponseEntity<?> bindExistingAccount(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String code) {

        try {
            // 验证并消费绑定code
            RedisCodeService.BindCodeData bindData = redisCodeService.validateAndConsumeBindCode(code);
            if (bindData == null || !"wechat".equals(bindData.getPlatform())) {
                return OAuth2ErrorResponse.error(OAuth2ErrorResponse.INVALID_GRANT, "无效的验证码或验证码已过期",
                        HttpStatus.BAD_REQUEST);
            }

            // 解密OpenID
            String openid = decryptOpenId(bindData.getEncryptedId());

            // 验证用户名密码
            SysUser user = sysUserService.getByUsername(username);
            if (user == null) {
                return OAuth2ErrorResponse.error(OAuth2ErrorResponse.INVALID_GRANT, "用户名或密码错误",
                        HttpStatus.UNAUTHORIZED);
            }

            if (!userLoginService.verifyPassword(password, user.getPassword())) {
                return OAuth2ErrorResponse.error(OAuth2ErrorResponse.INVALID_GRANT, "用户名或密码错误",
                        HttpStatus.UNAUTHORIZED);
            }

            // 绑定账号
            SysWechatUser wechatUser = sysWechatUserService.bindWechatToUser(
                    openid, null, null, null, null, null, user.getUserId());

            // 生成JWT
            Map<String, Object> tokenInfo = userLoginService.generateUserToken(user);
            return ResponseEntity.ok(tokenInfo);

        } catch (Exception e) {
            logger.error("绑定微信账号失败", e);
            return OAuth2ErrorResponse.error(OAuth2ErrorResponse.SERVER_ERROR, "绑定失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 创建新账号
     */
    @PostMapping("/create")
    public ResponseEntity<?> createNewAccount(
            @RequestParam String code,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String headimgurl) {

        try {
            // 验证并消费绑定code
            RedisCodeService.BindCodeData bindData = redisCodeService.validateAndConsumeBindCode(code);
            if (bindData == null || !"wechat".equals(bindData.getPlatform())) {
                return OAuth2ErrorResponse.error(OAuth2ErrorResponse.INVALID_GRANT, "无效的验证码或验证码已过期",
                        HttpStatus.BAD_REQUEST);
            }

            // 解密OpenID
            String openid = decryptOpenId(bindData.getEncryptedId());

            // 创建新用户
            SysUser user = userLoginService.createUser(
                    username,
                    password,
                    null, // email
                    nickname != null ? nickname : "微信用户");

            // 绑定微信账号
            SysWechatUser wechatUser = sysWechatUserService.bindWechatToUser(
                    openid, null, nickname, headimgurl, null, null, user.getUserId());

            // 生成JWT
            Map<String, Object> tokenInfo = userLoginService.generateUserToken(user);
            return ResponseEntity.ok(tokenInfo);

        } catch (Exception e) {
            logger.error("创建新账号失败", e);
            return OAuth2ErrorResponse.error(OAuth2ErrorResponse.SERVER_ERROR, "创建账号失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 加密OpenID
     */
    private String encryptOpenId(String openid) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), ENCRYPTION_ALGORITHM);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(openid.getBytes());
            return Base64.getUrlEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            logger.error("加密OpenID失败", e);
            return null;
        }
    }

    /**
     * 解密OpenID
     */
    private String decryptOpenId(String encryptedOpenId) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), ENCRYPTION_ALGORITHM);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getUrlDecoder().decode(encryptedOpenId);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted);
        } catch (Exception e) {
            logger.error("解密OpenID失败", e);
            return null;
        }
    }
}