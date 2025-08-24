/**
 * 登录配置管理
 * 统一管理登录相关的配置，支持SSO开关控制
 */

export interface LoginConfig {
  // SSO相关配置
  enableSSO: boolean;                    // 是否启用SSO登录
  ssoBaseUrl: string;                   // SSO服务器地址
  
  // OAuth2/PKCE相关配置
  enablePKCE: boolean;                  // 是否启用PKCE
  clientId: string;                     // OAuth2客户端ID
  redirectUri: string;                  // 回调地址
  scope: string;                        // 授权范围
  codeChallengeMethod: string;          // PKCE挑战方法
  
  // 业务API配置
  apiBaseUrl: string;                   // 业务API地址
  
  // 登录页面配置
  enableThirdPartyLogin: boolean;       // 是否显示第三方登录按钮
  supportedPlatforms: string[];         // 支持的第三方平台
  
  // 路由守卫配置
  bypassRoutes: string[];               // 绕过全局守卫的路由列表
  currentAppUrl: string;                // 当前应用的URL地址
}

/**
 * 默认配置
 */
const DEFAULT_CONFIG: LoginConfig = {
  enableSSO: false,
  ssoBaseUrl: 'http://localhost:9000',
  enablePKCE: false,
  clientId: 'webapp-client',
  redirectUri: 'http://localhost:3000/callback',
  scope: 'read write openid profile offline_access',
  codeChallengeMethod: 'S256',
  apiBaseUrl: 'http://localhost:9001',
  enableThirdPartyLogin: true,
  supportedPlatforms: ['wechat', 'github', 'alipay'],
  bypassRoutes: ['/oauth2/callback', '/error'],
  currentAppUrl: 'http://localhost:8081'
};

/**
 * 登录配置管理类
 */
export class LoginConfigManager {
  private static config: LoginConfig | null = null;
  
  /**
   * 获取登录配置
   * 优先级：环境变量 > 默认配置
   */
  static getConfig(): LoginConfig {
    if (this.config) {
      return this.config;
    }
    
    // 从环境变量读取配置
    this.config = {
      // SSO配置
      enableSSO: process.env.REACT_APP_ENABLE_SSO === 'true',
      ssoBaseUrl: process.env.REACT_APP_SSO_BASE_URL || DEFAULT_CONFIG.ssoBaseUrl,
      
      // OAuth2/PKCE配置
      enablePKCE: process.env.REACT_APP_ENABLE_PKCE === 'true',
      clientId: process.env.REACT_APP_OAUTH2_CLIENT_ID || DEFAULT_CONFIG.clientId,
      redirectUri: process.env.REACT_APP_OAUTH2_REDIRECT_URI || DEFAULT_CONFIG.redirectUri,
      scope: process.env.REACT_APP_OAUTH2_SCOPE || DEFAULT_CONFIG.scope,
      codeChallengeMethod: process.env.REACT_APP_OAUTH2_CODE_CHALLENGE_METHOD || DEFAULT_CONFIG.codeChallengeMethod,
      
      // API配置
      apiBaseUrl: process.env.REACT_APP_API_BASE_URL || DEFAULT_CONFIG.apiBaseUrl,
      
      // 第三方登录配置
      enableThirdPartyLogin: process.env.REACT_APP_ENABLE_THIRD_PARTY_LOGIN !== 'false', // 默认启用
      supportedPlatforms: process.env.REACT_APP_SUPPORTED_PLATFORMS 
        ? process.env.REACT_APP_SUPPORTED_PLATFORMS.split(',').map(p => p.trim())
        : DEFAULT_CONFIG.supportedPlatforms,
      
      // 路由守卫配置
      bypassRoutes: process.env.REACT_APP_BYPASS_ROUTES
        ? process.env.REACT_APP_BYPASS_ROUTES.split(',').map(r => r.trim())
        : DEFAULT_CONFIG.bypassRoutes,
      currentAppUrl: process.env.REACT_APP_CURRENT_APP_URL || DEFAULT_CONFIG.currentAppUrl
    };
    
    console.log('登录配置已加载:', this.config);
    return this.config;
  }
  
  /**
   * 重新加载配置（用于配置更新后刷新）
   */
  static reloadConfig(): LoginConfig {
    this.config = null;
    return this.getConfig();
  }
  
  /**
   * 检查是否启用SSO
   */
  static isSSO(): boolean {
    return this.getConfig().enableSSO;
  }
  
  /**
   * 检查是否启用PKCE
   */
  static isPKCE(): boolean {
    return this.getConfig().enablePKCE;
  }
  
  /**
   * 获取登录跳转URL
   * @param returnUrl 登录成功后的返回地址
   */
  static getLoginUrl(returnUrl?: string): string {
    const config = this.getConfig();
    
    if (config.enableSSO) {
      // SSO模式：跳转到SSO服务器的登录页面
      const params = new URLSearchParams();
      if (returnUrl) {
        params.set('returnUrl', returnUrl);
      }
      const queryString = params.toString();
      return `${config.ssoBaseUrl}${queryString ? '?' + queryString : ''}`;
    } else {
      // 本地模式：跳转到本地登录页面
      return '/login';
    }
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
    const config = this.getConfig();
    const baseUrl = config.enableSSO ? config.ssoBaseUrl : config.apiBaseUrl;
    
    const params = new URLSearchParams({
      response_type: 'code',
      client_id: config.clientId,
      redirect_uri: config.redirectUri,
      scope: config.scope,
    });
    
    // 添加PKCE参数
    if (config.enablePKCE && pkceParams) {
      params.set('code_challenge', pkceParams.codeChallenge);
      params.set('code_challenge_method', pkceParams.codeChallengeMethod);
      if (pkceParams.state) {
        params.set('state', pkceParams.state);
      }
    }
    
    return `${baseUrl}/oauth2/${platform}/authorize?${params.toString()}`;
  }
  
  /**
   * 获取Token交换URL
   */
  static getTokenUrl(): string {
    const config = this.getConfig();
    const baseUrl = config.enableSSO ? config.ssoBaseUrl : config.apiBaseUrl;
    return `${baseUrl}/api/oauth2/token`;
  }
  
  /**
   * 检查平台是否支持
   * @param platform 平台名称
   */
  static isPlatformSupported(platform: string): boolean {
    return this.getConfig().supportedPlatforms.includes(platform);
  }
  
  /**
   * 获取支持的平台列表
   */
  static getSupportedPlatforms(): string[] {
    return this.getConfig().supportedPlatforms;
  }
  
  /**
   * 获取绕过路由列表
   */
  static getBypassRoutes(): string[] {
    return this.getConfig().bypassRoutes;
  }
  
  /**
   * 获取当前应用URL
   */
  static getCurrentAppUrl(): string {
    return this.getConfig().currentAppUrl;
  }
  
  /**
   * 打印当前配置（用于调试）
   */
  static debugConfig(): void {
    console.group('🔧 登录配置信息');
    const config = this.getConfig();
    console.log('SSO模式:', config.enableSSO ? '✅ 启用' : '❌ 禁用');
    console.log('PKCE模式:', config.enablePKCE ? '✅ 启用' : '❌ 禁用');
    console.log('SSO地址:', config.ssoBaseUrl);
    console.log('API地址:', config.apiBaseUrl);
    console.log('客户端ID:', config.clientId);
    console.log('回调地址:', config.redirectUri);
    console.log('授权范围:', config.scope);
    console.log('第三方登录:', config.enableThirdPartyLogin ? '✅ 启用' : '❌ 禁用');
    console.log('支持平台:', config.supportedPlatforms.join(', '));
    console.log('绕过路由:', config.bypassRoutes.join(', '));
    console.log('当前应用URL:', config.currentAppUrl);
    console.groupEnd();
  }
}

// 导出配置实例
export const loginConfig = LoginConfigManager.getConfig();

// 开发环境下自动打印配置
if (process.env.NODE_ENV === 'development') {
  LoginConfigManager.debugConfig();
}