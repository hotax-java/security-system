/**
 * 令牌存储管理器
 * 用于管理认证令牌的存储、获取和清除
 */
class TokenStoreManager {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly TOKEN_TYPE_KEY = 'auth_token_type';
  private readonly EXPIRES_KEY = 'auth_expires_at';

  /**
   * 设置认证令牌
   * @param token 令牌值
   * @param tokenType 令牌类型，默认为Bearer
   * @param expiresIn 过期时间（秒）
   */
  public setToken(token: string, tokenType: string = 'Bearer', expiresIn?: number): void {
    localStorage.setItem(this.TOKEN_KEY, token);
    localStorage.setItem(this.TOKEN_TYPE_KEY, tokenType);

    if (expiresIn) {
      const expiresAt = new Date().getTime() + expiresIn * 1000;
      localStorage.setItem(this.EXPIRES_KEY, expiresAt.toString());
    }
  }

  /**
   * 获取认证令牌
   * @returns 认证令牌，如果不存在则返回null
   */
  public getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  /**
   * 获取认证令牌类型
   * @returns 认证令牌类型，如果不存在则返回Bearer
   */
  public getTokenType(): string {
    return localStorage.getItem(this.TOKEN_TYPE_KEY) || 'Bearer';
  }

  /**
   * 获取完整的认证头
   * @returns 完整的认证头，格式为"Bearer {token}"
   */
  public getAuthHeader(): string {
    const token = this.getToken();
    const tokenType = this.getTokenType();
    return token ? `${tokenType} ${token}` : '';
  }

  /**
   * 检查令牌是否已过期
   * @returns 如果令牌已过期则返回true，否则返回false
   */
  public isTokenExpired(): boolean {
    const expiresAt = localStorage.getItem(this.EXPIRES_KEY);
    if (!expiresAt) {
      return false; // 没有过期时间，视为未过期
    }

    const now = new Date().getTime();
    return now > parseInt(expiresAt, 10);
  }

  /**
   * 检查是否已认证
   * @returns 如果已认证则返回true，否则返回false
   */
  public isAuthenticated(): boolean {
    const token = this.getToken();
    return !!token && !this.isTokenExpired();
  }

  /**
   * 清除认证令牌
   */
  public clearToken(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.TOKEN_TYPE_KEY);
    localStorage.removeItem(this.EXPIRES_KEY);
  }
}

export const tokenStoreManager = new TokenStoreManager();