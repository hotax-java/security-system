import axios from 'axios';
import { TokenResponse, TokenRequestParams, OAuth2Error } from '../types/oauth2';

// 创建专用的OAuth2请求实例
const oauth2Api = axios.create({
  baseURL: process.env.REACT_APP_AUTH_BASE_URL || 'http://localhost:9000',
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded',
  },
});

// 请求参数序列化器 (处理form-urlencoded格式)
const paramsSerializer = (params: any): string => {
  return Object.keys(params)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&');
};

/**
 * OAuth2服务模块
 * 专门处理PKCE授权码流程
 */
export const oauth2Service = {
  /**
   * 使用授权码换取访问令牌
   * @param params Token请求参数
   * @returns Promise<TokenResponse>
   */
  exchangeCodeForToken: async (params: TokenRequestParams): Promise<TokenResponse> => {
    try {
      const response = await oauth2Api.post('/oauth2/token', params, {
        paramsSerializer,
      });
      return response.data;
    } catch (error: any) {
      if (error.response?.data) {
        const oauthError = error.response.data as OAuth2Error;
        throw new Error(oauthError.error_description || oauthError.error || 'Token交换失败');
      }
      throw new Error('网络错误，无法连接到认证服务器');
    }
  },

  /**
   * 构建授权URL
   * @param params 授权URL参数
   * @returns 完整的授权URL
   */
  buildAuthorizationUrl: (params: {
    client_id: string;
    redirect_uri: string;
    scope: string;
    code_challenge: string;
    state?: string;
  }): string => {
    const baseUrl = process.env.REACT_APP_AUTH_BASE_URL || 'http://localhost:9000';
    const queryParams = new URLSearchParams({
      response_type: 'code',
      client_id: params.client_id,
      scope: params.scope,
      redirect_uri: params.redirect_uri,
      code_challenge: params.code_challenge,
      code_challenge_method: 'S256',
      access_type: 'offline',
      ...(params.state && { state: params.state }),
    });
    
    return `${baseUrl}/oauth2/authorize?${queryParams.toString()}`;
  },

  /**
   * 生成随机状态参数
   * @returns 随机状态字符串
   */
  generateState: (): string => {
    return Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
  },
};