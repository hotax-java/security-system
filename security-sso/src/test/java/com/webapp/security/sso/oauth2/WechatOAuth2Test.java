package com.webapp.security.sso.oauth2;

import com.webapp.security.core.entity.SysUser;
import com.webapp.security.core.service.SysUserService;
import com.webapp.security.core.service.SysWechatUserService;
import com.webapp.security.sso.third.wechat.WechatOAuth2StateService;
import com.webapp.security.sso.third.wechat.WechatUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 微信登录功能测试类
 */
@SpringBootTest
@AutoConfigureMockMvc
public class WechatOAuth2Test {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WechatUserService wechatUserService;

    @MockBean
    private SysWechatUserService sysWechatUserService;

    @MockBean
    private SysUserService userService;

    @MockBean
    private WechatOAuth2StateService stateService;

    /**
     * 测试发起微信授权请求
     */
    @Test
    public void testAuthorize() throws Exception {
        // 模拟state生成
        String state = UUID.randomUUID().toString();
        when(stateService.generateAndSaveState()).thenReturn(state);

        // 模拟授权URL生成
        String authorizeUrl = "https://open.weixin.qq.com/connect/qrconnect?appid=test&redirect_uri=http://localhost:9000/oauth2/wechat/callback&response_type=code&scope=snsapi_login&state="
                + state + "#wechat_redirect";
        when(wechatUserService.getAuthorizeUrl(state)).thenReturn(authorizeUrl);

        // 发送请求并验证重定向
        mockMvc.perform(get("/oauth2/wechat/authorize"))
                .andExpect(status().is3xxRedirection());
    }

    /**
     * 测试微信授权回调 - 已关联用户
     */
    @Test
    public void testCallbackWithLinkedUser() throws Exception {
        // 模拟state验证
        when(stateService.validateState(anyString())).thenReturn(true);

        // 模拟微信用户信息
        WechatUserService.WechatUserInfo userInfo = new WechatUserService.WechatUserInfo();
        userInfo.setOpenid("test_openid");
        userInfo.setNickname("Test User");
        when(wechatUserService.getUserInfo(anyString())).thenReturn(userInfo);

        // 模拟已关联用户
        when(sysWechatUserService.processWechatUser(anyString(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(Optional.of(1L));

        // 发送请求并验证响应
        mockMvc.perform(get("/oauth2/wechat/callback")
                .param("code", "test_code")
                .param("state", "test_state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists());
    }

    /**
     * 测试微信授权回调 - 未关联用户
     */
    @Test
    public void testCallbackWithoutLinkedUser() throws Exception {
        // 模拟state验证
        when(stateService.validateState(anyString())).thenReturn(true);

        // 模拟微信用户信息
        WechatUserService.WechatUserInfo userInfo = new WechatUserService.WechatUserInfo();
        userInfo.setOpenid("test_openid");
        userInfo.setNickname("Test User");
        when(wechatUserService.getUserInfo(anyString())).thenReturn(userInfo);

        // 模拟未关联用户
        when(sysWechatUserService.processWechatUser(anyString(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(Optional.empty());

        // 发送请求并验证响应
        mockMvc.perform(get("/oauth2/wechat/callback")
                .param("code", "test_code")
                .param("state", "test_state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encryptedOpenId").exists())
                .andExpect(jsonPath("$.nickname").exists());
    }

    /**
     * 测试绑定已有账号
     */
    @Test
    public void testBindExistingAccount() throws Exception {
        // 模拟用户验证
        SysUser user = new SysUser();
        user.setUserId(1L);
        when(userService.getByUsername(anyString())).thenReturn(user);

        // 发送请求并验证响应
        mockMvc.perform(post("/oauth2/wechat/bind")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "testuser")
                .param("password", "password")
                .param("encryptedOpenId", "encrypted_openid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists());
    }

    /**
     * 测试创建新账号
     */
    @Test
    public void testCreateNewAccount() throws Exception {
        // 模拟用户创建
        SysUser user = new SysUser();
        user.setUserId(1L);
        when(userService.createUser(any())).thenReturn(true);

        // 发送请求并验证响应
        mockMvc.perform(post("/oauth2/wechat/create")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("encryptedOpenId", "encrypted_openid")
                .param("nickname", "Test User")
                .param("headimgurl", "http://example.com/avatar.jpg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists());
    }
}