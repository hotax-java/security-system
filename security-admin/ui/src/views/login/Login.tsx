import React, { useState, useEffect } from "react";
import { Form, Input, Button, Card, message, Typography, Space, Divider } from "antd";
import { UserOutlined, LockOutlined, LoginOutlined, WechatOutlined, GithubOutlined } from "@ant-design/icons";
import { authService } from "../../services/authService";
import { AuthConfigService } from "../../services/authConfigService";
import { PkceUtils } from "../../utils/pkceUtils";
import { PkceApiService } from "../../services/pkceApiService";

const { Title, Text } = Typography;

interface LoginProps {
  onLogin: (userData: any, token: string) => void;
}

const Login: React.FC<LoginProps> = ({ onLogin }) => {
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();
  const [pkceEnabled, setPkceEnabled] = useState<boolean>(false);
  const [ssoEnabled, setSsoEnabled] = useState<boolean>(false);
  const [supportedPlatforms, setSupportedPlatforms] = useState<string[]>([]);
  
  // 获取认证配置并处理SSO跳转 - 每次进入页面都检查
  useEffect(() => {
    const loadConfig = async () => {
      try {
        console.log('检查认证配置...');
        const isPkceEnabled = await AuthConfigService.isPkceEnabled();
        const isSsoEnabled = AuthConfigService.isSSO();
        const platforms = AuthConfigService.getSupportedPlatforms();
        
        setPkceEnabled(isPkceEnabled);
        setSsoEnabled(isSsoEnabled);
        setSupportedPlatforms(platforms);
        
        console.log(`PKCE ${isPkceEnabled ? '已启用' : '已禁用'}`);
        console.log(`SSO ${isSsoEnabled ? '已启用' : '已禁用'}`);
        console.log('支持的第三方平台:', platforms);
        
      } catch (error) {
        console.error('获取认证配置失败:', error);
      }
    };
    
    loadConfig();
  }, [window.location.pathname]); // 依赖路径变化，确保路由切换时重新检查

  const handleSubmit = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      // 本地登录模式
      const response = await authService.login(values.username, values.password);
      if (response.access_token) {
        message.success("登录成功");
        
        // 登录成功后清理localStorage中的PKCE相关参数
        PkceUtils.clearAllPkceParams();
        console.log('登录成功，已清理PKCE参数');
        
        // 使用access_token作为token，用户信息暂时使用用户名
        onLogin({ username: values.username }, response.access_token);
      } else {
        message.error("登录失败：未获取到访问令牌");
      }
    } catch (error: any) {
      console.error("登录失败:", error);
      if (error.response?.status === 401) {
        message.error("用户名或密码错误");
      } else {
        message.error("登录失败，请稍后重试");
      }
    } finally {
      setLoading(false);
    }
  };

  // 处理第三方登录
  const handleThirdPartyLogin = async (platform: string) => {
    try {
      // 检查平台是否支持
      if (!AuthConfigService.isPlatformSupported(platform)) {
        message.error(`不支持${platform}登录`);
        return;
      }
      
      if (pkceEnabled) {
        // PKCE模式：前端生成PKCE参数
        try {
          const pkceParams = await PkceUtils.generateAndStorePkceParams();
          
          console.log(`前端生成${platform}登录PKCE参数:`, {
            state: pkceParams.state,
            codeChallenge: pkceParams.codeChallenge,
            codeChallengeMethod: pkceParams.codeChallengeMethod
          });
          
          // 使用配置管理器获取第三方授权URL
          const authUrl = AuthConfigService.getThirdPartyAuthUrl(platform, {
            codeChallenge: pkceParams.codeChallenge,
            codeChallengeMethod: pkceParams.codeChallengeMethod,
            state: pkceParams.state
          });
          
          window.location.replace(authUrl);
        } catch (error) {
          console.error(`生成${platform}登录PKCE参数失败:`, error);
          message.error(`生成${platform}登录参数失败，请稍后重试`);
          return;
        }
      } else {
        // 传统模式
        const state = PkceUtils.generateRandomString(32);
        
        // 存储基础OAuth参数到localStorage
        localStorage.setItem('oauth_state', state);
        localStorage.setItem('oauth_platform', platform);
        
        // 传统模式不传递PKCE参数，让方法内部处理state参数
        const authUrl = AuthConfigService.getThirdPartyAuthUrl(platform);
        // 手动添加state参数到URL
        const urlWithState = authUrl + (authUrl.includes('?') ? '&' : '?') + `state=${state}`;
        window.location.replace(urlWithState);
        console.log(`使用传统模式发起${platform}登录`);
      }
    } catch (error) {
      console.error(`${platform}登录发起失败:`, error);
      message.error(`${platform}登录失败，请稍后重试`);
    }
  };

  // 替换特定平台的处理函数
  const handleWechatLogin = () => handleThirdPartyLogin('wechat');
  const handleGithubLogin = () => handleThirdPartyLogin('github');
  const handleAlipayLogin = () => handleThirdPartyLogin('alipay');

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
          width: 400,
          boxShadow: "0 4px 12px rgba(0, 0, 0, 0.15)",
          borderRadius: 8
        }}
        styles={{ body: { padding: "40px" } }}
      >
        <div style={{ textAlign: "center", marginBottom: 32 }}>
          <div style={{
            width: 64,
            height: 64,
            background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
            borderRadius: "50%",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            margin: "0 auto 16px",
            color: "white",
            fontSize: 24
          }}>
            <UserOutlined />
          </div>
          <Title level={2} style={{ margin: 0, color: "#1f2937" }}>
            权限管理系统
          </Title>
          <Text type="secondary">
            请输入您的账号和密码登录
          </Text>
        </div>

        <Form
          form={form}
          name="login"
          onFinish={handleSubmit}
          autoComplete="off"
          size="large"
          initialValues={{
            username: "admin",
            password: "admin123"
          }}
        >
          <Form.Item
            name="username"
            rules={[
              { required: true, message: "请输入用户名" },
              { min: 3, message: "用户名至少3个字符" }
            ]}
          >
            <Input
              prefix={<UserOutlined style={{ color: "#bfbfbf" }} />}
              placeholder="用户名"
              autoComplete="username"
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[
              { required: true, message: "请输入密码" },
              { min: 6, message: "密码至少6个字符" }
            ]}
          >
            <Input.Password
              prefix={<LockOutlined style={{ color: "#bfbfbf" }} />}
              placeholder="密码"
              autoComplete="current-password"
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 16 }}>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              size="large"
              icon={<LoginOutlined />}
              style={{
                background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
                border: "none",
                height: 48
              }}
            >
              {loading ? "登录中..." : "登录"}
            </Button>
          </Form.Item>

          <Divider plain>
            <Text type="secondary">第三方账号登录</Text>
          </Divider>

          <div style={{ display: 'flex', justifyContent: 'center', gap: '24px', marginTop: '16px' }}>
            {supportedPlatforms.includes('wechat') && (
              <Button 
                type="text" 
                shape="circle" 
                size="large"
                icon={<WechatOutlined style={{ fontSize: '24px', color: '#07C160' }} />} 
                onClick={handleWechatLogin}
                title="微信登录"
              />
            )}
            {supportedPlatforms.includes('github') && (
              <Button 
                type="text" 
                shape="circle" 
                size="large"
                icon={<GithubOutlined style={{ fontSize: '24px' }} />} 
                onClick={handleGithubLogin}
                title="GitHub登录"
              />
            )}
            {supportedPlatforms.includes('alipay') && (
              <Button 
                type="text" 
                shape="circle" 
                size="large"
                icon={
                  <svg viewBox="0 0 1024 1024" width="24" height="24" fill="#1677FF">
                    <path d="M230.1 630.2l-76.9 132.9h152.2l76.9-132.9H230.1z m224.2-387.7l-76.9 133h304.4l76.9-133H454.3z m0 258.5l-76.9 133h228.2l76.9-133H454.3z m380.5-258.5h-76.2l-76.9 133h152.2l76.9-133h-76z m0 258.5h-76.2l-76.9 133h152.2l76.9-133h-76z" />
                  </svg>
                } 
                onClick={handleAlipayLogin}
                title="支付宝登录"
              />
            )}
          </div>
        </Form>

        {!ssoEnabled && (
          <div style={{ 
            marginTop: 24, 
            padding: 16, 
            background: "#f8fafc", 
            borderRadius: 6,
            textAlign: "center"
          }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              默认账号：admin / admin123
            </Text>
          </div>
        )}
        
        {ssoEnabled && (
          <div style={{ 
            marginTop: 24, 
            padding: 16, 
            background: "#e6f7ff", 
            borderRadius: 6,
            textAlign: "center"
          }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              SSO模式已启用，将跳转到统一认证中心
            </Text>
          </div>
        )}
      </Card>
    </div>
  );
};

export default Login;
