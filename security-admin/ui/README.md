# 前端项目启动和构建指南

## 环境说明

本项目支持多环境配置：
- **开发环境 (development)**: 用于本地开发
- **测试环境 (test)**: 用于测试部署
- **生产环境 (production)**: 用于正式部署

## 可用脚本

### 启动脚本

```bash
# 默认启动 (开发模式)
npm start

# 开发环境启动
npm run start:dev

# 测试环境启动
npm run start:test

# 生产环境启动 (需要先构建)
npm run start:prod
```

### 构建脚本

```bash
# 默认构建 (生产模式)
npm run build

# 开发环境构建
npm run build:dev

# 测试环境构建
npm run build:test

# 生产环境构建
npm run build:prod
```

### 测试脚本

```bash
# 运行测试
npm test

# 运行测试并生成覆盖率报告
npm run test:coverage

# CI环境测试
npm run test:ci
```

### 代码质量脚本

```bash
# 代码检查
npm run lint

# 自动修复代码问题
npm run lint:fix

# 代码格式化
npm run format
```

### 其他脚本

```bash
# 分析打包文件大小
npm run analyze

# 启动静态文件服务器
npm run serve

# 清理构建文件和缓存
npm run clean
```

## 环境配置

项目使用 React 标准的环境变量配置方式，每个环境都有对应的配置文件：

- `.env` - 默认配置
- `.env.development` - 开发环境配置
- `.env.test` - 测试环境配置
- `.env.production` - 生产环境配置

### 环境变量说明

- `REACT_APP_ENV`: 当前环境标识
- `REACT_APP_API_BASE_URL`: 后台管理API基础地址（用于用户、角色、权限管理等业务API）
- `REACT_APP_AUTH_BASE_URL`: 认证服务API基础地址（用于登录、登出、令牌验证等认证API）
- `REACT_APP_TITLE`: 应用标题
- `REACT_APP_VERSION`: 应用版本
- `REACT_APP_DEBUG`: 是否开启调试模式

> **注意**: 所有自定义环境变量必须以 `REACT_APP_` 开头才能在前端代码中访问。

### 服务地址配置

项目采用微服务架构，前端需要与两个后端服务进行通信：

1. **认证服务** (`REACT_APP_AUTH_BASE_URL`): 
   - 默认地址: `http://localhost:9001`
   - 负责用户登录、登出、令牌验证等认证相关功能

2. **后台管理服务** (`REACT_APP_API_BASE_URL`):
   - 默认地址: `http://localhost:8080`
   - 负责用户管理、角色管理、权限管理等业务功能

## 使用示例

### 开发环境

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run start:dev
```

### 测试环境部署

```bash
# 构建测试环境版本
npm run build:test

# 启动测试环境服务器
npm run start:prod
```

### 生产环境部署

```bash
# 构建生产环境版本
npm run build:prod

# 部署到服务器
# 将 build 文件夹内容上传到服务器
```

## 注意事项

1. 首次运行前请确保已安装所有依赖：`npm install`
2. 生产环境启动需要先执行构建命令
3. 不同环境的 API 地址需要在对应的 `.env.*` 文件中配置
4. 代码提交前建议运行 `npm run lint:fix` 和 `npm run format`