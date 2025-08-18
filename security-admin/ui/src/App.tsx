import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Route, Routes, Navigate } from 'react-router-dom';
import Login from './views/login/Login';
import Dashboard from './views/dashboard/Dashboard';
import ThirdPartyCallback from './views/login/ThirdPartyCallback';
import ErrorPage from './views/login/ErrorPage';
import { TokenManager } from './services/tokenManager';
import './App.css';

const App: React.FC = () => {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [user, setUser] = useState<any>(null);

  useEffect(() => {
    // 检查是否有有效的token
    const isAuth = TokenManager.isAuthenticated();
    const userData = TokenManager.getUserInfo();
    
    setIsAuthenticated(isAuth);
    setUser(userData);
  }, []);

  const handleLogin = (userData: any, token: string) => {
    // 保存用户信息
    TokenManager.saveUserInfo(userData);
    setIsAuthenticated(true);
    setUser(userData);
  };

  const handleLogout = () => {
    // 清除所有令牌和用户信息
    TokenManager.clearTokens();
    setIsAuthenticated(false);
    setUser(null);
  };

  return (
    <Router>
      <div className="App">
        <Routes>
          <Route path="/login" element={
            isAuthenticated ? <Navigate to="/" /> : <Login onLogin={handleLogin} />
          } />
          {/* 第三方登录回调路由 */}
          <Route path="/oauth2/callback" element={
            <ThirdPartyCallback onLogin={handleLogin} />
          } />
          <Route path="/error" element={<ErrorPage />} />
          <Route path="/*" element={
            isAuthenticated ? <Dashboard user={user} onLogout={handleLogout} /> : <Navigate to="/login" />
          } />
        </Routes>
      </div>
    </Router>
  );
};

export default App;