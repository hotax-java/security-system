# 权限控制组件使用说明

## 简介

本项目提供了一套简单易用的权限控制组件，可以根据用户权限动态控制UI元素的显示。主要包括：

1. `Permission` 组件：用于控制任何React子组件的渲染
2. `PermissionUtil` 工具类：提供权限判断的核心功能

## 权限组件使用方法

### 基本用法

将需要权限控制的UI元素包裹在 `Permission` 组件中：

```jsx
import Permission from '../components/common/Permission';

// 基本用法 - 只有拥有user:create权限的用户才能看到按钮
<Permission code="user:create">
  <Button type="primary">创建用户</Button>
</Permission>
```

### 多权限控制（满足任一权限）

```jsx
// 拥有role:create或role:manage任一权限即可
<Permission code={["role:create", "role:manage"]}>
  <Button>角色管理</Button>
</Permission>
```

### 无权限时显示禁用状态

默认情况下，无权限时组件会被完全隐藏。如果希望显示禁用状态：

```jsx
<Permission code="report:export" noRender={false}>
  <Button>导出报表</Button>
</Permission>
```

### 无权限时显示替代内容

```jsx
<Permission 
  code="admin:access" 
  fallback={<Alert type="warning" message="您没有管理员权限" />}
>
  <AdminPanel />
</Permission>
```

### 表格操作列中使用

```jsx
const columns = [
  // ...其他列
  {
    title: '操作',
    render: (_, record) => (
      <>
        <Permission code="user:edit">
          <Button type="link">编辑</Button>
        </Permission>
        
        <Permission code="user:delete">
          <Button type="link" danger>删除</Button>
        </Permission>
      </>
    )
  }
];
```

## 权限工具类使用方法

`PermissionUtil` 提供了直接检查权限的方法，可以在组件、Hook或其他代码中使用：

```jsx
import { PermissionUtil } from '../utils/permissionUtil';

// 在条件渲染中使用
function MyComponent() {
  return (
    <div>
      {PermissionUtil.hasPermission('dashboard:view') && (
        <DashboardStats />
      )}
      
      {/* 检查多个权限（任一满足） */}
      {PermissionUtil.hasAnyPermission(['admin:access', 'super:admin']) && (
        <AdminSettings />
      )}
      
      {/* 获取所有权限（用于调试） */}
      <div>
        当前拥有权限: {PermissionUtil.getUserPermissions().join(', ')}
      </div>
    </div>
  );
}
```

## 在自定义Hook中使用

```jsx
import { useState, useEffect } from 'react';
import { PermissionUtil } from '../utils/permissionUtil';

export function useHasPermission(permission) {
  const [hasPermission, setHasPermission] = useState(false);
  
  useEffect(() => {
    setHasPermission(PermissionUtil.hasPermission(permission));
  }, [permission]);
  
  return hasPermission;
}

// 使用
function MyComponent() {
  const canCreateUser = useHasPermission('user:create');
  
  return (
    <div>
      {canCreateUser && <Button>创建用户</Button>}
    </div>
  );
}
```

## 性能优化

权限工具类已实现缓存机制，避免重复解析JWT token。在token更新时，请调用：

```jsx
// 在token更新后重置缓存
TokenManager.saveTokens(accessToken, refreshToken, expiresIn);
PermissionUtil.resetCache(); // 清除缓存，强制重新解析token
```

## 注意事项

1. 权限控制仅在前端生效，请确保关键操作在后端也有权限验证
2. JWT token中的权限（authorities字段）是权限控制的唯一依据
3. 权限组件使用React的条件渲染，不影响应用性能 