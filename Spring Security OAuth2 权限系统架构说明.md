# Spring Security OAuth2 权限系统架构说明

## 📋 目录
- [系统概述](#系统概述)
- [Spring Security OAuth2基础](#spring-security-oauth2基础)
- [项目实现架构](#项目实现架构)
- [核心组件详解](#核心组件详解)
- [配置清单](#配置清单)
- [实现细节](#实现细节)
- [常见问题与解答](#常见问题与解答)
- [性能分析](#性能分析)
- [最佳实践](#最佳实践)

## 🎯 系统概述

Spring Security OAuth2是Spring Security提供的OAuth2授权服务器实现，支持完整的OAuth2和OpenID Connect协议。本文档基于Spring Security OAuth2，结合具体项目实现来讲解权限系统的架构设计。

### 核心特性
- **OAuth2授权服务器** - 基于Spring Security OAuth2实现
- **JWT令牌** - 自包含的访问令牌，包含权限信息
- **RBAC权限模型** - 用户-角色-权限三层结构
- **前端权限控制** - 组件级权限验证
- **数据库持久化** - 存储所有权限数据

## 🔧 Spring Security OAuth2基础

### 默认实现

Spring Security OAuth2提供了以下默认实现：

#### 1. 客户端管理
- **默认实现**: 
  - `InMemoryRegisteredClientRepository` - 内存存储，重启后丢失
  - `JdbcRegisteredClientRepository` - JDBC数据库存储
- **存储方式**: 内存或JDBC数据库
- **特点**: 
  - 内存版本：快速开发，无需数据库
  - JDBC版本：使用Spring提供的JDBC模板，需要按约定创建表结构
- **适用场景**: 
  - 内存版本：开发测试，快速原型
  - JDBC版本：使用JDBC的项目，生产环境

#### 2. 授权记录存储
- **默认实现**: 
  - `InMemoryOAuth2AuthorizationService` - 内存存储，重启后丢失
  - `JdbcOAuth2AuthorizationService` - JDBC数据库存储
- **存储方式**: 内存或JDBC数据库
- **特点**: 
  - 内存版本：快速开发，无需数据库
  - JDBC版本：使用Spring提供的JDBC模板，需要按约定创建表结构
- **适用场景**: 
  - 内存版本：开发测试，快速原型
  - JDBC版本：使用JDBC的项目，生产环境

#### 3. 令牌生成
- **默认实现**: 随机字符串令牌
- **特点**: 无法解析内容，需要额外的令牌信息端点
- **适用场景**: 简单应用，不需要JWT的场景

### 自定义实现场景

当需要以下功能时，需要自定义实现：

1. **生产环境持久化** - 替换内存存储
2. **JWT令牌支持** - 替换随机字符串令牌
3. **自定义ORM框架** - 替换JDBC实现
4. **权限信息嵌入** - 在令牌中包含权限信息

### 自定义实现指导

Spring Security OAuth2提供了完整的接口定义，开发者可以根据需要实现自定义版本：

#### 1. 自定义RegisteredClientRepository
- **接口**: `RegisteredClientRepository`
- **默认实现**: `InMemoryRegisteredClientRepository`、`JdbcRegisteredClientRepository`
- **自定义实现**: 继承`RegisteredClientRepository`接口，实现`findById`、`findByClientId`、`save`等方法
- **参考实现**: 项目中的`OAuth2RegisteredClientService`

#### 2. 自定义OAuth2AuthorizationService
- **接口**: `OAuth2AuthorizationService`
- **默认实现**: `InMemoryOAuth2AuthorizationService`、`JdbcOAuth2AuthorizationService`
- **自定义实现**: 继承`OAuth2AuthorizationService`接口，实现`save`、`remove`、`findById`、`findByToken`等方法
- **参考实现**: 项目中的`MyBatisOAuth2AuthorizationService`

#### 3. 自定义UserDetailsService
- **接口**: `UserDetailsService`
- **默认实现**: 无
- **自定义实现**: 实现`UserDetailsService`接口，实现`loadUserByUsername`方法
- **参考实现**: 项目中的`UserDetailsServiceImpl`

## 🏗️ 项目实现架构

### 系统模块
```
security-system/
├── security-sso/          # OAuth2授权服务器
├── security-admin/        # 权限管理后端
├── security-core/         # 核心实体和服务
└── security-admin/ui/     # 前端管理界面
```

### 认证流程
1. 用户登录 → OAuth2授权服务器
2. 验证用户身份 → UserDetailsService
3. 生成JWT令牌 → 包含用户权限信息
4. 前端解析JWT → 获取权限列表
5. 组件级权限控制 → 显示/隐藏功能

## 🔧 核心组件详解

### 1. OAuth2授权服务器 (security-sso)

#### Spring Security OAuth2核心配置
- `SecurityConfig.java` - Spring Security配置
- `JwtConfig.java` - JWT自定义配置
- `JwkService.java` - JWK密钥管理

#### 自定义实现的服务
- `UserDetailsServiceImpl.java` - 用户认证服务
- `OAuth2RegisteredClientService.java` - 客户端管理（MyBatis实现）
- `MyBatisOAuth2AuthorizationService.java` - 授权记录持久化（MyBatis实现）

### 2. 资源服务器 (security-admin)

#### 控制器
- `UserController.java` - 用户管理
- `RoleController.java` - 角色管理  
- `PermissionController.java` - 权限管理
- `DashboardController.java` - 仪表盘

#### 配置
- `SecurityConfig.java` - 资源服务器配置
- `WebConfig.java` - Web配置

### 3. 前端权限控制 (security-admin/ui)

#### 核心组件
- `Permission.tsx` - 权限控制组件
- `ProtectedRoute.tsx` - 路由保护组件
- `permissionUtil.ts` - 权限工具类

## 📋 配置清单

### OAuth2授权服务器过滤器链配置

```java
@Bean
@Order(1)
public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
    // 1. 应用OAuth2默认安全配置
    OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

    // 2. 配置客户端注册仓库
    http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .registeredClientRepository(registeredClientService)
            .oidc(Customizer.withDefaults()); // 启用OpenID Connect

    // 3. 配置异常处理
    http.exceptionHandling((exceptions) -> exceptions
            .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"), // 未认证时跳转登录页
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML))) // 只对HTML请求生效

    // 4. 配置资源服务器（用于验证自己的令牌）
    .oauth2ResourceServer((resourceServer) -> resourceServer
            .jwt(Customizer.withDefaults())); // 使用JWT验证

    return http.build();
}
```

### 完整的Bean配置清单

| Bean名称 | Spring Security默认实现 | 项目自定义实现 | OAuth2作用 | 是否必须注入 | 注入方式 | 调用时机 | 自动使用位置 | 说明 |
|----------|----------------------|---------------|------------|-------------|----------|----------|-------------|------|
| `RegisteredClientRepository` | InMemoryRegisteredClientRepository / JdbcRegisteredClientRepository | `OAuth2RegisteredClientService` | 客户端管理 | ✅ 必须 | `@Service` | OAuth2端点调用时 | OAuth2自动使用 | 默认提供内存和JDBC两种实现，如需使用MyBatis等ORM框架需继承RegisteredClientRepository实现，参考项目OAuth2RegisteredClientService |
| `UserDetailsService` | 无 | `UserDetailsServiceImpl` | 用户认证 | ✅ 必须 | `@Service` | 用户登录时 | AuthenticationProvider自动使用 | 必须自定义，因为每个系统的用户模型都不同 |
| `PasswordEncoder` | BCryptPasswordEncoder | BCryptPasswordEncoder | 密码加密 | ✅ 必须 | `@Bean` | 密码验证时 | AuthenticationProvider自动使用 | 使用Spring Security默认实现 |
| `AuthenticationProvider` | DaoAuthenticationProvider | 自定义配置 | 认证逻辑 | ✅ 必须 | `@Bean` | 用户认证时 | AuthenticationManager自动使用 | 连接自定义UserDetailsService |
| `AuthenticationManager` | 自动配置 | 自定义配置 | 认证管理器 | ✅ 必须 | `@Bean` | 登录流程时 | OAuth2自动使用 | 管理认证流程 |
| `OAuth2AuthorizationService` | InMemoryOAuth2AuthorizationService / JdbcOAuth2AuthorizationService | `MyBatisOAuth2AuthorizationService` | 授权记录持久化 | ✅ 必须 | `@Bean` | 授权操作时 | OAuth2自动使用 | 默认提供内存和JDBC两种实现，如需使用MyBatis等ORM框架需继承OAuth2AuthorizationService实现，参考项目MyBatisOAuth2AuthorizationService |
| `SecurityFilterChain` | 无 | 两个过滤器链 | 安全配置 | ✅ 必须 | `@Bean` | 请求处理时 | Spring Security自动使用 | 定义安全规则 |
| `JWKSource` | 无 | 通过`JwkService`提供 | JWT签名密钥 | ❌ 非必须 | `@Bean` | JWT签名时 | JwtEncoder自动使用 | 需要JWT时注入，否则使用默认 |
| `JwtEncoder` | 无 | NimbusJwtEncoder | JWT编码 | ❌ 非必须 | `@Bean` | 生成JWT时 | OAuth2TokenGenerator自动使用 | 需要JWT时注入，否则使用随机字符串 |
| `JwtDecoder` | 无 | NimbusJwtDecoder | JWT解码 | ❌ 非必须 | `@Bean` | 验证JWT时 | OAuth2ResourceServer自动使用 | 需要JWT时注入，否则无法验证JWT |
| `OAuth2TokenCustomizer` | 无 | `JwtConfig.jwtCustomizer()` | JWT内容自定义 | ❌ 非必须 | `@Bean` | 生成JWT时 | JwtGenerator自动使用 | 添加权限信息到JWT中 |
| `OAuth2TokenGenerator` | 有默认实现 | 自定义JWT生成器 | 令牌生成 | ❌ 非必须 | `@Bean` | 生成令牌时 | OAuth2自动使用 | 需要JWT时注入，否则使用默认随机字符串 |
| `AuthorizationServerSettings` | 有默认实现 | 自定义配置 | OAuth2服务器设置 | ❌ 非必须 | `@Bean` | 服务器启动时 | OAuth2自动使用 | 需要自定义设置时注入 |

### 注入决策指南

#### 什么时候使用Spring Security默认实现？
- **开发阶段**：快速原型开发
- **简单场景**：只需要基本的OAuth2功能
- **内存存储**：可以接受重启后数据丢失
- **随机令牌**：不需要JWT，使用随机字符串令牌
- **JDBC存储**：使用Spring提供的JdbcRegisteredClientRepository

#### 什么时候需要自定义实现？
- **生产环境**：需要数据持久化
- **JWT需求**：需要自包含的令牌
- **权限控制**：需要在令牌中包含权限信息
- **自定义ORM**：使用MyBatis、Hibernate等非JDBC的ORM框架
- **自定义用户模型**：用户表结构与默认不同
- **自定义存储**：使用Redis、MongoDB等非关系型数据库

#### 本项目中的自定义实现

**必须自定义**：
- `RegisteredClientRepository` - 使用MyBatis实现，替换默认JDBC
- `UserDetailsService` - 自定义用户模型和权限加载
- `OAuth2AuthorizationService` - 使用MyBatis实现，替换默认内存存储

**可选自定义**：
- `JWKSource` - 提供JWT签名密钥
- `JwtEncoder/Decoder` - 支持JWT令牌
- `OAuth2TokenCustomizer` - 在JWT中添加权限信息

### 注入后自动使用机制

#### 1. Spring Security OAuth2自动发现机制
```java
// Spring Security OAuth2会自动查找这些Bean
@Bean
public OAuth2TokenGenerator<?> tokenGenerator(JwtEncoder jwtEncoder, // ← 自动注入
        OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer) {   // ← 自动注入
    // OAuth2会自动使用这个Bean
}
```

#### 2. 依赖注入链
```
UserDetailsService → AuthenticationProvider → AuthenticationManager → OAuth2
JWKSource → JwtEncoder → OAuth2TokenGenerator → OAuth2
OAuth2TokenCustomizer → JwtGenerator → OAuth2TokenGenerator → OAuth2
```

#### 3. 自动使用位置
- **OAuth2授权端点** - 自动使用 `RegisteredClientRepository`
- **用户认证** - 自动使用 `UserDetailsService`
- **JWT生成** - 自动使用 `JwtEncoder` + `OAuth2TokenCustomizer`
- **JWT验证** - 自动使用 `JwtDecoder`
- **授权记录** - 自动使用 `OAuth2AuthorizationService`

## ❓ 常见问题与解答

### Q1: 为什么需要自定义RegisteredClientRepository？

**A1**: 
- **Spring Security默认实现**: `JdbcRegisteredClientRepository`，只支持JDBC
- **项目自定义原因**: 使用MyBatis作为ORM框架，需要自定义实现

**对比**：
- ✅ **Spring Security默认实现**：使用JDBC，需要按约定创建表结构
- ✅ **项目自定义实现**：使用MyBatis，可以灵活控制数据库操作

### Q2: 为什么UserDetailsService必须自定义？

**A2**: 
- Spring Security OAuth2没有提供默认的UserDetailsService实现
- 每个系统的用户模型都不同
- 权限模型可能不同（RBAC、ABAC等）

```java
// 必须自定义，因为每个系统的用户模型都不同
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    // 自定义用户、角色、权限模型
}
```

### Q3: 为什么需要自定义OAuth2AuthorizationService？

**A3**:
- **Spring Security默认实现**: `InMemoryOAuth2AuthorizationService`，内存存储，重启后丢失
- **项目自定义原因**: 生产环境需要持久化存储，使用MyBatis实现

### Q4: OAuth2TokenGenerator有默认实现吗？

**A4**:
- **Spring Security默认实现**: 生成随机字符串令牌，无法解析内容
- **项目自定义实现**: 生成JWT令牌，包含权限信息，可直接解析

**对比**：
```java
// Spring Security默认实现 - 随机字符串
@Bean
public OAuth2TokenGenerator<?> tokenGenerator() {
    return new DelegatingOAuth2TokenGenerator(
        new OAuth2AccessTokenGenerator(), // 生成随机字符串
        new OAuth2RefreshTokenGenerator()
    );
}

// 项目自定义JWT实现
@Bean
public OAuth2TokenGenerator<?> tokenGenerator(JwtEncoder jwtEncoder,
        OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer) {
    // JWT包含用户信息和权限，可以直接解析
    JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
    jwtGenerator.setJwtCustomizer(jwtCustomizer);
    return new DelegatingOAuth2TokenGenerator(jwtGenerator, refreshTokenGenerator);
}
```

### Q5: 为什么需要两个SecurityFilterChain？

**A5**:

#### 第一个：OAuth2授权服务器过滤器链
```java
@Bean
@Order(1)
public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) {
    // 处理OAuth2授权端点
    // /oauth2/authorize - 授权端点
    // /oauth2/token - 令牌端点  
    // /oauth2/jwks - JWK端点
}
```

#### 第二个：默认安全过滤器链
```java
@Bean
@Order(2)
public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) {
    // 处理其他所有请求
    // 登录页面、用户信息等
}
```

**为什么需要两个？**

1. **职责分离**：
   - 第一个：专门处理OAuth2协议端点
   - 第二个：处理应用的其他功能

2. **安全策略不同**：
   - OAuth2端点：需要特殊的认证方式
   - 普通端点：使用表单登录等

3. **优先级控制**：
   - `@Order(1)`：OAuth2端点优先匹配
   - `@Order(2)`：其他请求后匹配

## 🔍 实现细节

### 1. JWT权限信息添加

```java
@Bean
public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
    return context -> {
        // 从UserDetails中提取权限
        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();
        List<String> authoritiesList = authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());
        
        // 添加到JWT声明中
        context.getClaims().claim("authorities", authoritiesList);
    };
}
```

**优势**：
- ✅ 权限信息自包含，无需额外查询
- ✅ 前端可直接解析获取权限
- ✅ 性能优秀，零网络请求

### 2. 前端权限控制

```typescript
// 权限检查
static hasPermission(permissionCode: string): boolean {
    const authorities = this.getAuthorities();
    return authorities.includes(permissionCode);
}

// 组件级权限控制
<Permission code="USER_CREATE">
    <Button>创建用户</Button>
</Permission>
```

### 3. 数据库设计

#### 核心表结构
- `sys_user` - 用户表
- `sys_role` - 角色表  
- `sys_permission` - 权限表
- `sys_user_role` - 用户角色关联表
- `sys_role_permission` - 角色权限关联表

#### OAuth2相关表
- `oauth2_registered_client` - 客户端注册表
- `oauth2_authorization` - 授权记录表
- `oauth2_jwk` - JWK密钥表

## ⚡ 性能分析

### 性能对比

| 方案 | 权限检查耗时 | 网络请求数 | 用户体验 |
|------|-------------|-----------|----------|
| JWT包含权限 | ~1ms | 0 | 极快 |
| API调用方案 | ~50-200ms | 每次检查1次 | 较慢 |

### 性能优势

1. **本地解析**: JWT解析是纯本地操作，耗时微秒级
2. **无网络开销**: 不需要额外的HTTP请求
3. **缓存机制**: 浏览器可以缓存JWT
4. **批量检查**: 一次解析，多次使用

## 🎯 最佳实践

### 1. 安全最佳实践

- ✅ 使用HTTPS传输JWT
- ✅ 设置合理的JWT过期时间
- ✅ 实现JWT刷新机制
- ✅ 定期轮换JWK密钥

### 2. 性能最佳实践

- ✅ 合理设置权限数量，避免JWT过大
- ✅ 实现权限缓存机制
- ✅ 使用CDN加速静态资源

### 3. 开发最佳实践

- ✅ 统一的权限命名规范
- ✅ 完善的权限文档
- ✅ 权限变更审计日志
- ✅ 自动化权限测试

## 🔧 部署说明

### 环境要求
- JDK 8+
- MySQL 5.7+
- Node.js 14+ (前端)

### 启动顺序
1. 启动MySQL数据库
2. 启动OAuth2授权服务器 (security-sso)
3. 启动权限管理后端 (security-admin)
4. 启动前端管理界面 (security-admin/ui)

### 配置说明
- 数据库连接配置在 `application.yml`
- OAuth2客户端配置在数据库 `oauth2_registered_client` 表
- 初始管理员账号: admin/admin123

## 📚 相关文档

- [Spring Security OAuth2官方文档](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)
- [JWT官方文档](https://jwt.io/)
- [Spring Security官方文档](https://docs.spring.io/spring-security/reference/)

## 🤝 贡献指南

欢迎提交Issue和Pull Request来改进这个权限系统！

---

**注意**: 本系统基于Spring Security OAuth2实现，结合具体项目展示了如何自定义Spring Security OAuth2的默认实现。如有问题，请查看日志或联系开发团队。