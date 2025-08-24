

import { LoginConfigManager } from '../config/loginConfig';

/**
 * 认证配置服务
 * 用于获取和缓存认证配置，统一管理登录相关配置
 */
export class AuthConfigService {

  /**
   * 检查是否启用PKCE
   * @returns 是否启用PKCE的Promise
   */
  static async isPkceEnabled(): Promise<boolean> {
    return LoginConfigManager.isPKCE();
  }
  
  /**
   * 检查是否启用SSO
   * @returns 是否启用SSO
   */
  static isSSO(): boolean {
    return LoginConfigManager.isSSO();
  }
  
  /**
   * 获取登录跳转URL
   * @param returnUrl 登录成功后的返回地址
   */
  static getLoginUrl(returnUrl?: string): string {
    return LoginConfigManager.getLoginUrl(returnUrl);
  }
  
  /**
   * 获取第三方登录授权URL
   * @param platform 第三方平台名称
   * @param pkceParams PKCE参数（如果启用PKCE）
   */
  static getThirdPartyAuthUrl(platform: string, pkceParams?: {
    codeChallenge: string;
    codeChallengeMethod: string;
    state?: string;
  }): string {
    return LoginConfigManager.getThirdPartyAuthUrl(platform, pkceParams);
  }
  
  /**
   * 获取Token交换URL
   */
  static getTokenUrl(): string {
    return LoginConfigManager.getTokenUrl();
  }
  
  /**
   * 检查平台是否支持
   * @param platform 平台名称
   */
  static isPlatformSupported(platform: string): boolean {
    return LoginConfigManager.isPlatformSupported(platform);
  }
  
  /**
   * 获取支持的平台列表
   */
  static getSupportedPlatforms(): string[] {
    return LoginConfigManager.getSupportedPlatforms();
  }
}