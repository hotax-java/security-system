import React, { useEffect, ReactNode } from 'react';
import { useLocation } from 'react-router-dom';
import { TokenManager } from '../services/tokenManager';
import { LoginConfigManager } from '../config/loginConfig';
import { PkceUtils } from '../utils/pkceUtils';

interface GlobalGuardProps {
  children: ReactNode;
}

/**
 * 全局路由守卫：拦截所有路由，未授权则跳转到 9000 端口的登录页
 */
const GlobalGuard: React.FC<GlobalGuardProps> = ({ children }) => {
  const location = useLocation();

  useEffect(() => {
    const executeGuard = () => {
      const currentPath = location.pathname;
      
      console.log('全局路由守卫检查:', currentPath);
      
      // 从配置文件获取不需要拦截的路径
      const publicPaths = LoginConfigManager.getBypassRoutes();
      const isPublicPath = publicPaths.some(path => currentPath.startsWith(path));
      
      if (isPublicPath) {
        console.log('路由已配置绕过全局守卫:', currentPath);
        return;
      }
      
      // 检查用户是否已认证
      const isAuthenticated = TokenManager.isAuthenticated();
      if (isAuthenticated) {
        console.log('用户已认证，允许访问:', currentPath);
        return;
      }
      
      console.log('用户未认证，跳转到9000端口登录页');
      
      // 构造登录后需要跳转回来的地址（使用配置中的当前应用URL）
      const currentAppUrl = LoginConfigManager.getCurrentAppUrl();
      const redirectUri = encodeURIComponent(`${currentAppUrl}${currentPath}`);
      
      // 检查是否启用PKCE
      const config = LoginConfigManager.getConfig();
      if (config.enablePKCE) {
        // 生成PKCE参数
        PkceUtils.generateAndStorePkceParams().then(pkceParams => {
          // 构建带PKCE参数的登录URL
          const loginUrl = `${config.ssoBaseUrl}/login?redirect=${redirectUri}&state=${pkceParams.state}&code_challenge=${pkceParams.codeChallenge}&code_challenge_method=${pkceParams.codeChallengeMethod}`;
          console.log('跳转到外部登录页（带PKCE参数）:', loginUrl);
          window.location.replace(loginUrl);
        }).catch(error => {
          console.error('生成PKCE参数失败，使用普通登录:', error);
          // 降级处理：不使用PKCE参数
          const loginUrl = `${config.ssoBaseUrl}/login?redirect=${redirectUri}`;
          console.log('跳转到外部登录页（无PKCE）:', loginUrl);
          window.location.replace(loginUrl);
        });
      } else {
        // 不启用PKCE，直接跳转
        const loginUrl = `${config.ssoBaseUrl}/login?redirect=${redirectUri}`;
        console.log('跳转到外部登录页（无PKCE）:', loginUrl);
        window.location.replace(loginUrl);
      }
    };

    executeGuard();
  }, [location.pathname]); // 路由路径变化时触发检查

  // 监听页面可见性变化，当页面重新可见时重新执行检查
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (!document.hidden) {
        console.log('页面重新可见，重新执行全局路由守卫检查');
        const currentPath = location.pathname;
        const publicPaths = LoginConfigManager.getBypassRoutes();
        const isPublicPath = publicPaths.some(path => currentPath.startsWith(path));
        
        if (!isPublicPath && !TokenManager.isAuthenticated()) {
          const currentAppUrl = LoginConfigManager.getCurrentAppUrl();
          const redirectUri = encodeURIComponent(`${currentAppUrl}${currentPath}`);
          const config = LoginConfigManager.getConfig();
          
          if (config.enablePKCE) {
            // 生成PKCE参数
            PkceUtils.generateAndStorePkceParams().then(pkceParams => {
              const loginUrl = `${config.ssoBaseUrl}/login?redirect=${redirectUri}&state=${pkceParams.state}&code_challenge=${pkceParams.codeChallenge}&code_challenge_method=${pkceParams.codeChallengeMethod}`;
              console.log('页面可见性检查：跳转到外部登录页（带PKCE参数）:', loginUrl);
              window.location.replace(loginUrl);
            }).catch(error => {
              console.error('页面可见性检查：生成PKCE参数失败，使用普通登录:', error);
              const loginUrl = `${config.ssoBaseUrl}/login?redirect=${redirectUri}`;
              console.log('页面可见性检查：跳转到外部登录页（无PKCE）:', loginUrl);
              window.location.replace(loginUrl);
            });
          } else {
            const loginUrl = `${config.ssoBaseUrl}/login?redirect=${redirectUri}`;
            console.log('页面可见性检查：跳转到外部登录页（无PKCE）:', loginUrl);
            window.location.replace(loginUrl);
          }
        }
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [location.pathname]);

  return <>{children}</>;
};

export default GlobalGuard;