import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, Tabs, message, Spin, Typography, Avatar } from 'antd';
import { UserOutlined, LockOutlined, GithubOutlined, WechatOutlined } from '@ant-design/icons';
import axios from 'axios';
import { TokenManager } from '../../services/tokenManager';
import ErrorPage from './ErrorPage';

const { Title, Text } = Typography;
const { TabPane } = Tabs;

// API基础URL
const API_BASE_URL = process.env.REACT_APP_AUTH_BASE_URL || 'http://localhost:9000';

interface ThirdPartyCallbackProps {
  onLogin: (userData: any, token: string) => void;
}

const ThirdPartyCallback: React.FC<ThirdPartyCallbackProps> = ({ onLogin }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [thirdPartyData, setThirdPartyData] = useState<any>(null);
  const [activeTab, setActiveTab] = useState('bind');
  const [bindForm] = Form.useForm();
  const [registerForm] = Form.useForm();
  const [platform, setPlatform] = useState<'github' | 'wechat' | 'alipay' | ''>('');

  useEffect(() => {
    // 解析URL中的参数
    const params = new URLSearchParams(location.search);
    const token = params.get('access_token');
    const errorMsg = params.get('error');
    const encryptedOpenId = params.get('encryptedOpenId');
    const nickname = params.get('nickname');
    const headimgurl = params.get('headimgurl');
    const code = params.get('code');
    const state = params.get('state');
    const username = params.get('username');
    
    // 从URL参数中获取平台信息
    const platformParam = params.get('platform') as 'github' | 'wechat' | 'alipay' | '';
    setPlatform(platformParam || '');
    
    // 处理错误情况
    if (errorMsg) {
      setError(decodeURIComponent(errorMsg));
      setLoading(false);
      return;
    }
    
    // 处理已有token的情况（已绑定用户）
    if (token) {
      // 从URL参数中获取完整的token信息
      const accessToken = params.get('access_token') || token;
      const refreshToken = params.get('refresh_token');
      const expiresIn = params.get('expires_in');
      
      // 解析过期时间，确保它是一个数字
      let validExpiresIn = expiresIn ? parseInt(expiresIn, 10) : undefined;
      
      // 使用TokenManager保存完整的token信息
      TokenManager.saveTokens(accessToken, refreshToken || undefined, validExpiresIn);
      
      // 调用登录回调
      onLogin({ username: username }, accessToken);
      
      // 跳转到首页
      navigate('/dashboard');
      message.success('登录成功');
      return;
    }
    
    // 处理未绑定用户的情况（后端已经处理过OAuth回调）
    if (encryptedOpenId && nickname) {
      setThirdPartyData({
        encryptedId: encryptedOpenId,
        nickname: nickname,
        avatarUrl: headimgurl || ''
      });
      setLoading(false);
      return;
    }
    
    // 处理OAuth回调的情况（code + state）
    if (code && state) {
      // 获取第三方用户数据
      redirectToBackend(platformParam || '', code, state);
      return;
    }
    
    // 如果没有上述参数，说明URL格式不正确
    setError('回调参数不完整，请重新登录');
    setLoading(false);
  }, [location.search, location.pathname]);

  const redirectToBackend = async (platform: string, code: string, state: string) => {
    try {
      setLoading(true);
      
      // 根据平台调用不同的后端接口
      let apiUrl = '';
      switch (platform) {
        case 'github':
          apiUrl = `${API_BASE_URL}/oauth2/github/callback`;
          break;
        case 'wechat':
          apiUrl = `${API_BASE_URL}/oauth2/wechat/callback`;
          break;
        case 'alipay':
          apiUrl = `${API_BASE_URL}/oauth2/alipay/callback`;
          break;
        default:
          setError('不支持的第三方平台');
          setLoading(false);
          return;
      }
      
      // 调用后端API处理OAuth回调
      const response = await axios.get(`${apiUrl}?code=${code}&state=${state}`);
      
      // 检查响应是否包含token（已绑定用户的情况）
      if (response.data && response.data.access_token) {
        handleLoginSuccess(response.data);
      } else {
        // 如果没有token，说明需要绑定账号，前端处理绑定流程
        setLoading(false);
      }
    } catch (error: any) {
      console.error('处理第三方登录失败:', error);
      setError(error.response?.data?.message || '第三方登录处理失败');
      setLoading(false);
    }
  };

  const handleLoginSuccess = (tokenData: any) => {
    // 提取完整的token信息
    const { access_token, refresh_token, expires_in } = tokenData;
    
    // 解析过期时间，确保它是一个数字
    let validExpiresIn = expires_in;
    if (typeof expires_in === 'string') {
      validExpiresIn = parseInt(expires_in, 10);
    }
    
    // 使用TokenManager保存完整的token信息
    TokenManager.saveTokens(access_token, refresh_token, validExpiresIn);
    
    // 调用登录回调
    onLogin({ username: `${platform}_user` }, access_token);
    
    // 跳转到首页
    navigate('/dashboard');
    message.success('登录成功');
  };

  const handleBindAccount = async (values: any) => {
    try {
      setLoading(true);
      
      // 根据平台选择API端点
      let apiUrl = '';
      switch (platform) {
        case 'github':
          apiUrl = `${API_BASE_URL}/oauth2/github/bind`;
          break;
        case 'wechat':
          apiUrl = `${API_BASE_URL}/oauth2/wechat/bind`;
          break;
        case 'alipay':
          apiUrl = `${API_BASE_URL}/oauth2/alipay/bind`;
          break;
        default:
          throw new Error('不支持的第三方平台');
      }
      
      // 调用绑定已有账号接口
      const params = new URLSearchParams();
      
      // 根据平台设置不同的参数名
      if (platform === 'github') {
        params.append('encryptedGithubId', thirdPartyData.encryptedId);
      } else {
        params.append('encryptedOpenId', thirdPartyData.encryptedId);
      }
      
      params.append('username', values.username);
      params.append('password', values.password);
      
      const response = await axios.post(apiUrl, params);
      
      if (response.data && response.data.success) {
        message.success('账号绑定成功');
        // response.data.token包含完整的token信息
        const tokenData = response.data.token;
        const { access_token, refresh_token, expires_in } = tokenData;
        
        // 解析过期时间，确保它是一个数字
        let validExpiresIn = expires_in;
        if (typeof expires_in === 'string') {
          validExpiresIn = parseInt(expires_in, 10);
        }
        
        // 使用TokenManager保存完整的token信息
        TokenManager.saveTokens(access_token, refresh_token, validExpiresIn);
        
        // 调用登录回调
        onLogin({ username: `${platform}_user` }, access_token);
        
        // 跳转到首页
        navigate('/dashboard');
        message.success('登录成功');
      } else {
        message.error(response.data.message || '账号绑定失败');
        setLoading(false);
      }
    } catch (error: any) {
      console.error('绑定账号失败:', error);
      message.error(error.response?.data?.message || '绑定账号失败');
      setLoading(false);
    }
  };

  const handleCreateAccount = async (values: any) => {
    try {
      setLoading(true);
      
      // 根据平台选择API端点
      let apiUrl = '';
      switch (platform) {
        case 'github':
          apiUrl = `${API_BASE_URL}/oauth2/github/create`;
          break;
        case 'wechat':
          apiUrl = `${API_BASE_URL}/oauth2/wechat/create`;
          break;
        case 'alipay':
          apiUrl = `${API_BASE_URL}/oauth2/alipay/create`;
          break;
        default:
          throw new Error('不支持的第三方平台');
      }
      
      // 调用创建新账号接口
      const params = new URLSearchParams();
      
      // 根据平台设置不同的参数名
      if (platform === 'github') {
        params.append('encryptedGithubId', thirdPartyData.encryptedId);
      } else {
        params.append('encryptedOpenId', thirdPartyData.encryptedId);
      }
      
      params.append('username', values.username);
      params.append('password', values.password);
      
      // 添加额外信息
      if (thirdPartyData.nickname) {
        params.append('nickname', thirdPartyData.nickname);
      }
      if (thirdPartyData.avatarUrl) {
        params.append('headimgurl', thirdPartyData.avatarUrl);
      }
      
      const response = await axios.post(apiUrl, params);
      
      if (response.data && response.data.success) {
        message.success('账号创建成功');
        // response.data.token包含完整的token信息
        const tokenData = response.data.token;
        const { access_token, refresh_token, expires_in } = tokenData;
        
        // 解析过期时间，确保它是一个数字
        let validExpiresIn = expires_in;
        if (typeof expires_in === 'string') {
          validExpiresIn = parseInt(expires_in, 10);
        }
        
        // 使用TokenManager保存完整的token信息
        TokenManager.saveTokens(access_token, refresh_token, validExpiresIn);
        
        // 调用登录回调
        onLogin({ username: `${platform}_user` }, access_token);
        
        // 跳转到首页
        navigate('/dashboard');
        message.success('登录成功');
      } else {
        message.error(response.data.message || '账号创建失败');
        setLoading(false);
      }
    } catch (error: any) {
      console.error('创建账号失败:', error);
      message.error(error.response?.data?.message || '创建账号失败');
      setLoading(false);
    }
  };

  const getPlatformIcon = () => {
    switch (platform) {
      case 'github':
        return <GithubOutlined />;
      case 'wechat':
        return <WechatOutlined style={{ color: '#07C160' }} />;
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

  if (!thirdPartyData) {
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
        </Card>
      </div>
    );
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
            src={thirdPartyData.avatarUrl} 
            icon={getPlatformIcon()}
            style={{ marginBottom: 16 }}
          />
          <Title level={3} style={{ margin: 0 }}>
            {getPlatformName()}账号关联
          </Title>
          <Text type="secondary">
            欢迎 {thirdPartyData.nickname}，请选择关联方式
          </Text>
        </div>

        <Tabs activeKey={activeTab} onChange={setActiveTab} centered>
          <TabPane tab="绑定已有账号" key="bind">
            <Form
              form={bindForm}
              name="bind_account"
              onFinish={handleBindAccount}
              layout="vertical"
            >
              <Form.Item
                name="username"
                rules={[{ required: true, message: '请输入用户名' }]}
              >
                <Input prefix={<UserOutlined />} placeholder="用户名" />
              </Form.Item>

              <Form.Item
                name="password"
                rules={[{ required: true, message: '请输入密码' }]}
              >
                <Input.Password prefix={<LockOutlined />} placeholder="密码" />
              </Form.Item>

              <Form.Item>
                <Button type="primary" htmlType="submit" block loading={loading}>
                  绑定账号
                </Button>
              </Form.Item>
            </Form>
          </TabPane>

          <TabPane tab="创建新账号" key="register">
            <Form
              form={registerForm}
              name="register_account"
              onFinish={handleCreateAccount}
              layout="vertical"
            >
              <Form.Item
                name="username"
                rules={[
                  { required: true, message: '请输入用户名' },
                  { min: 3, message: '用户名至少3个字符' }
                ]}
              >
                <Input prefix={<UserOutlined />} placeholder="设置用户名" />
              </Form.Item>

              <Form.Item
                name="password"
                rules={[
                  { required: true, message: '请输入密码' },
                  { min: 6, message: '密码至少6个字符' }
                ]}
              >
                <Input.Password prefix={<LockOutlined />} placeholder="设置密码" />
              </Form.Item>

              <Form.Item
                name="confirmPassword"
                dependencies={['password']}
                rules={[
                  { required: true, message: '请确认密码' },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (!value || getFieldValue('password') === value) {
                        return Promise.resolve();
                      }
                      return Promise.reject(new Error('两次输入的密码不一致'));
                    },
                  }),
                ]}
              >
                <Input.Password prefix={<LockOutlined />} placeholder="确认密码" />
              </Form.Item>

              <Form.Item>
                <Button type="primary" htmlType="submit" block loading={loading}>
                  创建并关联账号
                </Button>
              </Form.Item>
            </Form>
          </TabPane>
        </Tabs>

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