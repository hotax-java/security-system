import { authApi } from '../apis/api';
import { TokenManager } from './tokenManager';

/**
 * 认证服务模块
 * 
 * 职责：
 * 1. 封装所有认证相关的API调用
 * 2. 管理token的存储和获取
 * 3. 提供用户认证状态的判断
 * 4. 处理登录、登出、token刷新等核心认证流程
 */

// 使用Promise单例模式，确保同一时间只有一个刷新token的请求
let refreshTokenPromise: Promise<any> | null = null;

export const authService = {
  /**
   * 用户登录
   * 
   * @param username 用户名
   * @param password 密码
   * @returns Promise<any> 登录响应数据
   * 
   * 执行流程：
   * 1. 直接调用SSO服务的登录接口
   * 2. 获取access_token和refresh_token
   * 3. 将token保存到localStorage
   * 4. 返回完整的响应数据供组件使用
   */
  login: async (username: string, password: string) => {
    const response = await authApi.post('/oauth2/login', {
      username,
      password
    });

    console.log('Login response:', response.data);

    // 提取token信息
    const { access_token, refresh_token } = response.data;
    
    // 解析过期时间，确保它是一个数字
    let expires_in = response.data.expires_in;
    if (typeof expires_in === 'string') {
      expires_in = parseInt(expires_in, 10);
    }
    

    
    // 使用TokenManager保存token信息
    if (access_token) {
      TokenManager.saveTokens(access_token, refresh_token, expires_in);
    } else {
      console.error('Login response missing access_token');
    }

    return response.data;
  },

  /**
   * 刷新访问令牌 - 单例模式实现
   * 
   * @param refreshToken 可选的刷新令牌，如果不提供则从localStorage获取
   * @returns Promise<any> 刷新token的Promise
   * 
   * 执行流程：
   * 1. 检查是否已有刷新请求在进行中，如有则复用现有Promise
   * 2. 获取refresh_token（参数传入或从localStorage读取）
   * 3. 调用SSO服务的刷新接口
   * 4. 通过TokenManager保存新的token信息
   * 5. 无论成功失败，一段时间后清除Promise引用以允许再次刷新
   * 
   * 单例模式好处：
   * 1. 避免多个请求同时触发多次token刷新
   * 2. 所有需要刷新的请求共享同一个刷新结果
   * 3. 减少对认证服务器的请求压力
   * 4. 提高应用性能和用户体验
   */
  refreshToken: async (refreshToken?: string) => {
    // 如果已有刷新请求正在进行中，直接返回该Promise
    if (refreshTokenPromise) {
      console.log('Token refresh already in progress, reusing existing promise');
      return refreshTokenPromise;
    }

    console.log('Starting new token refresh');
    
    // 创建新的刷新Promise并保存到单例变量
    refreshTokenPromise = (async () => {
    try {
      const token = refreshToken || TokenManager.getRefreshToken();
      if (!token) {
        throw new Error('No refresh token available');
      }

      const response = await authApi.post('/oauth2/refresh', {
        refreshToken: token
      });

      // 更新本地存储的token
      const { access_token, refresh_token: newRefreshToken } = response.data;
      
      if (!access_token) {
        throw new Error('Refresh response missing access_token');
      }
      
      // 解析过期时间，确保它是一个数字
      let expires_in = response.data.expires_in;
      if (typeof expires_in === 'string') {
        expires_in = parseInt(expires_in, 10);
      }
      
      // 使用TokenManager保存token信息
      TokenManager.saveTokens(access_token, newRefreshToken, expires_in);
      console.info('Token refresh success, new access token saved');
        
        return response.data;
    } catch (error) {
      console.error('Token refresh failed:', error);
      // 刷新失败，清除token避免继续使用无效token
      TokenManager.clearTokens();
      throw error; // 重新抛出错误，让调用者处理
      } finally {
        // 5秒后清除Promise引用，允许新的刷新请求
        setTimeout(() => {
          refreshTokenPromise = null;
          console.log('Token refresh promise cleared, allowing new refresh');
        }, 5000);
      }
    })();

    return refreshTokenPromise;
  },



  /**
   * 用户登出
   * 
   * @returns Promise<any> 登出响应数据
   * 
   * 执行流程：
   * 1. 获取当前的access_token
   * 2. 直接调用SSO服务的登出接口
   * 3. 清除本地存储的所有token
   * 4. 返回登出结果
   * 
   * 设计考虑：
   * - 即使SSO登出失败，也要清除本地token，确保前端状态正确
   * - 传递access_token给SSO服务，让服务端撤销相关授权
   */
  logout: async () => {
    try {
      // 获取access_token用于服务端注销
      const accessToken = TokenManager.getAccessToken();
      if (accessToken) {
        await authApi.post('/oauth2/logout', {
          accessToken: accessToken
        });
      }
    } catch (error) {
      // 即使SSO登出失败，也要清除本地token
      console.error('Logout API call failed:', error);
    } finally {
      // 清除本地存储的token
      TokenManager.clearTokens();
    }
  },



  // ===== 工具函数 =====
  
  /**
   * 检查用户是否已认证
   * 
   * @returns boolean 是否已认证
   * 
   * 判断逻辑：
   * 1. 使用TokenManager检查用户是否已认证
   * 
   * 注意：这只是前端的简单判断，真正的认证状态需要后端验证
   */
  isAuthenticated: (): boolean => {
    return TokenManager.isAuthenticated();
  },

  /**
   * 获取访问令牌
   * 
   * @returns string | null 访问令牌
   */
  getAccessToken: (): string | null => {
    return TokenManager.getAccessToken();
  },

  /**
   * 获取刷新令牌
   * 
   * @returns string | null 刷新令牌
   */
  getRefreshToken: (): string | null => {
    return TokenManager.getRefreshToken();
  }
};