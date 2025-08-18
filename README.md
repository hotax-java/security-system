# Security System - 企业级认证授权系统

基于Spring Boot + Spring Security + OAuth2的模块化认证授权系统，提供完整的用户认证、权限管理和单点登录解决方案。

## 项目结构

```
security-system/
├── security-core/           # 核心模块
│   ├── src/main/java/      # 核心业务逻辑
│   │   └── com/webapp/security/core/
│   │       ├── entity/     # 实体类 (用户、角色、权限)
│   │       ├── mapper/     # MyBatis映射器
│   │       └── service/    # 核心服务实现
│   └── src/main/resources/ # 配置文件
├── security-sso/           # SSO认证服务
│   ├── src/main/java/      # 认证授权服务
│   │   └── com/webapp/security/sso/
│   │       ├── config/     # OAuth2和Security配置
│   │       ├── controller/ # 认证API控制器
│   │       ├── service/    # 认证服务实现
│   │       └── mapper/     # OAuth2数据映射
│   ├── src/main/resources/ # 配置和日志
│   └── start.sh           # 启动脚本
├── security-admin/         # 后台管理系统
│   ├── src/main/java/      # 管理后端
│   │   └── com/webapp/security/admin/
│   │       ├── config/     # 安全配置
│   │       └── controller/ # 管理API
│   ├── src/main/resources/ # 后端配置
│   └── ui/                # React前端
│       ├── src/           # 前端源码
│       ├── public/        # 静态资源
│       └── package.json   # 前端依赖
└── pom.xml                # 主项目配置
```

## 核心功能

### 🔐 认证授权
- **OAuth2认证服务器**: 支持授权码、客户端凭证、刷新令牌等标准流程
- **JWT令牌**: RSA签名的JWT令牌，包含用户信息和权限数据
- **单点登录(SSO)**: 统一认证入口，支持多应用集成
- **令牌管理**: 自动刷新、安全存储、过期处理

### 👥 权限管理
- **RBAC模型**: 用户-角色-权限三级权限控制
- **细粒度权限**: 支持菜单权限(perm_type=1)、按钮权限(perm_type=2)、接口权限(perm_type=3)
- **动态权限**: 运行时权限验证和动态加载，基于JWT Token解析用户权限
- **权限继承**: 角色权限继承和组合，支持多角色分配
- **权限缓存**: 前端权限缓存机制，避免重复解析JWT Token
- **权限审计**: 完整的权限操作和访问日志记录

### 🔑 权限定义与使用

#### 权限类型说明
- **菜单权限 (perm_type = 1)**: 控制左侧菜单栏的显示，如"用户管理"、"角色管理"
- **按钮权限 (perm_type = 2)**: 控制页面按钮的显示和操作，如"新增"、"删除"、"编辑"
- **接口权限 (perm_type = 3)**: 控制API接口的访问权限，如"GET /api/users"、"POST /api/roles"

#### 权限层级结构
权限支持树形结构，通过`parent_id`字段建立父子关系：
```
系统管理 (parent_id = null)
├── 用户管理 (parent_id = 系统管理ID)
│   ├── 用户查询 (parent_id = 用户管理ID)
│   ├── 用户新增 (parent_id = 用户管理ID)
│   └── 用户编辑 (parent_id = 用户管理ID)
└── 角色管理 (parent_id = 系统管理ID)
    ├── 角色查询 (parent_id = 角色管理ID)
    └── 角色新增 (parent_id = 角色管理ID)
```

#### 权限验证流程
1. **JWT Token解析**: 从JWT Token中解析用户权限列表（存储在authorities字段）
2. **权限缓存**: 前端使用PermissionUtil缓存权限判断结果，避免重复解析JWT
3. **权限匹配**: 检查用户权限列表中是否包含指定权限编码
4. **动态过滤**: 根据权限动态过滤菜单和按钮，无权限的菜单项不显示

#### 权限分配流程
1. **创建权限**: 在`sys_permission`表中创建权限记录，设置`perm_code`、`perm_name`、`perm_type`等字段
2. **分配角色**: 在`sys_role_permission`表中建立角色与权限的关联关系
3. **分配用户**: 在`sys_user_role`表中建立用户与角色的关联关系
4. **权限生效**: 用户登录后，系统从JWT Token中获取权限列表，前端根据权限动态显示菜单和按钮

### 🎛️ 管理界面
- **用户管理**: 用户CRUD、状态管理、角色分配
- **角色管理**: 角色定义、权限分配、层级管理
- **权限管理**: 权限树形结构、动态配置
- **现代化UI**: 基于Ant Design 5.x的响应式界面

### 🔒 安全特性
- **密码加密**: BCrypt加密存储
- **会话管理**: 无状态JWT会话
- **CORS支持**: 跨域资源共享配置
- **安全审计**: 操作日志和安全事件记录

## 快速开始

### 环境准备
- **JDK**: 1.8+
- **Maven**: 3.6+
- **MySQL**: 5.7+ / 8.0+
- **Node.js**: 16+
- **npm**: 8+

### 1. 数据库初始化

创建数据库并执行初始化脚本：

```sql
-- 创建数据库
CREATE DATABASE security_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE security_system;

-- 用户表
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    real_name VARCHAR(50) COMMENT '真实姓名',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    phone VARCHAR(20) UNIQUE COMMENT '手机号',
    avatar VARCHAR(200) COMMENT '头像URL',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 角色表
CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    description VARCHAR(200) COMMENT '角色描述',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 权限表
CREATE TABLE sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    perm_name VARCHAR(50) NOT NULL COMMENT '权限名称',
    perm_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    perm_type TINYINT NOT NULL COMMENT '权限类型：1-菜单，2-按钮',
    parent_id BIGINT DEFAULT 0 COMMENT '父权限ID',
    path VARCHAR(200) COMMENT '路径',
    icon VARCHAR(50) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 用户角色关联表
CREATE TABLE sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id)
);

-- 角色权限关联表
CREATE TABLE sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_permission (role_id, permission_id)
);

-- OAuth2相关表
CREATE TABLE oauth2_registered_client (
    id VARCHAR(100) NOT NULL PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL,
    client_id_issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret VARCHAR(200) DEFAULT NULL,
    client_secret_expires_at TIMESTAMP DEFAULT NULL,
    client_name VARCHAR(200) NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types VARCHAR(1000) NOT NULL,
    redirect_uris VARCHAR(1000) DEFAULT NULL,
    scopes VARCHAR(1000) NOT NULL,
    client_settings VARCHAR(2000) NOT NULL,
    token_settings VARCHAR(2000) NOT NULL
);

-- 插入测试数据
INSERT INTO sys_user (username, password, real_name, email, phone) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '系统管理员', 'admin@example.com', '13800138000'),
('user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '普通用户', 'user@example.com', '13800138001');

INSERT INTO sys_role (role_name, role_code, description) VALUES
('超级管理员', 'SUPER_ADMIN', '系统超级管理员，拥有所有权限'),
('普通用户', 'USER', '普通用户角色，基础权限');

INSERT INTO sys_permission (perm_name, perm_code, perm_type, parent_id, path, sort_order) VALUES
('系统管理', 'system', 1, 0, '/system', 1),
('用户管理', 'user', 1, 1, '/system/user', 1),
('用户查看', 'user:view', 2, 2, '', 1),
('用户添加', 'user:add', 2, 2, '', 2),
('用户编辑', 'user:edit', 2, 2, '', 3),
('用户删除', 'user:delete', 2, 2, '', 4),
('角色管理', 'role', 1, 1, '/system/role', 2),
('角色查看', 'role:view', 2, 7, '', 1),
('角色添加', 'role:add', 2, 7, '', 2),
('角色编辑', 'role:edit', 2, 7, '', 3),
('角色删除', 'role:delete', 2, 7, '', 4),
('权限分配', 'role:permission', 2, 7, '', 5);

-- 分配用户角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1), (2, 2);

-- 分配角色权限
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12),
(2, 2), (2, 3), (2, 7), (2, 8);
```

### 2. 服务启动

#### 方式一：使用Maven启动

```bash
# 1. 启动SSO认证服务 (端口: 9001)
cd security-sso
mvn spring-boot:run

# 2. 启动管理后端服务 (端口: 8080)
cd security-admin
mvn spring-boot:run

# 3. 启动前端界面 (端口: 3000)
cd security-admin/ui
npm install
npm start
```

#### 方式二：使用启动脚本

```bash
# Linux/Mac
cd security-sso
chmod +x start.sh
./start.sh

# Windows
cd security-sso
start.bat
```

### 3. 访问系统

- **前端管理界面**: http://localhost:3000
- **SSO认证服务**: http://localhost:9001
- **管理后端API**: http://localhost:8080

**默认账户**:
- 管理员: `admin` / `123456`
- 普通用户: `user` / `123456`

## API文档

### SSO认证服务 (http://localhost:9001)

#### 认证接口
```http
POST /api/login          # 用户登录
POST /api/logout         # 用户登出
POST /api/refresh        # 刷新令牌
GET  /api/user/current   # 获取当前用户信息
POST /api/validate       # 令牌验证
```

#### OAuth2标准端点
```http
GET  /oauth2/authorize        # 授权端点
POST /oauth2/token           # 令牌端点
POST /oauth2/revoke          # 撤销端点
GET  /.well-known/oauth-authorization-server  # 服务发现
GET  /.well-known/jwks.json  # 公钥端点
```

### 管理后端API (http://localhost:8080)

#### 用户管理
```http
GET    /api/users            # 用户列表 [user:view]
POST   /api/users            # 创建用户 [user:add]
PUT    /api/users/{id}       # 更新用户 [user:edit]
DELETE /api/users/{id}       # 删除用户 [user:delete]
GET    /api/users/me         # 当前用户信息
```

#### 角色管理
```http
GET    /api/roles            # 角色列表 [role:view]
POST   /api/roles            # 创建角色 [role:add]
PUT    /api/roles/{id}       # 更新角色 [role:edit]
DELETE /api/roles/{id}       # 删除角色 [role:delete]
POST   /api/roles/{id}/permissions  # 分配权限 [role:permission]
```

## 技术架构

### 后端技术栈
- **Spring Boot 2.7.5** - 应用框架
- **Spring Security 5.7.11** - 安全框架
- **OAuth2 Authorization Server 0.4.5** - OAuth2服务器
- **MyBatis-Plus 3.5.3** - ORM框架
- **MySQL 8.0** - 关系数据库
- **Druid 1.2.18** - 数据库连接池
- **JWT (JJWT) 0.11.5** - JWT令牌处理
- **Hutool 5.8.20** - Java工具库

### 前端技术栈
- **React 18.2** - 前端框架
- **TypeScript 4.9** - 类型系统
- **Ant Design 5.26** - UI组件库
- **React Router 6.8** - 路由管理
- **Axios 1.3** - HTTP客户端

### 架构特点
- **模块化设计**: 核心、认证、管理三层分离
- **标准化协议**: 遵循OAuth2和OpenID Connect标准
- **微服务友好**: 支持分布式部署和服务发现
- **高性能**: 无状态JWT令牌，支持水平扩展
- **安全可靠**: 多层安全防护，完整的审计日志

## 配置说明

### 数据库配置
```yaml
# security-sso/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/security_system
    username: root
    password: your_password
```

### 前端环境配置
```bash
# security-admin/ui/.env.development
REACT_APP_API_BASE_URL=http://localhost:8080
REACT_APP_AUTH_BASE_URL=http://localhost:9001
```

### OAuth2客户端配置
系统启动时会自动注册默认客户端，也可通过数据库手动配置。

## 部署指南

### 开发环境
1. 确保MySQL服务运行
2. 修改数据库连接配置
3. 按顺序启动各服务

### 生产环境
1. **安全配置**: 修改默认密码和密钥
2. **HTTPS**: 启用SSL证书
3. **数据库**: 使用生产级数据库配置
4. **监控**: 配置日志和监控系统
5. **备份**: 定期备份数据库和配置

## 扩展开发

### 添加新权限
1. 在`sys_permission`表中添加权限记录
2. 在角色管理界面分配权限
3. 在代码中使用`@PreAuthorize`注解

### 权限管理最佳实践
1. **权限命名规范**: 使用模块名_操作名的格式，如`USER_CREATE`、`ROLE_DELETE`
2. **权限层级设计**: 合理设计权限树形结构，通过`parent_id`字段建立父子关系
3. **权限缓存策略**: 前端使用PermissionUtil缓存权限判断结果，避免重复解析JWT Token
4. **权限数据管理**: 通过权限管理界面维护`sys_permission`表数据，确保权限编码的一致性

### 集成第三方应用
1. 注册OAuth2客户端
2. 配置回调地址和权限范围
3. 使用标准OAuth2流程集成

### 自定义认证方式
1. 实现`AuthenticationProvider`接口
2. 配置到Spring Security中
3. 添加相应的登录接口

## 常见问题

**Q: 忘记管理员密码怎么办？**
A: 可以直接在数据库中重置密码，使用BCrypt加密新密码。

**Q: 如何修改JWT过期时间？**
A: 在OAuth2配置中修改`accessTokenTimeToLive`和`refreshTokenTimeToLive`。

**Q: 前端如何处理令牌过期？**
A: 前端会自动检测401状态码并尝试刷新令牌，失败则跳转登录页。

**Q: 如何添加新的OAuth2客户端？**
A: 在`oauth2_registered_client`表中添加记录，或通过管理界面配置。

**Q: 权限系统是如何工作的？**
A: 系统采用RBAC模型，用户通过角色获得权限。权限分为菜单权限(perm_type=1)、按钮权限(perm_type=2)、接口权限(perm_type=3)。前端通过JWT Token解析用户权限，动态过滤菜单和按钮。

**Q: 如何实现动态菜单权限？**
A: 后端提供`/api/dashboard/menus`接口返回用户菜单权限，前端根据权限过滤菜单项。Dashboard菜单默认显示，其他菜单根据用户权限动态显示。

**Q: 权限缓存机制是什么？**
A: 前端使用PermissionUtil缓存权限判断结果，避免重复解析JWT Token。当Token变化时自动清除缓存，确保权限判断的准确性。

## 许可证

本项目采用 MIT 许可证，详见 [LICENSE](LICENSE) 文件。

## 贡献指南

欢迎提交Issue和Pull Request来改进项目。请确保：
1. 代码符合项目规范
2. 添加必要的测试用例
3. 更新相关文档

---

**技术支持**: 如有问题请提交Issue或联系开发团队。