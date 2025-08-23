import React from 'react';
import Callback from './views/login/Callback';
import { TokenManager } from './services/tokenManager';

/**
 * OAuth2授权码登录回调路由配置
 * 处理PKCE模式的授权码登录回调
 */
const handleLogin = (userData: any, token: string) => {
  // 保存用户信息
  TokenManager.saveUserInfo(userData);
};

const OAuth2Routes = [
  {
    path: "/callback",
    element: <Callback onLogin={handleLogin} />
  }
];

export default OAuth2Routes; 