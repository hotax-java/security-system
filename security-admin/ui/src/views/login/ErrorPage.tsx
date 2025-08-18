import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Card, Button, Alert, Typography } from 'antd';

const { Title, Text } = Typography;

interface ErrorPageProps {
  title?: string;
  error?: string;
  showBackButton?: boolean;
  backButtonText?: string;
  backPath?: string;
}

/**
 * 通用错误页面组件
 * 用于显示登录和授权过程中的错误信息
 */
const ErrorPage: React.FC<ErrorPageProps> = ({
  title = '操作失败',
  error,
  showBackButton = true,
  backButtonText = '返回登录页',
  backPath = '/login'
}) => {
  const location = useLocation();
  const navigate = useNavigate();
  
  // 如果没有传入错误信息，尝试从URL参数中获取
  const errorFromUrl = new URLSearchParams(location.search).get('error');
  const displayError = error || errorFromUrl || '发生未知错误';

  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      height: '100vh',
      background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
    }}>
      <Card 
        style={{ 
          width: 450, 
          boxShadow: "0 4px 12px rgba(0, 0, 0, 0.15)",
          borderRadius: 8 
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <Title level={3}>{title}</Title>
        </div>
        
        <Alert
          message="错误信息"
          description={displayError}
          type="error"
          showIcon
          style={{ marginBottom: 24 }}
        />
        
        {showBackButton && (
          <div style={{ textAlign: 'center' }}>
            <Button 
              type="primary" 
              onClick={() => navigate(backPath)}
            >
              {backButtonText}
            </Button>
          </div>
        )}
      </Card>
    </div>
  );
};

export default ErrorPage; 