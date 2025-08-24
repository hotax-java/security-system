import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Route, Routes, Navigate } from 'react-router-dom';
import Login from './views/login/Login';
import Dashboard from './views/dashboard/Dashboard';
import Callback from './views/login/Callback';
import ErrorPage from './views/login/ErrorPage';
import GlobalGuard from './components/GlobalGuard';
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
      <GlobalGuard>
        <div className="App">
          <Routes>
            <Route path="/login" element={
              isAuthenticated ? <Navigate to="/" /> : <Login onLogin={handleLogin} />
            } />
            {/* OAuth2授权码登录回调路由 */}
            <Route path="/oauth2/callback" element={
              <Callback onLogin={handleLogin} />
            } />
            <Route path="/error" element={<ErrorPage />} />
            <Route path="/*" element={
              isAuthenticated ? <Dashboard user={user} onLogout={handleLogout} /> : <div>Loading...</div>
            } />
          </Routes>
        </div>
      </GlobalGuard>
    </Router>
  );
};

export default App;