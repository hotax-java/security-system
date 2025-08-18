import { PermissionUtil } from '../utils/permissionUtil';

/**
 * Token管理工具类
 * 提供token的存储、获取、清除等功能
 */
export class TokenManager {
  private static readonly ACCESS_TOKEN_KEY = 'access_token';
  private static readonly REFRESH_TOKEN_KEY = 'refresh_token';
  private static readonly TOKEN_EXPIRY_KEY = 'token_expiry';
  private static readonly USER_KEY = 'user';
  private static readonly REFRESH_THRESHOLD_MINUTES = 2; // 默认刷新阈值为2分钟
  
  // TokenManager不再负责刷新token逻辑

  /**
   * 保存token信息
   * @param accessToken 访问令牌
   * @param refreshToken 刷新令牌
   * @param expiresIn 过期时间（秒）
   */
  static saveTokens(accessToken: string, refreshToken?: string, expiresIn?: number): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, accessToken);
    
    if (refreshToken) {
      localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
    }
    
    if (expiresIn && !isNaN(expiresIn)) {
      // 确保 expiresIn 是一个正数
      const validExpiresIn = Math.max(0, expiresIn);
      
      // 添加时间缓冲，将过期时间提前100秒，以处理服务器和客户端时间差异
      const bufferTime = 100; // 缓冲时间（秒）
      const adjustedExpiresIn = Math.max(0, validExpiresIn - bufferTime);
      
      const expiryTime = Date.now() + (adjustedExpiresIn * 1000);
      localStorage.setItem(this.TOKEN_EXPIRY_KEY, expiryTime.toString());
      console.log(`Token will expire at: ${new Date(expiryTime).toLocaleString()}`);
    } else {
      // 如果没有提供有效的过期时间，使用默认值（30分钟 - 缓冲时间）
      const defaultExpiry = Date.now() + ((30 * 60 - 100) * 1000); // 30分钟减去100秒缓冲
      localStorage.setItem(this.TOKEN_EXPIRY_KEY, defaultExpiry.toString());
      console.log(`Using default expiry time: ${new Date(defaultExpiry).toLocaleString()} (in 30 minutes with buffer)`);
    }

    // 重置权限缓存，确保权限信息与最新token同步
    if (PermissionUtil) {
      try {
        PermissionUtil.resetCache();
  
      } catch (error) {
        console.error('重置权限缓存失败', error);
      }
    }
  }

  /**
   * 获取访问令牌 (不检查是否过期)
   */
  static getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  /**
   * 获取有效的访问令牌
   * 如果token已过期，则返回null
   * @returns 有效的token或null
   */
  static getValidAccessToken(): string | null {
    if (this.isTokenExpired()) {
      return null;
    }
    return this.getAccessToken();
  }

  /**
   * 获取刷新令牌
   */
  static getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  /**
   * 检查token是否即将过期（提前指定分钟数）
   * @param bufferMinutes 提前多少分钟判定为即将过期
   */
  static isTokenExpiringSoon(bufferMinutes = this.REFRESH_THRESHOLD_MINUTES): boolean {
    const expiryTime = localStorage.getItem(this.TOKEN_EXPIRY_KEY);
    if (!expiryTime) {
      return false;
    }
    
    const expiry = parseInt(expiryTime, 10);
    if (isNaN(expiry)) {
      return false;
    }
    
    const now = Date.now();
    const minutes = bufferMinutes * 60 * 1000; // 转换为毫秒
    const timeRemaining = expiry - now;
    
    // 如果token已经过期或即将过期（剩余时间小于指定分钟），则需要刷新
    const isExpiring = timeRemaining <= minutes;
    
    // 只在接近过期时记录日志，避免日志过多
    if (isExpiring) {
      console.log(`Token expiry check: timeRemaining=${timeRemaining}ms, bufferMinutes=${minutes}ms, isExpiring=${isExpiring}`);
    }
    
    return isExpiring;
  }

  /**
   * 检查token是否已过期
   */
  static isTokenExpired(): boolean {
    const expiryTime = localStorage.getItem(this.TOKEN_EXPIRY_KEY);
    if (!expiryTime) {
      return true; // 如果没有过期时间，视为已过期
    }
    
    const expiry = parseInt(expiryTime, 10);
    if (isNaN(expiry)) {
      return true; // 如果过期时间无效，视为已过期
    }
    
    const now = Date.now();
    const isExpired = now >= expiry;
    
    // 只在已过期时记录日志，避免日志过多
    if (isExpired) {
      console.log(`Token expired check: now=${now}, expiry=${expiry}, isExpired=${isExpired}`);
    }
    
    return isExpired;
  }

  /**
   * 检查是否已认证
   */
  static isAuthenticated(): boolean {
    const accessToken = this.getAccessToken();
    const refreshToken = this.getRefreshToken();
    
    // 检查token是否存在且未过期
    const hasTokens = !!(accessToken && refreshToken);
    const notExpired = !this.isTokenExpired();
    
    return hasTokens && notExpired;
  }

  /**
   * 清除所有token
   */
  static clearTokens(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.TOKEN_EXPIRY_KEY);
    localStorage.removeItem(this.USER_KEY);
  }

  /**
   * 获取token的剩余有效时间（毫秒）
   */
  static getTokenRemainingTime(): number {
    const expiryTime = localStorage.getItem(this.TOKEN_EXPIRY_KEY);
    if (!expiryTime) {
      return 0;
    }
    
    const expiry = parseInt(expiryTime, 10);
    if (isNaN(expiry)) {
      return 0;
    }
    
    const now = Date.now();
    const remainingTime = Math.max(0, expiry - now);
    
    return remainingTime;
  }
  
  /**
   * 保存用户信息
   * @param userData 用户数据
   */
  static saveUserInfo(userData: any): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(userData));
  }
  
  /**
   * 获取用户信息
   */
  static getUserInfo(): any | null {
    const userStr = localStorage.getItem(this.USER_KEY);
    if (!userStr) {
      return null;
    }
    
    try {
      return JSON.parse(userStr);
    } catch (error) {
      console.error('解析用户数据失败', error);
      return null;
    }
  }
}