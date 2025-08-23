/**
 * 授权配置服务
 * 从环境变量中加载OAuth2配置
 */

export interface OAuth2Config {
  clientId: string;
  redirectUri: string;
  scope: string;
  authServerUrl: string;
  authEndpoint: string;
  tokenEndpoint: string;
}

// 硬编码默认配置（当环境变量不可用时）
const DEFAULT_CONFIG: OAuth2Config = {
  clientId: 'webapp-client',
  redirectUri: `${window.location.origin}/oauth2/callback`,
  scope: 'read write openid profile offline_access',
  authServerUrl: `${process.env.REACT_APP_SSO_BASE_URL}`,
  authEndpoint: '/oauth2/authorize',
  tokenEndpoint: '/oauth2/token'
};

// 从环境变量加载配置
export const getOAuth2Config = (): OAuth2Config => {
  return {
    clientId: process.env.REACT_APP_CLIENT_ID || DEFAULT_CONFIG.clientId,
    redirectUri: process.env.REACT_APP_REDIRECT_URI || DEFAULT_CONFIG.redirectUri,
    scope: process.env.REACT_APP_SCOPE || DEFAULT_CONFIG.scope,
    authServerUrl: process.env.REACT_APP_AUTH_SERVER_URL || DEFAULT_CONFIG.authServerUrl,
    authEndpoint: process.env.REACT_APP_AUTH_ENDPOINT || DEFAULT_CONFIG.authEndpoint,
    tokenEndpoint: process.env.REACT_APP_TOKEN_ENDPOINT || DEFAULT_CONFIG.tokenEndpoint,
  };
};

// 生成完整的授权端点URL
export const getAuthorizationEndpointUrl = (): string => {
  const config = getOAuth2Config();
  return `${config.authServerUrl}${config.authEndpoint}`;
};

// 生成完整的令牌端点URL
export const getTokenEndpointUrl = (): string => {
  const config = getOAuth2Config();
  return `${config.authServerUrl}${config.tokenEndpoint}`;
}; 