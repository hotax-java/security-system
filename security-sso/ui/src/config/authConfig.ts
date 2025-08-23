/**
 * OAuth2授权配置
 */
export interface OAuth2Config {
  clientId: string;
  authServerUrl: string;
  authorizationEndpoint: string;
  tokenEndpoint: string;
  scope: string;
}

// 测试环境配置
export const testOAuth2Config: OAuth2Config = {
  clientId: 'webapp-client',
  authServerUrl: `${process.env.REACT_APP_SSO_BASE_URL}`,
  authorizationEndpoint: '/oauth2/authorize',
  tokenEndpoint: '/oauth2/token',
  scope: 'read write openid profile offline_access'
};

// 获取应用环境配置
export const getOAuth2Config = (): OAuth2Config => {
  return testOAuth2Config;
};

// 构建测试重定向URI
export const getTestRedirectUri = (path: string = 'oauth2-callback'): string => {
  return `${window.location.origin}/test/${path}`;
};

// 获取授权状态的本地存储键
export const STORAGE_KEYS = {
  PKCE_CODE_VERIFIER: 'pkce_code_verifier',
  ACCESS_TOKEN: 'oauth2_access_token',
  REFRESH_TOKEN: 'oauth2_refresh_token',
  ID_TOKEN: 'oauth2_id_token',
  TOKEN_EXPIRY: 'oauth2_token_expiry',
  ALL_TOKENS: 'oauth2_tokens'
};

export default {
  getOAuth2Config,
  getTestRedirectUri,
  STORAGE_KEYS
}; 