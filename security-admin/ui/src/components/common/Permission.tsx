import React, { ReactElement } from 'react';
import { PermissionUtil } from '../../utils/permissionUtil';

interface PermissionProps {
  /**
   * 权限代码，单个字符串或字符串数组(任一满足)
   */
  code?: string | string[];
  
  /**
   * 无权限时的替代内容
   */
  fallback?: React.ReactNode;
  
  /**
   * 无权限时的行为
   * true: 不渲染内容
   * false: 渲染禁用状态
   */
  noRender?: boolean;
}

/**
 * 权限控制组件
 * 根据用户权限决定是否显示子组件内容
 */
const Permission: React.FC<React.PropsWithChildren<PermissionProps>> = ({
  code,
  fallback = null,
  noRender = true,
  children
}) => {
  // 无权限要求时直接显示
  if (!code) {
    return <>{children}</>;
  }
  
  // 判断权限
  const permissions = Array.isArray(code) ? code : [code];
  let hasPermission = false;
  
  try {
    hasPermission = permissions.some(p => PermissionUtil.hasPermission(p));
    // console.log(`权限检查[${permissions.join(',')}]: ${hasPermission ? '通过✅' : '未通过❌'}`);
    
    // 添加所有权限的调试信息
    if (!hasPermission) {
      const allPermissions = PermissionUtil.getUserPermissions();
      console.log('当前用户所有权限:', allPermissions);
    }
  } catch (error) {
    console.error(`权限检查[${permissions.join(',')}]出错:`, error);
    // 出错默认不显示，保证安全
    return fallback ? <>{fallback}</> : null;
  }
  
  // 根据权限判断结果渲染内容
  if (hasPermission) {
    // 有权限，直接显示内容
    return <>{children}</>;
  } else if (!noRender && React.isValidElement(children)) {
    // 无权限但配置为显示禁用状态
    return React.cloneElement(
      children as ReactElement<any>,
      { 
        disabled: true,
        title: '您没有执行此操作的权限'
      }
    );
  } else {
    // 无权限且配置为不渲染，显示fallback或null
    return <>{fallback}</>;
  }
};

export default Permission; 