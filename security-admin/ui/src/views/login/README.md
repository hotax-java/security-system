# 第三方登录与PKCE流程

本文档介绍了系统中的第三方登录流程，特别是PKCE（Proof Key for Code Exchange）机制的实现和使用。

## PKCE简介

PKCE是OAuth 2.0的扩展，用于提高授权码流程的安全性，特别适用于原生应用和单页应用。PKCE通过在授权请求中添加一个动态生成的挑战码（code_challenge），来防止授权码被拦截后的攻击。

## 系统配置

### 后端配置

在`application.yml`中配置PKCE开关：

```yaml
webapp:
  oauth2:
    enable-pkce: false # 设置为true启用PKCE，false使用传统Basic认证
```

可以通过环境变量或JVM参数启用：

```
-Dwebapp.oauth2.enable-pkce=true
```

### 前端配置

前端会自动从后端获取PKCE配置，无需额外设置。前端通过调用`/api/oauth2/config`接口获取配置信息。

## 流程说明

### 传统流程（不使用PKCE）

1. 用户点击第三方登录按钮（支付宝/微信/GitHub等）
2. 前端直接跳转到SSO服务器的授权页面
3. SSO服务器重定向到第三方平台授权
4. 第三方平台授权后回调SSO服务器
5. SSO服务器生成授权码并直接重定向到前端回调页面
6. 前端回调页面直接调用后端接口`/api/oauth2/token`获取Token
7. 后端使用Basic认证（客户端ID和密钥）从SSO服务器获取Token
8. 前端保存Token并完成登录

### PKCE流程

1. 用户点击第三方登录按钮
2. 前端生成`code_verifier`和`code_challenge`，将`code_verifier`保存在会话存储中
3. 前端直接跳转到SSO服务器的授权页面，携带`code_challenge`和`code_challenge_method`参数
4. SSO服务器重定向到第三方平台授权
5. 第三方平台授权后回调SSO服务器，SSO服务器将`code_challenge`与授权码绑定
6. SSO服务器生成授权码并直接重定向到前端回调页面
7. 前端回调页面从会话存储中获取`code_verifier`，并调用后端接口`/api/oauth2/token`获取Token，携带`code_verifier`
8. 后端使用`code_verifier`从SSO服务器获取Token（SSO服务器会验证`code_verifier`与之前的`code_challenge`是否匹配）
9. 前端保存Token并完成登录

## 关键组件

### 后端组件

1. `OAuth2ClientProperties`：配置属性类，控制PKCE启用状态
2. `OAuth2ConfigController`：提供配置API，让前端获取当前配置
3. `OAuth2ClientService`：处理令牌交换，支持PKCE和Basic认证两种方式
4. `OAuth2AuthorizeController`：（仅保留`/api/oauth2/token`端点）处理令牌交换请求

### 前端组件

1. `AuthConfigService`：从后端获取并缓存配置信息
2. `PkceUtils`：生成和管理PKCE参数的工具类
3. `Login.tsx`：根据配置决定使用PKCE还是传统方式发起授权
4. `ThirdPartyCallback.tsx`：处理回调并根据配置使用相应方式获取Token

## 调试建议

1. 开启DEBUG级别日志，查看完整的请求和响应信息
2. 前端开发者工具中监控网络请求和会话存储
3. 使用`RequestLoggingFilter`查看详细的HTTP请求和响应内容
4. 观察SSO服务器日志，确认PKCE参数是否正确传递和验证

## 常见问题

1. **Token获取失败**：检查客户端配置、PKCE参数传递和SSO服务器配置
2. **code_verifier丢失**：确保前端正确存储和获取code_verifier
3. **PKCE验证失败**：确保code_challenge_method一致（默认S256） 