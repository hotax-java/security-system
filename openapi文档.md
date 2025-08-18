

# OPENAPI 客户端认证与权限管理系统设计文档

## 一、设计背景与目标

### 1.1 背景

为支持外部系统安全对接内部服务，需提供一套标准化的 OPENAPI 认证授权机制，实现“客户端身份校验、接口权限管控、操作可追溯”的核心能力，同时兼容现有微服务架构和 Admin 权限体系。

### 1.2 目标

- 支持外部客户端通过`appid/appsecret`获取访问令牌（Token）；
- 实现客户端对 OPENAPI 的细粒度权限控制（可配置允许访问的接口）；
- 关联客户端创建者信息（`userid/username`），支持操作审计与业务追溯；
- 兼容 Spring Security OAuth2 标准，保障系统安全性与可扩展性。

## 二、核心流程设计

### 2.1 整体流程概览

```mermaid
graph TD
    A[客户端创建] --> B[配置API权限];
    B --> C[获取访问令牌(Token)];
    C --> D[携带Token调用OPENAPI];
    D --> E[Token校验+权限校验];
    E --> F[调用业务微服务(传递创建者信息)];
```

### 2.2 详细流程说明

#### （1）客户端创建与权限配置（Admin 系统操作）

- **操作人**：内部管理员（`sys_user`系统用户）；
- **流程**：
  1. 管理员在 Admin 界面填写客户端信息（名称、联系人等），系统生成`appid`和`appsecret`；
  2. 系统在`oauth2_registered_client`中仅保留一条固定的 OPENAPI 客户端记录（例如`client_id='openapi'`，用于承载令牌配置）；
  3. 将创建的`appid/appsecret`与固定的`client_id`关联，写入`sys_client_credential`表（记录创建人信息与状态等）；
  4. 管理员在界面为每个`appid`配置可访问的 OPENAPI 权限（选择`sys_resource`中的资源），关联关系存入`sys_credential_resource_rel`表。

#### （2）令牌获取流程（外部客户端操作）

- **接口**：`POST /oauth2/token`（OAuth2 标准端点）；
- **请求参数**：

  ```http
  Content-Type: application/x-www-form-urlencoded
  Authorization: Basic Base64(appid:appsecret)  // 客户端凭证

  grant_type=client_credentials&scope=openapi
  ```

- **校验逻辑**：
  1. 系统解析`Authorization`头，获取`appid`和`appsecret`；
  2. 查询`sys_client_credential`校验`appsecret`是否匹配（加密比对，状态是否启用）；
  3. 通过后，映射到固定的`oauth2_registered_client.client_id='openapi'`完成令牌签发（令牌中携带`app_id`作为自定义声明用于后续权限校验与审计）；
- **响应示例**：
  ```json
  {
    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "Bearer",
    "expires_in": 3600
  }
  ```

#### （3）OPENAPI 调用流程（外部客户端操作）

- **接口**：业务微服务提供的 OPENAPI（如`GET /api/v1/users`）；
- **请求头**：
  ```http
  Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...  // 步骤2获取的token
  ```
- **校验与转发逻辑**：
  1. **Token 校验**：OPENAPI 网关/认证服务解析`access_token`（Opaque Token），通过本地缓存（L1）/Redis（L2）获取`appId`并校验令牌有效性（未过期、未吊销）；缓存未命中时查询`oauth2_authorization`后回填缓存；
  2. **客户端状态校验**：通过`appId`查询`sys_client_credential`状态（优先缓存，必要时回源DB），校验是否启用（`status=1`）；
  3. **权限校验**：解析当前请求的接口路径，匹配`sys_resource`中的资源，通过`sys_credential_resource_rel`表校验`appId`是否有权访问；
  4. **传递创建者信息**：校验通过后，通过`appId`查询`sys_client_credential`获取创建者`creator_user_id`和`creator_username`，以请求头（如`X-Creator-Id`、`X-Creator-Name`）或内部上下文形式传递给业务微服务；
  5. **业务处理**：业务微服务接收请求，基于创建者信息进行审计或业务逻辑处理；
  6. **资源级限流/配额**：在权限通过后，对匹配到的资源执行资源级 QPS/并发限流（Redis+Lua 令牌桶/漏桶）与日配额（Redis 计数器）校验；若触发限流/配额，返回`429 Too Many Requests`，并附带`RateLimit-Limit`、`RateLimit-Remaining`、`RateLimit-Reset`响应头。

### 2.3 令牌签发与缓存写入（详细）

- **请求**：`POST /oauth2/token`，`Authorization: Basic Base64(appid:appsecret)`，`grant_type=client_credentials&scope=openapi`
- **处理步骤**：
  1. 解析 Basic，得到`appId/appSecret`；
  2. 读取 L1/L2 `app:{appId}` 缓存，未命中回源`sys_client_credential`校验密钥（BCrypt）、状态；
  3. 映射固定`oauth2_registered_client.client_id='openapi'`完成签发；生成 Opaque Token；
  4. 写入`oauth2_authorization`（包含`tokenValue`、`expiresAt`、`appId`、`scopes`等扩展）；
  5. 回填缓存：
     - L2：`SETEX tok:{token} <ttl> {appId, expiresAt, status, scopes}`；
     - L1：放入 Guava Cache（值内包含`expiresAt`，读取时二次校验时间）；
  6. 响应 token；token 到期后需重新获取新 token（原 token 不可续期）。

- **可选（单活策略）**：如需“同一 app 仅允许一个活跃 token”，则在签发新 token 后：
  - 吊销旧 token（更新`oauth2_authorization`）；
  - 发布事件`sec:token:revoked`（载荷包含旧`token`与`appId`）。

## 三、表结构设计

### 3.1 核心表清单

| 表名                           | 作用                                   | 类型       |
| ------------------------------ | -------------------------------------- | ---------- |
| `oauth2_registered_client`     | 固定一条 OPENAPI 客户端配置            | 框架标准表 |
| `sys_client_credential`        | 存储`appid/appsecret`与创建人等信息    | 业务扩展表 |
| `sys_resource`                 | 定义可开放的 API 资源                  | 业务基础表 |
| `sys_credential_resource_rel`  | 记录`appid`与 API 资源的授权关系       | 关联关系表 |
| `oauth2_authorization`         | 存储令牌信息（框架自动维护）           | 框架标准表 |

### 3.2 详细表结构

#### （1）`oauth2_registered_client`（框架标准表）

```sql
CREATE TABLE oauth2_registered_client (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    client_id VARCHAR(100) NOT NULL UNIQUE COMMENT '客户端标识（仅保留 openapi 等固定记录）',
    client_secret VARCHAR(200) NOT NULL COMMENT '客户端密钥（加密存储）',
    client_name VARCHAR(200) NOT NULL COMMENT '客户端名称',
    client_authentication_methods VARCHAR(1000) NOT NULL COMMENT '认证方式（如client_secret_basic）',
    authorization_grant_types VARCHAR(1000) NOT NULL COMMENT '授权类型（如client_credentials）',
    redirect_uris VARCHAR(1000) COMMENT '重定向URI（客户端凭证模式可不填）',
    scopes VARCHAR(1000) NOT NULL COMMENT '权限范围（如openapi）',
    client_settings VARCHAR(2000) NOT NULL COMMENT '客户端配置（JSON格式）',
    token_settings VARCHAR(2000) NOT NULL COMMENT '令牌配置（JSON格式，如过期时间）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2客户端标准表（仅固定记录）';
```

#### （2）`sys_client_credential`（AppID/Secret 凭证表）

```sql
CREATE TABLE sys_client_credential (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    app_id VARCHAR(100) NOT NULL UNIQUE COMMENT '应用唯一标识（AppID）',
    app_secret VARCHAR(200) NOT NULL COMMENT '应用密钥（BCrypt 加密存储）',
    client_id VARCHAR(100) NOT NULL COMMENT '关联 oauth2_registered_client.client_id（固定为 openapi）',
    creator_user_id BIGINT NOT NULL COMMENT '创建者用户ID（关联 sys_user.user_id）',
    creator_username VARCHAR(100) COMMENT '创建者用户名快照',
    status TINYINT DEFAULT 1 COMMENT '状态（1-启用，0-禁用）',
    remark VARCHAR(500) COMMENT '备注信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_client_id (client_id),
    INDEX idx_creator_user_id (creator_user_id),
    FOREIGN KEY (client_id) REFERENCES oauth2_registered_client(client_id) ON DELETE RESTRICT,
    FOREIGN KEY (creator_user_id) REFERENCES sys_user(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AppID/AppSecret 凭证表';
```

#### （3）`sys_resource`（API 资源定义表）

```sql
CREATE TABLE sys_resource (
    resource_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    resource_code VARCHAR(100) NOT NULL UNIQUE COMMENT '资源编码（如user:query）',
    resource_name VARCHAR(100) NOT NULL COMMENT '资源名称（如用户查询接口）',
    resource_path VARCHAR(500) NOT NULL COMMENT '接口路径（如/api/v1/users/**）',
    method VARCHAR(10) NOT NULL COMMENT 'HTTP方法（GET/POST/PUT/DELETE）',
    qps_limit INT DEFAULT NULL COMMENT '每秒请求上限（资源级，NULL 表示不限制）',
    burst_capacity INT DEFAULT NULL COMMENT '令牌桶突发容量（资源级，可选）',
    daily_quota INT DEFAULT NULL COMMENT '每日调用上限（资源级，可选）',
    concurrency_limit INT DEFAULT NULL COMMENT '并发限制（资源级，可选）',
    status TINYINT DEFAULT 1 COMMENT '资源状态（1-启用，0-禁用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_resource_path (resource_path, method)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OPENAPI资源定义表';
```

#### （4）`sys_credential_resource_rel`（凭证-资源关联表）

```sql
CREATE TABLE sys_credential_resource_rel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    credential_id BIGINT NOT NULL COMMENT '关联 sys_client_credential.id',
    resource_id BIGINT NOT NULL COMMENT '关联 sys_resource.resource_id',
    grant_user_id BIGINT NOT NULL COMMENT '授权人ID（关联 sys_user.user_id）',
    grant_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    UNIQUE KEY uk_cred_resource (credential_id, resource_id) COMMENT '避免重复授权',
    FOREIGN KEY (credential_id) REFERENCES sys_client_credential(id) ON DELETE CASCADE,
    FOREIGN KEY (resource_id) REFERENCES sys_resource(resource_id) ON DELETE CASCADE,
    FOREIGN KEY (grant_user_id) REFERENCES sys_user(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='App 凭证-API 资源授权关系表';
```

### 3.3 表关系图

```mermaid
erDiagram
    oauth2_registered_client ||--o{ sys_client_credential : "1对多（固定 openapi -> 多个 appid）"
    sys_client_credential ||--o{ sys_credential_resource_rel : "1对多关联"
    sys_resource ||--o{ sys_credential_resource_rel : "1对多关联"
    sys_user ||--o{ sys_client_credential : "1对多（创建人）"
    sys_user ||--o{ sys_credential_resource_rel : "1对多（授权人）"
```

## 四、权限控制设计

### 4.1 资源权限粒度

- **资源定义**：以“接口路径+HTTP 方法”为最小粒度（如`GET /api/v1/users`和`POST /api/v1/users`为两个不同资源）；
- **权限配置**：管理员为每个`appid`（`sys_client_credential`）勾选可访问的资源，关联关系存入`sys_credential_resource_rel`表。

### 4.2 权限校验逻辑

在 OPENAPI 网关/认证服务中实现拦截器，核心逻辑：

```java
// 伪代码
public boolean checkPermission(HttpServletRequest request, String accessToken) {
    // 1. 从 Token 解析出 appId（签发时已作为自定义声明写入）
    String appId = tokenService.parseAppId(accessToken);

    // 2. 根据 appId 找到凭证与创建者、状态
    SysClientCredential cred = credentialService.findByAppId(appId);
    if (cred == null || cred.getStatus() != 1) {
        return false;
    }

    // 3. 获取当前请求路径与方法
    String path = request.getRequestURI();
    String method = request.getMethod();

    // 4. 匹配资源并校验授权关系
    Long resourceId = sysResourceMapper.matchResource(path, method);
    if (resourceId == null) {
        return false; // 未定义的资源
    }
    return credentialResourceRelMapper.exists(cred.getId(), resourceId);
}
```

## 五、接口规范

### 5.1 令牌获取接口（`/oauth2/token`）

- **请求方式**：POST
- **请求头**：`Authorization: Basic Base64(appid:appsecret)`
- **请求参数**：`grant_type=client_credentials&scope=openapi`
- **响应格式**：
  ```json
  {
    "access_token": "字符串令牌",
    "token_type": "Bearer",
    "expires_in": 3600 // 过期时间（秒）
  }
  ```

### 5.2 OPENAPI 调用规范

- **请求头**：`Authorization: Bearer {access_token}`
- **创建者信息传递**：网关自动添加请求头
  ```http
  X-Creator-Id: 10086  // 客户端创建者userid
  X-Creator-Name: "张三"  // 客户端创建者username
  ```

### 5.3 错误码定义

| 错误码 | 描述           | 场景示例                        |
| ------ | -------------- | ------------------------------- |
| 401    | 未授权         | Token 无效、过期或未携带        |
| 403    | 权限不足       | 客户端未配置当前接口的访问权限  |
| 400    | 无效请求       | 缺少 appid/appsecret 或参数错误 |
| 500    | 服务器内部错误 | 服务端处理异常                  |

## 六、安全设计

1. **传输安全**：所有接口强制使用 HTTPS，防止`appsecret`和 Token 被窃听；
2. **凭证安全**：`appsecret`存储时使用 BCrypt 加密，禁止明文存储；
3. **令牌安全（Opaque Token）**：
   - 采用 Opaque Token（`client_credentials` 签发），令牌元数据落库于`oauth2_authorization`；
   - 校验路径：优先从本地缓存（L1：Guava Cache）与 Redis 缓存（L2）读取`token -> {appId, expiresAt, status, scopes}`；
   - 缓存 TTL 与令牌过期时间一致；令牌过期后客户端需重新获取新令牌；
   - 支持令牌吊销：吊销后主动删除 Redis 缓存并通过消息通知清理各实例本地缓存；
4. **权限隔离**：客户端仅能访问已授权的 API 资源，未授权资源直接拦截；
5. **审计日志**：记录客户端调用记录（包含`appid`、`userid`、接口路径、时间等），并包含限流/配额触发信息，支持追溯；
6. **限流与配额**：资源级 QPS/并发限流采用 Redis+Lua，全局一致；日配额采用 Redis 计数器（`INCR`+当日过期），必要时定期落库审计。

## 七、与现有系统集成点

1. **Admin 系统**：
   - 新增“客户端管理”模块：创建`appid/appsecret`（写入`sys_client_credential`），禁用/启用；
   - 新增“资源授权”模块：为`appid`配置 API 资源（写入`sys_credential_resource_rel`）；
2. **业务微服务**：
   - 从请求头获取`X-Creator-Id`和`X-Creator-Name`（由认证服务根据`appid`回填）；
3. **认证服务**：
   - 解析 Basic 凭证时按`sys_client_credential`校验，映射到固定`oauth2_registered_client`记录完成签发；
   - 当`appid`被禁用或权限变更时，通过消息通知（如 Redis Pub/Sub）下发失效事件，清理各实例本地缓存（令牌、`appid`状态、授权关系、路由索引）。

## 八、部署与扩展建议

1. **部署架构**：认证服务与 OPENAPI 网关独立部署，业务微服务按需扩展；
2. **性能优化**：
   - 缓存客户端信息和权限配置（本地 L1 + Redis L2），减少数据库查询；
   - 令牌校验逻辑异步化，提升接口响应速度；
3. **扩展性**：未来支持多环境（测试/生产）客户端隔离，可通过`sys_client_credential`表增加`env`字段实现。

## 九、缓存与限流设计

### 9.1 缓存分层与配置
- **L1 本地缓存**：Guava Cache（或 Caffeine）
  - 建议：最大条目数 `maxSize=200k`（按流量调整）；
  - 过期策略：`expireAfterWrite=10m`（统一上限）；值对象内包含`expiresAt`，读取时若`now > expiresAt`则判定过期并清理，避免不同 token TTL 无法逐条控制的问题；
  - 防穿透：对不存在数据可短期缓存`null`或特殊标记（1-5s）。
- **L2 分布式缓存（Redis）**
  - 序列化：JSON 或 MsgPack；
  - TTL：令牌键与 token 过期一致；`app`与`auth`键 TTL 可配置（例如 300s）；
  - 一致性：采用“写 DB → 写/删 Redis → 发布事件 → 清理 L1”的顺序。

### 9.2 缓存键、值结构与示例
- 令牌缓存：`tok:{accessToken}`
  - 值：
    ```json
    {"appId":"app_abc","expiresAt":1736937600000,"status":1,"scopes":["openapi"],"creatorUserId":10001,"creatorUsername":"zhangsan"}
    ```
- 应用缓存：`app:{appId}`
  - 值：
    ```json
    {"id":123,"status":1,"creatorUserId":10001,"creatorUsername":"zhangsan","updatedAt":1736837600000}
    ```
- 授权缓存：`auth:{appId}:{resourceId}` → `"1"` 或 `"0"`
- 资源版本：`sec:resource:version` → 递增版本号（数值）；本地维持路由索引版本，订阅变更进行热更新。

### 9.3 读写流程（命中/未命中）
- Token 校验伪代码：
  ```java
  TokenMeta meta = L1.get(token);
  if (meta == null || meta.isExpired(now)) {
      meta = Redis.getJson("tok:"+token);
      if (meta == null || meta.isExpired(now)) {
          meta = dbLoadAuthorization(token); // 查询 oauth2_authorization
          if (meta == null) return unauthorized();
          Redis.setex("tok:"+token, ttl(meta), meta);
      }
      L1.put(token, meta);
  }
  // 拿到 appId 后，校验 app 状态与授权
  AppSnapshot app = L1.getApp(meta.getAppId());
  if (app == null) {
      app = Redis.getJson("app:"+meta.getAppId());
      if (app == null) {
          app = dbLoadApp(meta.getAppId());
          if (app == null) return unauthorized();
          Redis.setex("app:"+app.getAppId(), appTtl, app);
      }
      L1.putApp(app.getAppId(), app);
  }
  if (app.getStatus() != 1) return forbidden();
  // 授权关系
  String k = "auth:"+app.getAppId()+":"+resourceId;
  String allowed = L1.get(k);
  if (allowed == null) {
      allowed = Redis.get(k);
      if (allowed == null) {
          boolean ok = dbCheckAuth(app.getId(), resourceId);
          allowed = ok ? "1" : "0";
          Redis.setex(k, 300, allowed);
      }
      L1.put(k, allowed);
  }
  if (!"1".equals(allowed)) return forbidden();
  ```

### 9.4 失效与消息通知
- 频道与载荷：
  - `sec:app:disabled`
    ```json
    {"appId":"app_abc","ts":1736837600000}
    ```
  - `sec:resource:updated`
    ```json
    {"version":102,"ts":1736837600000}
    ```
  - `sec:auth:changed`
    ```json
    {"appId":"app_abc","resourceId":456,"ts":1736837600000}
    ```
  - `sec:token:revoked`（可选）
    ```json
    {"token":"opaque_xxx","appId":"app_abc","ts":1736837600000}
    ```
- 订阅方处理：
  - 收到后优先清理 L1；对 L2 使用`DEL`（若存在）+ 等待自然过期；
  - 处理幂等与乱序（仅依据键删除，不依赖消息顺序）；
  - 对`resource:updated`，比对版本号后重建本地路由索引。

### 9.5 资源路由索引
- 启动时全量加载`sys_resource`（状态=1），构建 Ant 风格匹配结构（可用前缀树/Trie + 通配符节点）。
- 维护`sec:resource:version`，当版本变更时：
  - 拉取增量或全量资源，重建索引并热切换；
  - 清理与资源相关的授权缓存（可通过`sec:auth:changed`批量事件或逐条）。

### 9.6 限流与配额（Redis + Lua）
- 键命名：
  - QPS：`rl:qps:{resourceId}`（存剩余令牌、上次补充时间戳）
  - 并发：`rl:cc:{resourceId}`（当前占用数）
  - 日配额：`quota:day:{resourceId}:{yyyyMMdd}`（累计次数）

- QPS 令牌桶（Lua 伪代码）：
  ```lua
  local key = KEYS[1]       -- rl:qps:{resourceId}
  local rate = tonumber(ARGV[1])       -- qps_limit
  local burst = tonumber(ARGV[2])      -- burst_capacity (>= rate)
  local now = tonumber(ARGV[3])        -- ms
  local interval = 1000                -- ms
  local last_ts = tonumber(redis.call('HGET', key, 'ts') or now)
  local tokens = tonumber(redis.call('HGET', key, 'tk') or burst)
  local delta = math.floor((now - last_ts) * rate / interval)
  tokens = math.min(burst, tokens + delta)
  if tokens <= 0 then
    redis.call('HSET', key, 'ts', now, 'tk', tokens)
    redis.call('PEXPIRE', key, 2000)
    return {0, tokens}
  end
  tokens = tokens - 1
  redis.call('HSET', key, 'ts', now, 'tk', tokens)
  redis.call('PEXPIRE', key, 2000)
  return {1, tokens}
  ```

- 并发限制（Lua 伪代码）：
  ```lua
  local key = KEYS[1] -- rl:cc:{resourceId}
  local max = tonumber(ARGV[1])
  local ttl = tonumber(ARGV[2]) -- ms，防止泄漏
  local cur = tonumber(redis.call('GET', key) or 0)
  if cur >= max then return {0, cur} end
  cur = cur + 1
  redis.call('SET', key, cur, 'PX', ttl)
  return {1, cur}
  ```
  - 在请求完成时，服务端执行`DECR`或 Lua 安全释放（避免并发误减）。

- 日配额：
  ```lua
  local key = KEYS[1] -- quota:day:{resourceId}:{yyyyMMdd}
  local max = tonumber(ARGV[1])
  local ttl = tonumber(ARGV[2]) -- 秒，到当天 23:59:59
  local cnt = tonumber(redis.call('INCR', key))
  if cnt == 1 then redis.call('EXPIRE', key, ttl) end
  if cnt > max then return {0, cnt} end
  return {1, cnt}
  ```

- 返回头（建议）：
  - `RateLimit-Limit`: 配置的速率或配额；
  - `RateLimit-Remaining`: 剩余额度/令牌；
  - `RateLimit-Reset`: 距离窗口重置的秒数或时间戳。

### 9.7 降级与保护
- DB 限频：缓存双 miss 时，对 DB 访问启用单飞（singleflight）与限频；
- 失败快速返回：Redis 不可用时可短期退化为 L1 决策或全拒（按开关）；
- 负缓存：对不存在资源/授权短期缓存，降低穿透风险。

### 9.8 观测与告警
- 指标：
  - `auth_token_lookup_total{source=l1|l2|db, result=hit|miss}`
  - `rate_limit_reject_total{reason=qps|concurrency|quota}`
  - `authorization_denied_total{reason=no_resource|no_grant|app_disabled}`
- 日志：结构化输出`traceId, appId, resourceId, path, method, decision, latencyMs`；
- 告警：连续失败、限流突增、事件处理堆积告警阈值。

```

```
