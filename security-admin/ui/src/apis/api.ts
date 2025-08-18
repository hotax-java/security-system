import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import { authService } from '../services/authService';
import { TokenManager } from '../services/tokenManager';

// ===== API实例创建和配置 =====
/**
 * 认证API实例 (authApi)
 * 
 * 用途：专门处理认证相关的请求（登录、登出、刷新token等）
 * 特点：
 * 1. 直接对接SSO服务进行用户认证
 * 2. 不包含token自动刷新逻辑，避免循环依赖
 * 3. 标记请求为_isAuthApi=true，让业务API实例识别并跳过token刷新
 */
export const authApi: AxiosInstance = axios.create({
  baseURL: process.env.REACT_APP_AUTH_BASE_URL || 'http://localhost:9000',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'X-Client-Id': 'webapp-client'  // SSO服务需要的客户端ID
  },
});

// 为authApi的请求添加标识，避免触发token刷新逻辑
authApi.interceptors.request.use((config: any) => {
  config._isAuthApi = true;  // 标记为认证API请求
  return config;
});

/**
 * 业务API实例 (businessApi)
 * 
 * 用途：处理所有业务相关的请求（用户管理、角色管理、权限管理等）
 * 特点：
 * 1. 自动添加Authorization头
 * 2. 处理401错误并自动刷新token
 * 3. 提供无感知的用户体验
 */
export const businessApi: AxiosInstance = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || 'http://localhost:9001',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// ===== 请求拦截器配置 =====
/**
 * 请求拦截器：自动添加Authorization头
 * 检查token是否有效，无效则尝试刷新
 */
const requestInterceptor = async (config: any) => {
  // 如果是认证API的请求，直接返回配置
  if (config._isAuthApi) {
    return config;
  }
  
  // 获取有效token (如果已过期则为null)
  let validToken = TokenManager.getValidAccessToken();
  
  // 如果没有有效token，尝试刷新
  if (!validToken) {
    try {
      console.log('No valid token available, refreshing token before proceeding with request');
      // 等待刷新token完成
      await authService.refreshToken();
      
      // 获取刷新后的token
      validToken = TokenManager.getValidAccessToken();
      
      // 如果刷新后仍然没有有效token，则可能是刷新失败
      if (!validToken) {
        throw new Error('Failed to obtain valid token after refresh');
      }
    } catch (error) {
      // 如果刷新失败，重定向到登录页
      console.error('Failed to refresh token in request interceptor:', error);
      TokenManager.clearTokens();
      window.location.replace('/login');
      return Promise.reject(error);
    }
  }
  
  // 添加token到请求头
  config.headers.Authorization = `Bearer ${validToken}`;

  // 返回配置继续请求
  return config;
};

/**
 * 响应拦截器：处理返回数据
 */
const responseInterceptor = (response: any) => {  
  // 检查token是否即将过期，在后台刷新
  if (TokenManager.isTokenExpiringSoon()) {
    console.log('Token expiring soon, refreshing in background after successful response');
    // 不阻塞响应处理，在后台刷新token
    authService.refreshToken().catch((error) => {
      console.error('Background token refresh after response failed:', error);
      // 错误已在refreshToken内部处理
    });
  }

  // 检查HTTP状态码
  if(response.status !== 200) {
    const error = new Error(response.data?.message || `HTTP错误: ${response.status}`);
    return Promise.reject(error);
  }

  // 检查响应数据中的code字段
  if (response.data && typeof response.data === 'object') {
    // 检查是否有code字段
    if ('code' in response.data) {
      if (response.data.code !== 'success') {
        // 如果code不是success，抛出业务逻辑错误
        const error = new Error(response.data.message || '操作失败');
        // 添加额外信息以便区分业务逻辑错误
        (error as any).isBusinessError = true;
        (error as any).code = response.data.code;
        return Promise.reject(error);
      }
      
      // 成功情况下，直接返回data字段内容
      return response.data.data;
    }
  }
  
  // 如果响应数据不符合预期格式，直接返回原始响应数据
  return response.data;
};

/**
 * 响应错误拦截器：处理401错误并自动刷新token
 */
const responseErrorInterceptor = async (error: any) => {
  // 获取原始请求配置
  const originalRequest = error.config;
  
  // 处理HTTP错误
  if (error.response) {
    const { status } = error.response;

    // 处理401错误 - 未授权
    if (status === 401) {
      // 如果是认证API的请求，不进行刷新尝试
      if (originalRequest._isAuthApi) {
        console.log('Auth API request unauthorized (401), redirecting to login page');
        TokenManager.clearTokens();
        window.location.replace('/login'); // 使用replace防止历史记录问题
        return Promise.reject(error);
      }
      
      // 如果请求已经重试过一次，不要继续刷新，直接清除token并跳转登录
      if (originalRequest._retry) {
        console.log('已重试过请求但仍失败(401)，清除token并跳转登录');
        TokenManager.clearTokens();
        window.location.replace('/login'); // 使用replace防止历史记录问题
        return Promise.reject(error);
      }
      
      console.log('Request unauthorized (401), attempting to refresh token');
      
      // 标记请求已经重试过，避免无限循环
      originalRequest._retry = true;
      
      try {
        // 等待刷新完成
        await authService.refreshToken();
        
        // 更新失败请求的Authorization头，总是使用最新的token
        const newToken = TokenManager.getAccessToken(); // 从TokenManager获取刷新后的token
        if (!newToken) {
          throw new Error('Failed to get new token after refresh');
        }
        
        // 设置新的认证头
        originalRequest.headers.Authorization = `Bearer ${newToken}`;

        // 重新发送原始请求
        return axios(originalRequest);
      } catch (refreshError) {
        console.error('Token refresh failed during 401 handling:', refreshError);
        
        // 如果刷新token失败，清除token并跳转到登录页
        TokenManager.clearTokens();
        window.location.replace('/login'); // 使用replace防止历史记录问题
        
        return Promise.reject(refreshError);
      }
    } 
    // 处理403错误 - 权限不足
    else if (status === 403) {
      console.log('Access forbidden (403): 权限不足');
      
      // 获取错误信息
      const errorMsg = error.response?.data?.message || '您没有权限执行此操作';
      
      // 显示友好的错误提示，但不跳转登录页
      // 使用antd全局消息提示
      const { message } = await import('antd');
      message.error({
        content: errorMsg,
        duration: 3,
        style: {
          marginTop: '20vh',
        }
      });
      
      // 返回被拒绝的Promise，让调用者可以处理
      return Promise.reject({
        ...error,
        handledByInterceptor: true, // 标记为已处理
        errorType: 'permission_denied' // 权限错误类型
      });
    }
    // 处理400错误 - 可能是因为token问题
    else if (status === 400) {
      console.log('Bad request (400) - may be due to token issues:', error);
      
      // 如果错误消息表明与token相关，可以尝试刷新或直接登出
      const errorMessage = error.response?.data?.message || '';
      if (errorMessage.toLowerCase().includes('token') || 
          errorMessage.toLowerCase().includes('auth') || 
          errorMessage.toLowerCase().includes('unauthorized')) {
                  console.log('Token可能无效，尝试清除并跳转登录页');
          TokenManager.clearTokens();
          window.location.replace('/login'); // 使用replace防止历史记录问题
      }
    }
    // 处理404错误 - 资源不存在
    else if (status === 404) {
      console.log('Resource not found (404)');
    }
    // 处理500错误 - 服务器错误
    else if (status >= 500) {
      console.log('Server error:', status);
    }
  } else if (error.request) {
    // 请求已发送但没有收到响应
    console.log('No response received:', error.request);
  } else {
    // 请求配置出错
    console.log('Request error:', error.message);
    
    // 检查是否与token相关的错误
    if (error.code === 'ERR_BAD_REQUEST' && !error._tokenErrorHandled) {
      console.log('ERR_BAD_REQUEST可能是token问题，尝试刷新token后重试一次');
      error._tokenErrorHandled = true;
      
      // 获取原始请求
      const originalRequest = error.config;
      
      // 避免无限循环
      if (!originalRequest._badRequestRetried) {
        originalRequest._badRequestRetried = true;
        
        try {
          // 尝试刷新token
  
          await authService.refreshToken();
          
          // 使用新token重试
          const newToken = TokenManager.getAccessToken();
          if (newToken) {
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            return axios(originalRequest);
          }
        } catch (refreshError) {
          console.error('Token刷新失败，跳转到登录页:', refreshError);
          TokenManager.clearTokens();
          window.location.replace('/login'); // 使用replace防止历史记录问题
        }
      } else {
        // 如果已经尝试过一次，就清除token并重定向
        console.log('多次尝试失败，可能是token无效，跳转到登录页');
        TokenManager.clearTokens();
        window.location.replace('/login'); // 使用replace防止历史记录问题
      }
    }
  }

  return Promise.reject(error);
};

// 配置业务API的拦截器
businessApi.interceptors.request.use(requestInterceptor);
businessApi.interceptors.response.use(responseInterceptor, responseErrorInterceptor);

// 导出默认的业务API实例
export default businessApi;