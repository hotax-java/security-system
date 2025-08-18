package com.webapp.security.sso.third.alipay;

import com.webapp.security.core.entity.SysUser;
import com.webapp.security.core.model.OAuth2ErrorResponse;
import com.webapp.security.core.service.SysAlipayUserService;
import com.webapp.security.core.service.SysUserService;
import com.webapp.security.sso.third.alipay.AlipayUserService.AlipayUserInfo;
import com.webapp.security.sso.third.UserLoginService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 支付宝OAuth2控制器
 */
@RestController
@RequestMapping("/oauth2/alipay")
public class AlipayOAuth2Controller {
    private static final Logger logger = LoggerFactory.getLogger(AlipayOAuth2Controller.class);
    private static final String ENCRYPTION_KEY = "AlipayOAuth2KeyAlipayOAuth2Key12"; // 32字节密钥
    private static final String ENCRYPTION_ALGORITHM = "AES";

    @Autowired
    private AlipayOAuth2Config alipayConfig;

    @Autowired
    private AlipayUserService alipayUserService;

    @Autowired
    private AlipayOAuth2StateService stateService;

    @Autowired
    private UserLoginService userLoginService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysAlipayUserService sysAlipayUserService;

    /**
     * 重定向到支付宝授权页面
     */
    @GetMapping("/authorize")
    public RedirectView authorize() {
        // 使用stateService生成state
        String state = stateService.generateAndSaveState();
        logger.info("发起支付宝授权请求，state: {}", state);

        return new RedirectView(alipayUserService.getAuthorizeUrl(state));
    }

    /**
     * 处理支付宝回调
     * 重定向到前端React应用的回调页面
     */
    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam("auth_code") String authCode,
            @RequestParam("state") String state) throws UnsupportedEncodingException {

        // 使用stateService验证state
        if (!stateService.validateState(state)) {
            // 重定向到前端错误页面
            String redirectUrl = UriComponentsBuilder.fromUriString(alipayConfig.getFrontendCallbackUrl())
                    .queryParam("error", URLEncoder.encode("无效的state参数，可能是CSRF攻击", StandardCharsets.UTF_8.name()))
                    .build()
                    .toUriString();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, redirectUrl)
                    .build();
        }

        try {
            // 获取支付宝用户信息
            AlipayUserInfo alipayUser = alipayUserService.getUserInfo(authCode);

            if (alipayUser == null) {
                // 重定向到前端错误页面
                String redirectUrl = UriComponentsBuilder.fromUriString(alipayConfig.getFrontendCallbackUrl())
                        .queryParam("error", URLEncoder.encode("获取支付宝用户信息失败", StandardCharsets.UTF_8.name()))
                        .build()
                        .toUriString();

                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, redirectUrl)
                        .build();
            }

            // 处理支付宝用户登录
            Optional<Long> userId = sysAlipayUserService.processAlipayUser(
                    alipayUser.getUserId(),
                    alipayUser.getNickName(),
                    alipayUser.getAvatar(),
                    alipayUser.getGender(),
                    alipayUser.getProvince(),
                    alipayUser.getCity(),
                    null, // accessToken
                    null // refreshToken
            );

            if (userId.isPresent()) {
                // 已关联，直接生成令牌
                SysUser user = sysUserService.getById(userId.get());
                if (user != null) {
                    // 生成访问令牌
                    Map<String, Object> tokenInfo = userLoginService.generateUserToken(user);

                    // 重定向到前端应用，并附带令牌
                    String redirectUrl = UriComponentsBuilder.fromUriString(alipayConfig.getFrontendCallbackUrl())
                            .queryParam("access_token", tokenInfo.get("access_token"))
                            .queryParam("token_type", tokenInfo.get("token_type"))
                            .queryParam("expires_in", tokenInfo.get("expires_in"))
                            .queryParam("refresh_token", tokenInfo.get("refresh_token"))
                            .queryParam("username", tokenInfo.get("username"))
                            .build()
                            .toUriString();

                    return ResponseEntity.status(HttpStatus.FOUND)
                            .header(HttpHeaders.LOCATION, redirectUrl)
                            .build();
                }
            }

            // 未关联，返回选择页面，加密支付宝用户ID
            String encryptedAlipayUserId = encryptAlipayUserId(alipayUser.getUserId());

            // 重定向到前端，附带必要的参数
            String redirectUrl = UriComponentsBuilder.fromUriString(alipayConfig.getFrontendCallbackUrl())
                    .queryParam("platform", "alipay")
                    .queryParam("encryptedOpenId", encryptedAlipayUserId)
                    .queryParam("nickname", URLEncoder.encode(alipayUser.getNickName(), StandardCharsets.UTF_8.name()))
                    .queryParam("headimgurl", alipayUser.getAvatar())
                    .build()
                    .toUriString();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, redirectUrl)
                    .build();

        } catch (Exception e) {
            logger.error("支付宝OAuth2回调处理失败", e);
            // 重定向到前端错误页面
            String redirectUrl = UriComponentsBuilder.fromUriString(alipayConfig.getFrontendCallbackUrl())
                    .queryParam("error",
                            URLEncoder.encode("处理支付宝登录时发生错误: " + e.getMessage(), StandardCharsets.UTF_8.name()))
                    .build()
                    .toUriString();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, redirectUrl)
                    .build();
        }
    }

    /**
     * 绑定已有账户
     */
    @PostMapping("/bind")
    public ResponseEntity<?> bindExistingUser(
            @RequestParam("encryptedOpenId") String encryptedAlipayUserId,
            @RequestParam("username") String username,
            @RequestParam("password") String password) {

        try {
            // 解密支付宝用户ID
            String alipayUserId = decryptAlipayUserId(encryptedAlipayUserId);

            // 验证用户名密码
            SysUser user = sysUserService.getByUsername(username);
            if (user == null) {
                return OAuth2ErrorResponse.error(OAuth2ErrorResponse.INVALID_GRANT, "用户名或密码错误", HttpStatus.UNAUTHORIZED);
            }

            if (!userLoginService.verifyPassword(password, user.getPassword())) {
                return OAuth2ErrorResponse.error(OAuth2ErrorResponse.INVALID_GRANT, "用户名或密码错误", HttpStatus.UNAUTHORIZED);
            }

            // 绑定账号
            sysAlipayUserService.bindUser(alipayUserId, user.getUserId());

            // 生成JWT
            Map<String, Object> accessToken = userLoginService.generateUserToken(user);

            return ResponseEntity.ok(accessToken);

        } catch (Exception e) {
            logger.error("绑定支付宝账号失败", e);
            return OAuth2ErrorResponse.error(OAuth2ErrorResponse.SERVER_ERROR, "绑定失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 创建新账号
     */
    @PostMapping("/create")
    public ResponseEntity<?> createNewAccount(
            @RequestParam("encryptedOpenId") String encryptedAlipayUserId,
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String headimgurl) {

        try {
            // 解密支付宝用户ID
            String alipayUserId = decryptAlipayUserId(encryptedAlipayUserId);

            // 创建新用户
            SysUser user = userLoginService.createUser(
                    username,
                    password,
                    null, // email
                    nickname != null ? nickname : "支付宝用户");

            // 绑定支付宝账号
            sysAlipayUserService.bindAlipayToUser(
                    alipayUserId,
                    nickname,
                    headimgurl,
                    null, // gender
                    null, // province
                    null, // city
                    null, // accessToken
                    null, // refreshToken
                    user.getUserId());

            // 生成JWT
            Map<String, Object> accessToken = userLoginService.generateUserToken(user);
            return ResponseEntity.ok(accessToken);

        } catch (Exception e) {
            logger.error("创建新账号失败", e);
            return OAuth2ErrorResponse.error(OAuth2ErrorResponse.SERVER_ERROR, "创建账号失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 加密支付宝用户ID
     */
    private String encryptAlipayUserId(String alipayUserId) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), ENCRYPTION_ALGORITHM);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(alipayUserId.getBytes());
            return Base64.getUrlEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            logger.error("加密支付宝用户ID失败", e);
            return null;
        }
    }

    /**
     * 解密支付宝用户ID
     */
    private String decryptAlipayUserId(String encryptedAlipayUserId) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), ENCRYPTION_ALGORITHM);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getUrlDecoder().decode(encryptedAlipayUserId);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted);
        } catch (Exception e) {
            logger.error("解密支付宝用户ID失败", e);
            return null;
        }
    }


}