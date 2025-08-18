import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import ThirdPartyCallback from './views/login/ThirdPartyCallback';
import { TokenManager } from './services/tokenManager';

/**
 * 第三方登录路由配置
 * 这个文件包含所有第三方登录的通用回调路由
 * 需要在主应用的路由配置中添加这个路由
 */
const handleLogin = (userData: any, token: string) => {
  // 保存用户信息
  TokenManager.saveUserInfo(userData);
};

const ThirdPartyRoutes = [
  {
    path: "/oauth2/callback",
    element: <ThirdPartyCallback onLogin={handleLogin} />
  }
];

export default ThirdPartyRoutes; 