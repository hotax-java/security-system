import { TokenManager } from '../services/tokenManager';
// 使用import导入jwt-decode
import { jwtDecode } from 'jwt-decode';

/**
 * 权限工具类
 * 提供检查用户权限的相关功能
 */
export class PermissionUtil {
  // 权限缓存，避免重复解析JWT
  private static permissionCache: Map<string, boolean> = new Map();
  // 缓存权限列表
  private static permissionsArray: string[] | null = null;
  // 缓存使用的token，用于检测token是否变化
  private static lastTokenUsed: string | null = null;
  
  /**
   * 重置缓存
   * 当token变化时调用
   */
  static resetCache() {
    this.permissionCache.clear();
    this.permissionsArray = null;
    this.lastTokenUsed = null;

  }

  /**
   * 核心方法：获取当前用户的所有权限
   * 内部方法，复用逻辑
   * @returns 权限代码数组
   */
  private static getAuthorities(): string[] {
    // 使用缓存
    if (this.permissionsArray !== null) {
      return this.permissionsArray;
    }

    try {
      // 获取当前token
      const token = TokenManager.getAccessToken();
      if (!token) {

        return [];
      }

      // 检查token是否变化
      if (token !== this.lastTokenUsed) {
        this.resetCache();
        this.lastTokenUsed = token;
      }

      // 解析token
      let decodedToken: any;
      try {
        // @ts-ignore
        decodedToken = jwtDecode(token);
  
      } catch (e) {
        console.error('JWT解析失败:', e);
        return [];
      }
      
      // 检查是否有authorities字段
      if (!decodedToken || typeof decodedToken !== 'object') {
        console.error('JWT格式无效:', decodedToken);
        return [];
      }
      
      // 从token中获取权限列表
      const authorities = Array.isArray(decodedToken.authorities) 
        ? decodedToken.authorities 
        : [];
      

      
      // 保存到缓存
      this.permissionsArray = authorities;
      return authorities;
    } catch (error) {
      console.error('获取权限信息失败:', error);
      return [];
    }
  }

  /**
   * 检查用户是否拥有指定权限
   * @param permissionCode 权限代码，如'USER_CREATE'
   * @returns 是否拥有权限
   */
  static hasPermission(permissionCode: string): boolean {
    if (!permissionCode) return true; // 未指定权限则默认通过
    
    // 检查缓存
    if (this.permissionCache.has(permissionCode)) {
      return this.permissionCache.get(permissionCode)!;
    }
    
    // 获取权限并判断
    const authorities = this.getAuthorities();
    
    // 直接匹配
    const result = authorities.includes(permissionCode);

    
    // 缓存结果
    this.permissionCache.set(permissionCode, result);
    
    return result;
  }

  /**
   * 检查用户是否拥有指定权限中的任意一个
   * @param permissionCodes 权限代码数组
   * @returns 是否拥有任一权限
   */
  static hasAnyPermission(permissionCodes: string[]): boolean {
    return permissionCodes.some(code => this.hasPermission(code));
  }

  /**
   * 检查用户是否拥有指定权限中的所有权限
   * @param permissionCodes 权限代码数组
   * @returns 是否拥有所有权限
   */
  static hasAllPermissions(permissionCodes: string[]): boolean {
    return permissionCodes.every(code => this.hasPermission(code));
  }

  /**
   * 获取当前用户的所有权限
   * @returns 权限代码数组
   */
  static getUserPermissions(): string[] {
    return this.getAuthorities();
  }
} 