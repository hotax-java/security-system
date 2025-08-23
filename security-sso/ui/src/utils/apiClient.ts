import axios, { AxiosResponse, AxiosRequestConfig } from 'axios';

// OAuth2 Token请求参数类型
export interface TokenRequestParams {
  grant_type: string;
  code?: string;
  code_verifier?: string;
  client_id: string;
  redirect_uri: string;
  refresh_token?: string;
}

// OAuth2 Token响应类型
export interface TokenResponse {
  access_token: string;
  token_type: string;
  expires_in: number;
  refresh_token?: string;
  scope?: string;
  id_token?: string;
}

// 登录请求参数类型
export interface LoginRequestParams {
  username: string;
  password: string;
}

// OAuth2 错误响应类型
export interface OAuth2Error {
  error: string;
  error_description?: string;
  error_uri?: string;
}

// 创建axios实例
const apiClient = axios.create({
  baseURL: '', // 使用相对路径，不需要指定baseURL
  timeout: 10000,
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded',
  }
});

// 请求拦截器，处理表单编码
apiClient.interceptors.request.use((config) => {
  if (config.method === 'post' && config.data && config.headers['Content-Type'] === 'application/x-www-form-urlencoded') {
    // 将对象转换为URL编码的表单数据
    config.data = new URLSearchParams(config.data).toString();
  }
  return config;
});

// 响应拦截器，处理错误
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.data) {
      return Promise.reject({
        ...error,
        oauth2Error: error.response.data as OAuth2Error
      });
    }
    return Promise.reject(error);
  }
);

// 封装token请求函数
export const requestToken = async (
  params: TokenRequestParams, 
  tokenEndpoint: string = '/oauth2/token'
): Promise<TokenResponse> => {
  try {
    const response: AxiosResponse<TokenResponse> = await apiClient.post(
      tokenEndpoint,
      params
    );
    return response.data;
  } catch (error: any) {
    console.error('Token请求失败:', error);
    throw error.oauth2Error || error;
  }
};

// 封装使用授权码交换token的函数
export const exchangeCodeForToken = async (
  code: string,
  codeVerifier: string,
  clientId: string,
  redirectUri: string,
  tokenEndpoint: string = '/oauth2/token'
): Promise<TokenResponse> => {
  return requestToken({
    grant_type: 'authorization_code',
    code,
    code_verifier: codeVerifier,
    client_id: clientId,
    redirect_uri: redirectUri
  }, tokenEndpoint);
};

// 封装刷新token的函数
export const refreshToken = async (
  refreshToken: string,
  clientId: string,
  tokenEndpoint: string = '/oauth2/token'
): Promise<TokenResponse> => {
  return requestToken({
    grant_type: 'refresh_token',
    refresh_token: refreshToken,
    client_id: clientId,
    redirect_uri: '' // 刷新令牌不需要redirect_uri
  }, tokenEndpoint);
};

// 封装用户名密码登录函数
export const loginWithCredentials = async (
  params: LoginRequestParams,
  loginEndpoint: string = '/login'
): Promise<AxiosResponse> => {
  try {
    // 登录请求特殊处理，允许跟随重定向但最大限制为0（因为我们要自己处理重定向）
    const response = await apiClient.post(
      loginEndpoint,
      params,
      {
        maxRedirects: 0,
        validateStatus: (status) => {
          return status >= 200 && status < 400;
        }
      }
    );
    return response;
  } catch (error: any) {
    console.error('登录请求失败:', error);
    throw error;
  }
};

export default apiClient; 