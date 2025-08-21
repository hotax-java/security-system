import businessApi from '../apis/api';

/**
 * 认证配置信息接口
 */
export interface AuthConfig {
  enablePkce: boolean;
  // 未来可能的其他配置项
  // supportedPlatforms?: string[];
  // otherConfig?: string;
}

/**
 * 认证配置服务
 * 用于获取和缓存后端认证配置
 */
export class AuthConfigService {
  private static config: AuthConfig | null = null;
  
  /**
   * 获取认证配置
   * @returns 认证配置对象
   */
  static async getConfig(): Promise<AuthConfig> {
    if (this.config) return this.config;
    
    try {
      // 从后端获取配置
      const response = await businessApi.get('/api/oauth2/config');
      this.config = response.data;
      return this.config || { enablePkce: false };
    } catch (error) {
      console.error('获取认证配置失败:', error);
      // 提供默认值
      return {
        enablePkce: false
      };
    }
  }
  
  /**
   * 检查是否启用PKCE
   * @returns 是否启用PKCE的Promise
   */
  static async isPkceEnabled(): Promise<boolean> {
    const config = await this.getConfig();
    return config.enablePkce === true;
  }
  
  /**
   * 清除缓存的配置
   */
  static clearCache(): void {
    this.config = null;
  }
} 