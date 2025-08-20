import { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Card, message, Spin, Typography, Avatar, Button } from 'antd';
import { UserOutlined, GithubOutlined, WechatOutlined } from '@ant-design/icons';
import { businessApi } from '../../apis/api';
import { TokenManager } from '../../services/tokenManager';
import ErrorPage from './ErrorPage';

const { Title, Text } = Typography;

interface ThirdPartyCallbackProps {
  onLogin: (userData: any, token: string) => void;
}

interface TokenResponse {
  access_token: string;
  token_type: string;
  expires_in: number;
  refresh_token: string;
  scope: string;
  id_token?: string;
}

const ThirdPartyCallback: React.FC<ThirdPartyCallbackProps> = ({ onLogin }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [platform, setPlatform] = useState<'github' | 'wechat' | 'alipay' | 'admin' | ''>('');

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    
    // 检查是否是Admin后端回调 (已经有token)
    if (params.get('platform') === 'admin') {
      handleAdminCallback(params);
    } else {
      // 这是第三方登录后的回调，需要发起OAuth2授权
      handleSSOCallback(params);
    }
  }, [location.search]);

  // 处理Admin后端回调 (已有token)
  const handleAdminCallback = (params: URLSearchParams) => {
    try {
      const accessToken = params.get('access_token');
      const refreshToken = params.get('refresh_token');
      const expiresInStr = params.get('expires_in');
      const platformType = params.get('platform');
      
      if (!accessToken) {
        setError('缺少访问令牌');
        return;
      }
      
      setPlatform(platformType as 'admin');
      
      // 解析过期时间
      let expiresIn = 3600; // 默认1小时
      if (expiresInStr) {
        expiresIn = parseInt(expiresInStr, 10);
      }
      
      // 保存token
      TokenManager.saveTokens(accessToken, refreshToken || '', expiresIn);
      
      // 调用登录回调
      onLogin({ username: `${platform || 'third_party'}_user` }, accessToken);
      
      // 跳转到首页
      message.success('登录成功');
      navigate('/dashboard');
    } catch (error: any) {
      console.error('处理Admin回调失败:', error);
      setError('处理登录信息失败');
    }
  };

  // 处理SSO第三方登录回调
  const handleSSOCallback = (params: URLSearchParams) => {
    const state = params.get('state') || '';
    const clientId = params.get('clientId') || '';
    const platformType = params.get('platform') || '';

    if (!state || !clientId) {
      setError('缺少必要参数，请重新登录');
      return;
    }

    if (platformType) {
      setPlatform(platformType as 'github' | 'wechat' | 'alipay' | '');
    }
    
    // 调用后端接口发起OAuth2授权
    initiateAuthorization(state, clientId, platformType);
  };
  
  // 发起后端OAuth2授权
  const initiateAuthorization = async (state: string, clientId: string, platform: string) => {
    try {
      setLoading(true);
      
      // 调用后端授权接口
      await businessApi.post('/api/oauth2/authorize', {
        state: state,
        clientId: clientId,
        redirectUri: window.location.origin + '/oauth2/callback',
        platform: platform
      });
      
      // 注意: 这里实际上不会执行到，因为后端会重定向
      // 但为了防止任何意外情况，我们设置一个超时错误
      setTimeout(() => {
        if (loading) {
          setError('授权请求超时，请重试');
          setLoading(false);
        }
      }, 10000);
      
    } catch (error: any) {
      console.error('授权请求失败:', error);
      setError(error.response?.data?.message || '授权请求失败');
      setLoading(false);
    }
  };

  const getPlatformIcon = () => {
    switch (platform) {
      case 'github':
        return <GithubOutlined />;
      case 'wechat':
        return <WechatOutlined style={{ color: '#07C160' }} />;
      case 'admin':
        return <UserOutlined style={{ color: '#1890ff' }} />;
      default:
        return <UserOutlined />;
    }
  };

  const getPlatformName = () => {
    switch (platform) {
      case 'github':
        return 'GitHub';
      case 'wechat':
        return '微信';
      case 'alipay':
        return '支付宝';
      case 'admin':
        return '系统';
      default:
        return '第三方';
    }
  };

  if (loading) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh',
        background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
      }}>
        <Spin size="large" tip={`正在处理${getPlatformName()}登录...`} />
      </div>
    );
  }

  if (error) {
    return <ErrorPage title={`${getPlatformName()}登录失败`} error={error} />;
  }

  return (
    <div style={{
      minHeight: "100vh",
      background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      padding: "20px"
    }}>
      <Card
        style={{
          width: 450,
          boxShadow: "0 4px 12px rgba(0, 0, 0, 0.15)",
          borderRadius: 8
        }}
      >
        <div style={{ textAlign: "center", marginBottom: 24 }}>
          <Avatar 
            size={64}
            icon={getPlatformIcon()}
            style={{ marginBottom: 16 }}
          />
          <Title level={3} style={{ margin: 0 }}>
            {getPlatformName()}登录
          </Title>
          <Text type="secondary">
            正在处理登录请求...
          </Text>
        </div>
        
        <div style={{ textAlign: 'center', marginTop: 20 }}>
          <Spin size="large" />
        </div>
        
        <div style={{ textAlign: 'center', marginTop: 16 }}>
          <Button type="link" onClick={() => navigate('/login')}>
            返回登录页
          </Button>
        </div>
      </Card>
    </div>
  );
};

export default ThirdPartyCallback;