/**
 * PKCE参数接口
 */
interface PkceParams {
  codeVerifier: string;
  codeChallenge: string;
  codeChallengeMethod: string;
}

/**
 * PKCE (Proof Key for Code Exchange) 工具类
 * 用于OAuth2授权码流程的PKCE扩展
 * 使用state作为key来安全存储PKCE参数
 */
export class PkceUtils {
  // localStorage存储前缀
  private static readonly STORAGE_PREFIX = 'pkce_params_';

  /**
   * 生成随机字符串
   * @param length 长度，默认为32字符
   * @returns 生成的随机字符串
   */
  static generateRandomString(length = 32): string {
    const charset = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    const randomValues = new Uint8Array(length);
    window.crypto.getRandomValues(randomValues);
    
    for (let i = 0; i < length; i++) {
      result += charset[randomValues[i] % charset.length];
    }
    return result;
  }

  /**
   * 生成随机的code_verifier
   * @param length 长度，默认为64字符（推荐43-128字符）
   * @returns 生成的code_verifier
   */
  static generateCodeVerifier(length = 64): string {
    const charset = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';
    let result = '';
    const randomValues = new Uint8Array(length);
    window.crypto.getRandomValues(randomValues);
    
    for (let i = 0; i < length; i++) {
      result += charset[randomValues[i] % charset.length];
    }
    return result;
  }

  /**
   * 使用SHA-256算法计算code_challenge
   * @param codeVerifier 需要计算的code_verifier
   * @returns 生成的code_challenge（Base64-URL编码）
   */
  static async generateCodeChallenge(codeVerifier: string): Promise<string> {
    // 将字符串转换为UTF-8字节数组
    const encoder = new TextEncoder();
    const data = encoder.encode(codeVerifier);
    
    // 使用SHA-256哈希
    const hashBuffer = await window.crypto.subtle.digest('SHA-256', data);
    
    // 转换为Base64URL编码
    return this.base64UrlEncode(hashBuffer);
  }

  /**
   * 将ArrayBuffer转换为Base64-URL编码字符串
   */
  private static base64UrlEncode(buffer: ArrayBuffer): string {
    // 转换ArrayBuffer为Uint8Array
    const bytes = new Uint8Array(buffer);
    
    // 将字节数组转换为字符串
    let binary = '';
    for (let i = 0; i < bytes.byteLength; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    
    // Base64编码
    const base64 = window.btoa(binary);
    
    // 转换为Base64URL格式（替换+为-，/为_，去除=）
    return base64
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  }

  /**
   * 生成完整的PKCE参数并存储到localStorage
   * @param state 用作存储key的state参数
   * @returns 包含state、code_verifier、code_challenge等完整参数的对象
   */
  static async generateAndStorePkceParams(state?: string): Promise<{
    state: string;
    codeVerifier: string;
    codeChallenge: string;
    codeChallengeMethod: string;
  }> {
    // 如果没有提供state，则生成一个
    const finalState = state || this.generateRandomString(32);
    
    // 生成PKCE参数
    const codeVerifier = this.generateCodeVerifier();
    const codeChallenge = await this.generateCodeChallenge(codeVerifier);
    const codeChallengeMethod = 'S256';
    
    // 构建PKCE参数对象
    const pkceParams: PkceParams = {
      codeVerifier,
      codeChallenge,
      codeChallengeMethod
    };
    
    // 使用state作为key存储到localStorage
    const storageKey = this.STORAGE_PREFIX + finalState;
    localStorage.setItem(storageKey, JSON.stringify(pkceParams));
    
    console.log('PKCE参数已生成并存储:', {
      state: finalState,
      storageKey,
      codeChallenge,
      codeChallengeMethod
    });
    
    return {
      state: finalState,
      codeVerifier,
      codeChallenge,
      codeChallengeMethod
    };
  }

  /**
   * 通过state获取存储的PKCE参数
   * @param state state参数
   * @returns 对应的PKCE参数，如果不存在返回null
   */
  static getPkceParamsByState(state: string): PkceParams | null {
    if (!state) {
      console.warn('getPkceParamsByState: state参数为空');
      return null;
    }
    
    const storageKey = this.STORAGE_PREFIX + state;
    const storedData = localStorage.getItem(storageKey);
    
    if (!storedData) {
      console.warn('getPkceParamsByState: 未找到对应的PKCE参数', { state, storageKey });
      return null;
    }
    
    try {
      const pkceParams = JSON.parse(storedData) as PkceParams;
      console.log('成功获取PKCE参数:', { state, pkceParams });
      return pkceParams;
    } catch (error) {
      console.error('getPkceParamsByState: 解析PKCE参数失败', error);
      return null;
    }
  }

  /**
   * 通过state获取code_verifier
   * @param state state参数
   * @returns 对应的code_verifier，如果不存在返回null
   */
  static getCodeVerifierByState(state: string): string | null {
    const pkceParams = this.getPkceParamsByState(state);
    return pkceParams ? pkceParams.codeVerifier : null;
  }

  /**
   * 清除指定state对应的PKCE参数
   * @param state state参数
   */
  static clearPkceParamsByState(state: string): void {
    if (!state) {
      console.warn('clearPkceParamsByState: state参数为空');
      return;
    }
    
    const storageKey = this.STORAGE_PREFIX + state;
    localStorage.removeItem(storageKey);
    console.log('已清除PKCE参数:', { state, storageKey });
  }

  /**
   * 清除所有PKCE参数
   */
  static clearAllPkceParams(): void {
    const keys = Object.keys(localStorage);
    const pkceKeys = keys.filter(key => key.startsWith(this.STORAGE_PREFIX));
    
    pkceKeys.forEach(key => {
      localStorage.removeItem(key);
    });
    
    console.log('已清除所有PKCE参数:', pkceKeys);
  }

  // ===== 兼容性方法（保持向后兼容） =====

  /**
   * 准备PKCE参数用于授权请求（兼容性方法）
   * @returns 包含code_verifier和code_challenge的Promise
   */
  static async preparePkceParams(): Promise<{
    codeVerifier: string;
    codeChallenge: string;
    codeChallengeMethod: string;
  }> {
    const codeVerifier = this.generateCodeVerifier();
    const codeChallenge = await this.generateCodeChallenge(codeVerifier);
    
    return {
      codeVerifier,
      codeChallenge,
      codeChallengeMethod: 'S256'
    };
  }

  /**
   * 获取存储的code_verifier（兼容性方法）
   * @returns 存储的code_verifier值，如果不存在返回null
   */
  static getStoredCodeVerifier(): string | null {
    // 尝试从旧的存储方式获取
    const oldCodeVerifier = localStorage.getItem('pkce_code_verifier');
    if (oldCodeVerifier) {
      return oldCodeVerifier;
    }
    
    // 尝试从新的存储方式获取（需要state）
    const oldState = localStorage.getItem('pkce_state');
    if (oldState) {
      return this.getCodeVerifierByState(oldState);
    }
    
    console.warn('getStoredCodeVerifier: 未找到code_verifier，请使用getCodeVerifierByState方法');
    return null;
  }

  /**
   * 清除存储的PKCE参数（兼容性方法）
   */
  static clearCodeVerifier(): void {
    // 清除旧的存储方式
    localStorage.removeItem('pkce_code_verifier');
    localStorage.removeItem('pkce_state');
    
    // 清除所有新的存储方式
    this.clearAllPkceParams();
  }

  /**
   * 清理localStorage中的PKCE参数（兼容性方法）
   */
  static clearLocalStoragePkceParams(): void {
    this.clearCodeVerifier();
  }

  // ===== 构建授权URL的辅助方法 =====

  /**
   * 构建第三方授权URL
   * @param platform 平台名称
   * @param clientId 客户端ID
   * @param redirectUri 重定向URI
   * @param scope 授权范围
   * @param codeChallenge code_challenge
   * @param state state参数
   * @returns 完整的授权URL
   */
  static buildAuthUrl(
    platform: string,
    clientId: string,
    redirectUri: string,
    scope: string,
    codeChallenge: string,
    state: string
  ): string {
    const params = new URLSearchParams({
      client_id: clientId,
      redirect_uri: redirectUri,
      scope: scope,
      response_type: 'code',
      state: state,
      code_challenge: codeChallenge,
      code_challenge_method: 'S256'
    });
    
    return `http://localhost:9000/oauth2/${platform}/authorize?${params.toString()}`;
  }
}