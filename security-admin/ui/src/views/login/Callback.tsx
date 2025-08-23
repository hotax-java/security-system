import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Result, Button, Spin, Typography, Card } from 'antd';
import { CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import { TokenManager } from '../../services/tokenManager';
import { oauth2Service } from '../../services/oauth2Service';
import { PkceUtils } from '../../utils/pkceUtils';

const { Title, Text } = Typography;

interface CallbackProps {
  onLogin: (userData: any, token: string) => void;
}

const Callback: React.FC<CallbackProps> = ({ onLogin }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    handleCallback();
  }, [location.search]);

  const handleCallback = async () => {
    try {
      // 解析URL参数
      const params = new URLSearchParams(location.search);
      const code = params.get('code');
      const error = params.get('error');
      const errorDescription = params.get('error_description');

      // 检查是否有错误
      if (error) {
        setError(errorDescription || `授权错误: ${error}`);
        setLoading(false);
        return;
      }

      // 检查授权码
      if (!code) {
        setError('授权码不存在，授权失败');
        setLoading(false);
        return;
      }

      // 获取之前存储的code_verifier
      const codeVerifier = PkceUtils.getStoredCodeVerifier();
      if (!codeVerifier) {
        setError('PKCE参数丢失，授权失败');
        setLoading(false);
        return;
      }

      // 使用授权码换取Token
      const tokenResponse = await oauth2Service.exchangeCodeForToken({
        grant_type: 'authorization_code',
        code: code,
        code_verifier: codeVerifier,
        client_id: process.env.REACT_APP_OAUTH2_CLIENT_ID || 'webapp-client',
        redirect_uri: process.env.REACT_APP_OAUTH2_REDIRECT_URI || 'http://localhost:3000/callback',
      });

      // 保存Token信息
      TokenManager.saveTokens(
        tokenResponse.access_token,
        tokenResponse.refresh_token,
        tokenResponse.expires_in
      );

      // 清除PKCE参数
      PkceUtils.clearCodeVerifier();

      // 调用登录成功回调
      onLogin({ username: 'oauth2_user' }, tokenResponse.access_token);

      // 跳转到首页
      setTimeout(() => {
        navigate('/dashboard');
      }, 1500);

    } catch (err: any) {
      console.error('Callback处理失败:', err);
      setError(err.message || '授权失败，请重试');
      setLoading(false);
      
      // 清除PKCE参数
      PkceUtils.clearCodeVerifier();
    }
  };

  const handleBackToLogin = () => {
    navigate('/login');
  };

  if (loading) {
    return (
      <div style={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '20px'
      }}>
        <Card style={{
          width: 400,
          boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
          borderRadius: 8,
          textAlign: 'center'
        }}>
          <Spin size="large" tip="正在处理授权..." />
          <div style={{ marginTop: 16 }}>
            <Text type="secondary">正在验证您的身份...</Text>
          </div>
        </Card>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '20px'
      }}>
        <Result
          status="error"
          icon={<CloseCircleOutlined />}
          title="授权失败"
          subTitle={error}
          extra={[
            <Button type="primary" key="back" onClick={handleBackToLogin}>
              返回登录页
            </Button>
          ]}
          style={{
            backgroundColor: '#fff',
            padding: '40px',
            borderRadius: '8px',
            boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)'
          }}
        />
      </div>
    );
  }

  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '20px'
    }}>
      <Result
        status="success"
        icon={<CheckCircleOutlined />}
        title="登录成功"
        subTitle="正在跳转到系统主页..."
        style={{
          backgroundColor: '#fff',
          padding: '40px',
          borderRadius: '8px',
          boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)'
        }}
      />
    </div>
  );
};

export default Callback;