import { STORAGE_KEYS } from '../config/authConfig';

// Token响应类型
export interface TokenResponse {
  access_token: string;
  token_type: string;
  expires_in: number;
  refresh_token?: string;
  scope?: string;
  id_token?: string;
}

// Token存储类型
export interface TokenStorage extends TokenResponse {
  expires_at: number; // 过期时间戳
}

// 保存tokens到localStorage
export const saveTokens = (tokens: TokenResponse): void => {
  const tokenStorage: TokenStorage = {
    ...tokens,
    expires_at: Date.now() + (tokens.expires_in * 1000)
  };
  
  localStorage.setItem(STORAGE_KEYS.ALL_TOKENS, JSON.stringify(tokenStorage));
};

// 从localStorage获取tokens
export const getTokens = (): TokenStorage | null => {
  const tokensJson = localStorage.getItem(STORAGE_KEYS.ALL_TOKENS);
  if (!tokensJson) return null;
  
  try {
    return JSON.parse(tokensJson) as TokenStorage;
  } catch (e) {
    console.error('Token解析失败:', e);
    return null;
  }
};

// 获取访问令牌
export const getAccessToken = (): string | null => {
  const tokens = getTokens();
  return tokens?.access_token || null;
};

// 获取刷新令牌
export const getRefreshToken = (): string | null => {
  const tokens = getTokens();
  return tokens?.refresh_token || null;
};

// 检查访问令牌是否过期
export const isTokenExpired = (): boolean => {
  const tokens = getTokens();
  if (!tokens) return true;
  
  // 提前5分钟视为过期，给刷新留出时间
  const bufferTime = 5 * 60 * 1000; // 5分钟
  return Date.now() + bufferTime > tokens.expires_at;
};

// 清除所有令牌
export const clearTokens = (): void => {
  localStorage.removeItem(STORAGE_KEYS.ALL_TOKENS);
};

// 导出默认对象
export default {
  saveTokens,
  getTokens,
  getAccessToken,
  getRefreshToken,
  isTokenExpired,
  clearTokens
}; 