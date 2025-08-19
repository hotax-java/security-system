# 自定义JWT功能

## 概述

这个模块提供了自定义JWT（JSON Web Token）功能，作为Spring Security OAuth2的补充。它允许你使用自定义的JWT令牌进行认证，而不影响现有的OAuth2功能。

## 功能特性

- ✅ 自定义JWT令牌生成和验证
- ✅ 访问令牌和刷新令牌支持
- ✅ 用户权限信息嵌入（与OAuth2保持一致）
- ✅ 用户详细信息嵌入（用户ID、真实姓名、邮箱、手机号等）
- ✅ 权限和角色信息查询
- ✅ 令牌过期时间配置
- ✅ 令牌撤销功能
- ✅ 条件注入（可选择性启用）
- ✅ 与现有OAuth2功能完全隔离
- ✅ 完整的用户详情服务支持

## 配置

### 1. 启用自定义JWT功能

在 `application.yml` 中添加以下配置：

```yaml
# 自定义JWT配置
custom:
  jwt:
    enabled: true  # 是否启用自定义JWT功能
    secret: your-secret-key-here-must-be-at-least-256-bits-long-for-security
    expiration: 3600  # 访问令牌过期时间（秒）
    refresh-expiration: 86400  # 刷新令牌过期时间（秒）
```

### 2. 配置说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `custom.jwt.enabled` | `false` | 是否启用自定义JWT功能 |
| `custom.jwt.secret` | `your-secret-key-here-must-be-at-least-256-bits` | JWT签名密钥 |
| `custom.jwt.expiration` | `3600` | 访问令牌过期时间（秒） |
| `custom.jwt.refresh-expiration` | `86400` | 刷新令牌过期时间（秒） |

## API接口

### 1. 用户登录

**接口地址：** `POST /api/custom/jwt/login`

**请求体：**
```json
{
    "username": "admin",
    "password": "123456"
}
```

**响应：**
```json
{
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "Bearer",
    "expires_in": 3600,
    "refresh_expires_in": 86400,
    "scope": "read write",
    "jti": "uuid-here",
    "user_info": {
        "user_id": 1,
        "username": "admin",
        "real_name": "管理员",
        "email": "admin@example.com",
        "phone": "13800138000",
        "status": 1
    }
}
```

### 2. 刷新JWT令牌

**接口地址：** `POST /api/custom/jwt/refresh`

**请求体：**
```json
{
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**响应：**
```json
{
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "Bearer",
    "expires_in": 3600,
    "scope": "read write",
    "jti": "uuid-here"
}
```

### 3. 验证JWT令牌

**接口地址：** `POST /api/custom/jwt/validate`

**请求体：**
```json
{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**响应：**
```json
{
    "username": "admin",
    "user_id": 1,
    "authorities": ["ROLE_ADMIN", "ROLE_USER", "PERMISSION_user:read"],
    "jti": "uuid-here",
    "token_type": "access_token"
}
```

### 4. 撤销JWT令牌

**接口地址：** `POST /api/custom/jwt/revoke`

**请求体：**
```json
{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**响应：**
```json
{
    "message": "Token revoked successfully"
}
```

### 5. 获取用户信息

**接口地址：** `GET /api/custom/jwt/userinfo`

**请求头：**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**响应：**
```json
{
    "username": "admin",
    "user_id": 1,
    "authorities": ["ROLE_ADMIN", "ROLE_USER", "PERMISSION_user:read"],
    "jti": "uuid-here",
    "token_type": "access_token"
}
```

## 使用示例

### 1. 客户端登录

```bash
curl -X POST http://localhost:9000/api/custom/jwt/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456"
  }'
```

### 2. 使用访问令牌

```bash
curl -X GET http://localhost:9000/api/custom/jwt/userinfo \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 3. 刷新令牌

```bash
curl -X POST http://localhost:9000/api/custom/jwt/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refresh_token": "YOUR_REFRESH_TOKEN"
  }'
```

## 架构设计

### 目录结构

```
custom/
├── config/
│   └── CustomJwtSecurityConfig.java      # 自定义JWT安全配置
├── controller/
│   └── CustomJwtController.java          # JWT控制器
├── filter/
│   └── CustomJwtAuthenticationFilter.java # JWT认证过滤器
├── service/
│   ├── CustomJwtAuthenticationService.java # JWT认证服务
│   └── CustomJwtUserDetailsService.java   # 自定义用户详情服务
├── util/
│   └── CustomJwtUtil.java                # JWT工具类
└── README.md                             # 说明文档
```

### 组件关系

```
CustomJwtController
    ↓
CustomJwtAuthenticationService
    ↓
CustomJwtUserDetailsService (查询用户信息和权限)
    ↓
CustomJwtUtil (生成JWT令牌)
    ↓
CustomJwtAuthenticationFilter (验证JWT令牌)
    ↓
CustomJwtSecurityConfig (安全配置)
```

## 用户详情服务

### CustomJwtUserDetailsService

专门为自定义JWT功能提供的用户详情服务，具有以下特点：

1. **条件注入**：只有当 `custom.jwt.enabled=true` 时才注入
2. **用户查询**：支持根据用户名查询用户信息
3. **权限查询**：支持查询用户权限和角色
4. **状态检查**：自动检查用户状态（启用/禁用）
5. **权限合并**：自动合并权限和角色信息

### 权限信息

JWT令牌中会包含以下权限信息：

- **authorities**：Spring Security格式的权限列表（与OAuth2保持一致）
  - 包含 `ROLE_` 前缀的角色
  - 包含 `PERMISSION_` 前缀的权限

## 与OAuth2的一致性

### 权限格式

自定义JWT功能与OAuth2保持完全一致的权限格式：

1. **authorities字段**：所有权限信息都统一放在 `authorities` 字段中
2. **权限前缀**：
   - 角色：`ROLE_ADMIN`、`ROLE_USER`
   - 权限：`PERMISSION_user:read`、`PERMISSION_user:write`
3. **JWT声明**：与OAuth2的 `jwtCustomizer` 保持一致的声明格式

### 兼容性

- ✅ 权限格式与OAuth2完全一致
- ✅ JWT声明结构与OAuth2兼容
- ✅ 可以无缝替换OAuth2 JWT功能
- ✅ 不影响现有OAuth2功能

## 条件注入

所有自定义JWT组件都使用 `@ConditionalOnProperty` 注解进行条件注入：

```java
@ConditionalOnProperty(name = "custom.jwt.enabled", havingValue = "true", matchIfMissing = false)
```

这意味着：
- 只有当 `custom.jwt.enabled=true` 时，这些组件才会被注入
- 当 `custom.jwt.enabled=false` 或未配置时，这些组件不会被注入
- 这样可以避免在不使用时影响现有功能

## 安全考虑

1. **密钥安全**：确保JWT密钥足够长且安全
2. **HTTPS**：生产环境建议使用HTTPS
3. **令牌过期**：合理设置令牌过期时间
4. **令牌撤销**：实现令牌撤销机制
5. **权限验证**：确保权限信息正确嵌入和验证
6. **用户状态**：自动检查用户状态，禁用用户无法登录

## 注意事项

1. 自定义JWT功能与现有OAuth2功能完全隔离
2. 可以通过配置开关控制是否启用
3. 所有组件都支持条件注入
4. 建议在生产环境中使用强密钥
5. 定期轮换JWT密钥
6. 用户状态检查确保安全性
7. 权限格式与OAuth2保持一致

## 故障排除

### 1. 组件未注入

检查配置：
```yaml
custom:
  jwt:
    enabled: true
```

### 2. 令牌验证失败

检查密钥配置和令牌格式。

### 3. 权限信息缺失

确保用户有正确的权限配置。

### 4. 用户状态问题

检查用户状态是否为启用状态（status=1）。

## 扩展功能

可以根据需要扩展以下功能：
- Redis黑名单支持
- 令牌轮换
- 多租户支持
- 审计日志
- 监控指标
- 用户会话管理 