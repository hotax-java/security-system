import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';

// 导入主应用组件
import Login from './views/login/Login';
import AdminAuthInitiator from './test/AdminAuthInitiator';
import TestOAuth2Callback from './test/OAuth2Callback';

import './App.css';

const App: React.FC = () => {
  return (
    <ConfigProvider locale={zhCN}>
      <Router>
        <div className="App">
          <Routes>
            {/* 主应用路由 */}
            <Route path="/login" element={<Login />} />
            {/* 测试相关路由 */}
            <Route path="/test/admin-auth-initiator" element={<AdminAuthInitiator />} />
            <Route path="/test/oauth2-callback" element={<TestOAuth2Callback />} />
            
            {/* 默认路由重定向到测试入口 */}
            <Route path="/" element={<AdminAuthInitiator />} />
          </Routes>
        </div>
      </Router>
    </ConfigProvider>
  );
};

export default App;
