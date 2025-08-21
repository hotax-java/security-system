/**
 * PKCE (Proof Key for Code Exchange) 工具类
 * 用于OAuth2授权码流程的PKCE扩展
 */
export class PkceUtils {
  // 本地存储key
  private static readonly STORAGE_KEY = 'pkce_code_verifier';

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
   * 准备PKCE参数用于授权请求
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
   * 存储PKCE参数到会话存储
   * @param codeVerifier code_verifier值
   */
  static storeCodeVerifier(codeVerifier: string): void {
    sessionStorage.setItem(this.STORAGE_KEY, codeVerifier);
  }

  /**
   * 获取存储的code_verifier
   * @returns 存储的code_verifier值，如果不存在返回null
   */
  static getStoredCodeVerifier(): string | null {
    return sessionStorage.getItem(this.STORAGE_KEY);
  }

  /**
   * 清除存储的PKCE参数
   */
  static clearCodeVerifier(): void {
    sessionStorage.removeItem(this.STORAGE_KEY);
  }
} 