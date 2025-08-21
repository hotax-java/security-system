

/**
 * 认证配置服务
 * 用于获取和缓存后端认证配置
 */
export class AuthConfigService {

  /**
   * 检查是否启用PKCE
   * @returns 是否启用PKCE的Promise
   */
  static async isPkceEnabled(): Promise<boolean> {
    return process.env.REACT_APP_ENABLE_PKCE === 'true';
  }
} 