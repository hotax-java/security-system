# SSO认证授权服务

## 功能特性

### 核心功能
- ✅ OAuth2授权服务器
- ✅ JWT令牌生成和验证
- ✅ 用户认证和授权
- ✅ 客户端注册管理
- ✅ JWK密钥管理

### 新增功能
- ✅ Redis分布式缓存
- ✅ 令牌黑名单管理
- ✅ 黑名单API接口

## 技术栈

- **Spring Boot 2.7.x**
- **Spring Security OAuth2**
- **MyBatis-Plus**
- **MySQL**
- **Redis**
- **JWT**

## 配置说明

### Redis配置
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0
    timeout: 5000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

### 缓存配置
- **客户端信息缓存**: 1小时
- **黑名单缓存**: 1天
- **默认缓存**: 30分钟

## API接口

### 黑名单管理API

#### 1. 添加令牌到黑名单
```http
POST /api/token-blacklist/add
Content-Type: application/json

{
  "jti": "jwt-id-123",
  "expirationTime": 3600
}
```

#### 2. 检查令牌黑名单状态
```http
GET /api/token-blacklist/check/{jti}
```

#### 3. 从黑名单移除令牌
```http
DELETE /api/token-blacklist/remove/{jti}
```

#### 4. 获取黑名单统计
```http
GET /api/token-blacklist/stats
```

#### 5. 清理过期条目
```http
POST /api/token-blacklist/cleanup
```

## 使用场景

### 令牌撤销场景
1. **用户主动登出**: 将当前令牌加入黑名单
2. **密码修改**: 撤销用户所有活跃令牌
3. **权限变更**: 撤销相关用户的令牌
4. **安全事件**: 撤销可疑用户的令牌

### 缓存优化
1. **客户端信息**: 减少数据库查询
2. **用户权限**: 提高权限验证性能
3. **黑名单检查**: 快速验证令牌状态

## 部署说明

### 环境要求
- JDK 11+
- MySQL 8.0+
- Redis 6.0+

### 启动命令
```bash
# 开发环境
mvn spring-boot:run -pl security-sso

# 生产环境
java -jar security-sso-1.0.0.jar --spring.profiles.active=prod
```

### 环境变量
```bash
# 数据库配置
export DB_URL=jdbc:mysql://localhost:3306/security_system
export DB_USERNAME=root
export DB_PASSWORD=password

# Redis配置
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=
export REDIS_DATABASE=0
```

## 监控和日志

### 健康检查
- 应用状态: `GET /actuator/health`
- 数据库连接: 自动检查
- Redis连接: 自动检查

### 日志配置
- 应用日志: `logs/security-sso.log`
- 错误日志: `logs/security-sso-error.log`
- 访问日志: `logs/security-sso-access.log`

## 安全考虑

### 黑名单安全
- 令牌ID (JTI) 唯一性验证
- 过期时间自动清理
- 黑名单大小监控

### 缓存安全
- Redis访问控制
- 敏感数据不缓存
- 缓存过期策略

## 故障排除

### 常见问题
1. **Redis连接失败**: 检查Redis服务状态和配置
2. **黑名单API无响应**: 检查安全配置和权限
3. **缓存不生效**: 检查Redis连接和序列化配置

### 调试命令
```bash
# 检查Redis连接
redis-cli ping

# 查看黑名单大小
redis-cli keys "token:blacklist:*" | wc -l

# 检查应用日志
tail -f logs/security-sso.log
``` 