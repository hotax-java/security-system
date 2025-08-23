/**
 * PKCE (Proof Key for Code Exchange) 工具函数
 * 实现OAuth2授权码流程中的PKCE扩展
 */
import { getOAuth2Config } from '../services/authConfigService';

// 生成指定长度的随机字符串
export const generateRandomString = (length: number): string => {
  const charset = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';
  let result = '';
  const randomValues = new Uint8Array(length);
  window.crypto.getRandomValues(randomValues);
  
  for (let i = 0; i < length; i++) {
    result += charset.charAt(randomValues[i] % charset.length);
  }
  
  return result;
};

// Base64Url编码函数
export const base64UrlEncode = (arrayBuffer: ArrayBuffer): string => {
  const bytes = new Uint8Array(arrayBuffer);
  // 将Uint8Array转换为字符串
  let binaryString = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binaryString += String.fromCharCode(bytes[i]);
  }
  
  let base64 = window.btoa(binaryString);
  
  return base64
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
};

// 生成code_verifier (43-128字符的随机字符串)
export const generateCodeVerifier = (): string => {
  // 推荐长度为64字符
  return generateRandomString(64);
};

// 使用SHA-256生成code_challenge
export const generateCodeChallenge = async (codeVerifier: string): Promise<string> => {
  const encoder = new TextEncoder();
  const data = encoder.encode(codeVerifier);
  const hashBuffer = await window.crypto.subtle.digest('SHA-256', data);
  
  return base64UrlEncode(hashBuffer);
};

// PKCE相关的Session Storage操作
export const PKCE_STORAGE_KEY = 'pkce_code_verifier';

export const storePkceCodeVerifier = (codeVerifier: string): void => {
  sessionStorage.setItem(PKCE_STORAGE_KEY, codeVerifier);
};

export const getPkceCodeVerifier = (): string | null => {
  return sessionStorage.getItem(PKCE_STORAGE_KEY);
};

export const removePkceCodeVerifier = (): void => {
  sessionStorage.removeItem(PKCE_STORAGE_KEY);
};

// 生成完整的授权URL (包含PKCE参数)
export const generateAuthorizationUrl = async (
  state: string = generateRandomString(16)
): Promise<{url: string, codeVerifier: string}> => {
  const config = getOAuth2Config();
  const codeVerifier = generateCodeVerifier();
  const codeChallenge = await generateCodeChallenge(codeVerifier);
  
  const params = new URLSearchParams({
    response_type: 'code',
    client_id: config.clientId,
    redirect_uri: config.redirectUri,
    scope: config.scope,
    state: state,
    code_challenge: codeChallenge,
    code_challenge_method: 'S256',
    access_type: 'offline'  // 确保返回refresh_token
  });
  
  return {
    url: `${config.authServerUrl}${config.authEndpoint}?${params.toString()}`,
    codeVerifier
  };
}; 