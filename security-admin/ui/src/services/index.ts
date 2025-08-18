/**
 * 服务模块统一导出
 * 
 * 这个文件提供了所有服务的统一导出，方便在组件中导入使用
 * 
 * 使用示例：
 * import { authService, userService } from '@/services';
 * 或者
 * import { authService } from '@/services/authService';
 */

// HTTP配置
export { businessApi, authApi } from '../apis/api';

// 认证服务
export { authService } from './authService';

// 业务服务
export { userService } from '../apis/userService';
export { roleService } from '../apis/roleService';
export { permissionService } from '../apis/permissionService';

// 工具类
export { TokenManager } from './tokenManager';

// 为了保持向后兼容，也导出默认的api实例
export { default as api } from '../apis/api';