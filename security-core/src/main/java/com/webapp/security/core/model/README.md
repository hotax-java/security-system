# 统一错误响应类使用指南

本项目提供了两个统一的错误响应类，用于规范化API错误响应格式：

## 1. OAuth2ErrorResponse

专门用于OAuth2相关的错误响应，符合RFC 6749 OAuth2规范。

### 使用示例

```java
// 无效授权
return OAuth2ErrorResponse.invalidGrant("用户名或密码错误");

// 无效客户端
return OAuth2ErrorResponse.invalidClient("客户端认证失败");

// 服务器错误
return OAuth2ErrorResponse.serverError("服务器内部错误");

// 自定义错误
return OAuth2ErrorResponse.error("custom_error", "自定义错误描述", HttpStatus.BAD_REQUEST);
```

### 支持的标准错误类型

- `invalidRequest()` - 无效请求 (400)
- `invalidClient()` - 无效客户端 (400)
- `invalidGrant()` - 无效授权 (401)
- `unauthorizedClient()` - 未授权客户端 (400)
- `unsupportedGrantType()` - 不支持的授权类型 (400)
- `invalidScope()` - 无效范围 (400)
- `serverError()` - 服务器错误 (500)
- `temporarilyUnavailable()` - 临时不可用 (503)
- `accessDenied()` - 访问被拒绝 (403)
- `unsupportedResponseType()` - 不支持的响应类型 (400)

### 响应格式

```json
{
  "error": "invalid_grant",
  "error_description": "用户名或密码错误"
}
```

## 2. ErrorResponse

通用错误响应类，适用于各种业务场景。

### 使用示例

```java
// 400错误
return ErrorResponse.badRequest("请求参数错误");

// 401错误
return ErrorResponse.unauthorized("认证失败");

// 403错误
return ErrorResponse.forbidden("权限不足");

// 404错误
return ErrorResponse.notFound("资源不存在");

// 500错误
return ErrorResponse.internalServerError("服务器内部错误");

// 业务错误
return ErrorResponse.businessError("BIZ_001", "业务逻辑错误");

// 从异常创建错误响应
return ErrorResponse.fromException(e, HttpStatus.INTERNAL_SERVER_ERROR);

// 自定义错误（带详细信息）
return ErrorResponse.custom("CUSTOM_001", "自定义错误", "详细错误信息", HttpStatus.BAD_REQUEST);
```

### 响应格式

```json
{
  "success": false,
  "code": "UNAUTHORIZED",
  "message": "认证失败",
  "timestamp": "2024-01-01T12:00:00",
  "details": "详细错误信息（可选）",
  "path": "/api/users（可选）"
}
```

## 3. 兼容性支持

两个错误响应类都提供了 `toMap()` 方法，用于兼容现有代码：

```java
// 返回Map格式
OAuth2ErrorResponse error = OAuth2ErrorResponse.builder()
    .error("invalid_grant")
    .error_description("用户名或密码错误")
    .build();
// 可以通过 toMap() 方法获取 Map 格式
Map<String, Object> errorMap = error.toMap();

// 或者直接使用静态方法
return OAuth2ErrorResponse.error(OAuth2ErrorResponse.INVALID_GRANT, "用户名或密码错误", HttpStatus.UNAUTHORIZED);
```

## 4. 迁移指南

### 从Map构建方式迁移

**之前的代码：**
```java
Map<String, Object> errorResponse = new HashMap<>();
errorResponse.put("error", "invalid_grant");
errorResponse.put("error_description", "用户名或密码错误");
return ResponseEntity.status(401).body(errorResponse);
```

**迁移后：**
```java
return OAuth2ErrorResponse.invalidGrant("用户名或密码错误");
```

### 从自定义ErrorResponse类迁移

**之前的代码：**
```java
return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
    .body(new CustomErrorResponse("创建账号失败"));
```

**迁移后：**
```java
return ErrorResponse.internalServerError("创建账号失败");
```

## 5. 最佳实践

1. **OAuth2场景**：使用 `OAuth2ErrorResponse`
2. **业务场景**：使用 `ErrorResponse`
3. **统一错误码**：为不同的错误类型定义统一的错误码
4. **国际化支持**：错误消息支持国际化
5. **日志记录**：在返回错误响应前记录相应的日志

## 6. 注意事项

- 所有静态方法都会自动设置合适的HTTP状态码
- 时间戳会自动生成
- 可以通过Builder模式创建更复杂的错误响应
- 支持链式调用，提高代码可读性