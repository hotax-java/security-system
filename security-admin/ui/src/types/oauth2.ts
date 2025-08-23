/**
 * OAuth2相关类型定义
 */

// PKCE参数类型
export interface PkceParams {
  codeVerifier: string;
  codeChallenge: string;
  codeChallengeMethod: string;
}

// Token请求参数类型
export interface TokenRequestParams {
  grant_type: string;
  code: string;
  code_verifier?: string;
  client_id: string;
  redirect_uri: string;
}

// Token响应类型
export interface TokenResponse {
  access_token: string;
  token_type: string;
  expires_in: number;
  refresh_token: string;
  scope: string;
  id_token?: string;
}

// OAuth2错误类型
export interface OAuth2Error {
  error: string;
  error_description?: string;
  error_uri?: string;
}

// 授权URL参数类型
export interface AuthorizationUrlParams {
  response_type: string;
  client_id: string;
  scope: string;
  redirect_uri: string;
  state?: string;
  code_challenge?: string;
  code_challenge_method?: string;
  access_type?: string;
}