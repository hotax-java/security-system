# SSO集成方案 - 技术设计文档

## 概述

本方案实现了一个可配置的SSO（单点登录）系统，支持传统登录和SSO登录的无缝切换，同时集成了PKCE（Proof Key for Code Exchange）安全机制和第三方登录功能。

## 核心特性

### 1. 登录模式切换
- **SSO模式**：跳转到 `https://885ro126ov70.vicp.fun` 进行统一认证
- **传统模式**：使用本地用户名密码登录
- **环境变量控制**：通过 `REACT_APP_ENABLE_SSO` 开关控制

### 2. PKCE安全机制
- 支持OAuth2 PKCE扩展，提高授权码流程安全性
- 使用SHA-256算法生成code_challenge
- 自动管理code_verifier的存储和清理

### 3. 第三方登录集成
- 支持微信、GitHub、支付宝等平台
- 可配置支持的平台列表
- 统一的授权URL生成机制

## 技术架构

### 配置管理层
```
├── config/
│   └── loginConfig.ts          # 统一登录配置管理
├── services/
│   └── authConfigService.ts    # 认证配置服务（已更新）
└── .env files                  # 环境变量配置
```

### 核心组件
```
├── views/login/
│   └── Login.tsx              # 登录页面（已更新）
├── apis/
│   └── api.ts                 # API拦截器（已更新）
└── utils/
    └── pkceUtils.ts           # PKCE工具类
```

## 环境变量配置

### 开发环境 (.env.development)
```bash
# ===== 登录模式配置 =====
REACT_APP_ENABLE_SSO=true

# ===== SSO服务器配置 =====
REACT_APP_SSO_BASE_URL=https://885ro126ov70.vicp.fun

# ===== OAuth2/PKCE配置 =====
REACT_APP_ENABLE_PKCE=true
REACT_APP_OAUTH2_CLIENT_ID=webapp-client
REACT_APP_OAUTH2_REDIRECT_URI=http://localhost:8081/callback
REACT_APP_OAUTH2_SCOPE=read write openid profile offline_access
REACT_APP_OAUTH2_CODE_CHALLENGE_METHOD=S256

# ===== 第三方登录配置 =====
REACT_APP_ENABLE_THIRD_PARTY_LOGIN=true
REACT_APP_SUPPORTED_PLATFORMS=wechat,github,alipay
```

### 生产环境 (.env.production)
```bash
# ===== 登录模式配置 =====
REACT_APP_ENABLE_SSO=true

# ===== SSO服务器配置 =====
REACT_APP_SSO_BASE_URL=https://885ro126ov70.vicp.fun

# ===== OAuth2/PKCE配置 =====
REACT_APP_OAUTH2_CLIENT_ID=webapp-client
REACT_APP_OAUTH2_REDIRECT_URI=http://host.docker.internal:8091/callback
REACT_APP_OAUTH2_SCOPE=read write openid profile offline_access
REACT_APP_OAUTH2_CODE_CHALLENGE_METHOD=S256
```

## 主要修改点

### 1. 新增文件

#### `src/config/loginConfig.ts`
- **功能**：统一登录配置管理
- **特性**：
  - 环境变量读取和缓存
  - URL生成（登录、第三方授权、Token交换）
  - 平台支持检查
  - 配置验证

#### `.env.example`
- **功能**：环境变量模板
- **用途**：开发者参考和部署指导

### 2. 更新文件

#### `src/services/authConfigService.ts`
- **变更**：集成LoginConfigManager
- **新增方法**：
  - `isSSO()`: 检查SSO开关
  - `getLoginUrl()`: 获取登录URL
  - `getThirdPartyAuthUrl()`: 获取第三方授权URL
  - `isPlatformSupported()`: 平台支持检查

#### `src/views/login/Login.tsx`
- **变更**：支持SSO模式切换
- **新增功能**：
  - SSO开关检测
  - 动态第三方平台显示
  - 配置化URL生成
  - 模式提示信息

#### `src/apis/api.ts`
- **变更**：拦截器跳转逻辑
- **修改点**：所有 `window.location.replace('/login')` 改为使用 `AuthConfigService.getLoginUrl()`

#### 环境配置文件
- **`.env.development`**: 开发环境SSO配置
- **`.env.production`**: 生产环境SSO配置

## 工作流程

### SSO登录流程
```
1. 用户访问Dashboard
2. 检测到未认证状态
3. 拦截器检查SSO配置
4. 跳转到 https://885ro126ov70.vicp.fun/login?returnUrl=...
5. 用户在SSO页面完成认证
6. 返回原页面并携带认证信息
```

### 第三方登录流程（PKCE模式）
```
1. 用户点击第三方登录按钮
2. 生成PKCE参数（code_verifier, code_challenge）
3. 存储code_verifier到sessionStorage
4. 跳转到第三方授权页面（携带PKCE参数）
5. 用户授权后返回callback页面
6. 使用授权码和code_verifier交换access_token
```

## 安全特性

### 1. PKCE机制
- **code_verifier**: 43-128字符随机字符串
- **code_challenge**: SHA-256哈希 + Base64URL编码
- **code_challenge_method**: S256
- **存储方式**: sessionStorage（会话级别）

### 2. 状态管理
- **state参数**: 防止CSRF攻击
- **会话隔离**: 每个授权流程独立
- **自动清理**: 授权完成后清理临时数据

### 3. 错误处理
- **网络错误**: 自动重试机制
- **认证失败**: 清理token并重定向
- **配置错误**: 友好错误提示

## 部署注意事项

### 1. 环境变量配置
- 确保所有必需的环境变量已正确设置
- 生产环境需要更新回调URI为实际域名
- 检查SSO服务器地址的可访问性

### 2. 网络配置
- 确保前端应用可以访问SSO服务器
- 配置CORS策略允许跨域请求
- 检查防火墙和代理设置

### 3. 安全配置
- 使用HTTPS协议
- 配置适当的CSP策略
- 定期更新依赖包

## 测试建议

### 1. 功能测试
- [ ] SSO开关切换测试
- [ ] 传统登录功能测试
- [ ] 第三方登录流程测试
- [ ] PKCE参数生成和验证
- [ ] 错误处理和重试机制

### 2. 兼容性测试
- [ ] 不同浏览器兼容性
- [ ] 移动端适配
- [ ] 网络异常情况处理

### 3. 安全测试
- [ ] PKCE流程安全性验证
- [ ] CSRF攻击防护测试
- [ ] Token安全存储验证

## 扩展性

### 1. 新增第三方平台
1. 在环境变量中添加平台名称
2. 在LoginConfigManager中添加平台配置
3. 在Login.tsx中添加对应的图标和按钮

### 2. 自定义认证流程
1. 扩展LoginConfigManager接口
2. 添加新的认证方法
3. 更新Login组件支持新流程

### 3. 多环境支持
1. 创建对应的.env文件
2. 配置构建脚本
3. 更新部署流程

## 总结

本方案提供了一个完整的、可配置的SSO集成解决方案，具有以下优势：

- **灵活性**：支持SSO和传统登录模式切换
- **安全性**：集成PKCE机制和多重安全防护
- **可扩展性**：模块化设计，易于扩展新功能
- **可维护性**：统一配置管理，代码结构清晰
- **用户体验**：无感知切换，友好的错误提示

通过环境变量的简单配置，即可在不同部署环境中灵活切换登录模式，满足不同场景的需求。