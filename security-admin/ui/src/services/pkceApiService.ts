/**
 * PKCE API服务
 * 调用后端API生成和获取PKCE参数
 */

import axios from 'axios';
import { LoginConfigManager } from '../config/loginConfig';

export interface PkceParamsResponse {
  state: string;
  codeVerifier: string;
  codeChallenge: string;
  codeChallengeMethod: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

export class PkceApiService {
  private static baseUrl = LoginConfigManager.getConfig().apiBaseUrl;

  /**
   * 生成PKCE参数
   * 调用后端API生成state和PKCE参数，并存储到Redis中
   */
  static async generatePkceParams(): Promise<PkceParamsResponse> {
    try {
      const response = await axios.post<ApiResponse<PkceParamsResponse>>(
        `${this.baseUrl}/api/oauth2/generate-pkce`
      );
      
      if (response.data.success && response.data.data) {
        console.log('成功从后端获取PKCE参数:', response.data.data);
        return response.data.data;
      } else {
        throw new Error(response.data.message || '生成PKCE参数失败');
      }
    } catch (error: any) {
      console.error('调用生成PKCE参数API失败:', error);
      throw new Error(`生成PKCE参数失败: ${error.response?.data?.message || error.message}`);
    }
  }

  /**
   * 根据state获取code_verifier
   * 用于OAuth2回调时获取对应的code_verifier进行token交换
   */
  static async getCodeVerifier(state: string): Promise<string> {
    try {
      const response = await axios.get<ApiResponse<{ codeVerifier: string }>>(
        `${this.baseUrl}/api/oauth2/get-verifier/${state}`
      );
      
      if (response.data.success && response.data.data) {
        console.log('成功获取code_verifier');
        return response.data.data.codeVerifier;
      } else {
        throw new Error(response.data.message || '获取code_verifier失败');
      }
    } catch (error: any) {
      console.error('获取code_verifier失败:', error);
      throw new Error(`获取code_verifier失败: ${error.response?.data?.message || error.message}`);
    }
  }

  /**
   * 验证state参数
   * 检查state参数是否有效
   */
  static async validateState(state: string): Promise<boolean> {
    try {
      const response = await axios.get<ApiResponse<{ valid: boolean }>>(
        `${this.baseUrl}/api/oauth2/validate-state/${state}`
      );
      
      if (response.data.success && response.data.data) {
        return response.data.data.valid;
      } else {
        return false;
      }
    } catch (error: any) {
      console.error('验证state失败:', error);
      return false;
    }
  }

  /**
   * 清理PKCE参数
   * 登录成功后清理Redis中的PKCE参数
   */
  static async cleanupPkceParams(state: string): Promise<void> {
    try {
      await axios.delete(`${this.baseUrl}/api/oauth2/cleanup/${state}`);
      console.log('成功清理PKCE参数');
    } catch (error: any) {
      console.error('清理PKCE参数失败:', error);
      // 清理失败不影响主流程，只记录日志
    }
  }
}