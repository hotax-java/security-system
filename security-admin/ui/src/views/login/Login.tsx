import React, { useState, useEffect } from "react";
import { Form, Input, Button, Card, message, Typography, Space, Divider, Spin } from "antd";
import { UserOutlined, LockOutlined, LoginOutlined, SafetyOutlined } from "@ant-design/icons";
import { authService } from "../../services/authService";
import { oauth2Service } from "../../services/oauth2Service";
import { PkceUtils } from "../../utils/pkceUtils";

const { Title, Text } = Typography;

interface LoginProps {
  onLogin: (userData: any, token: string) => void;
}

const Login: React.FC<LoginProps> = ({ onLogin }) => {
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();
  const [pkceLoading, setPkceLoading] = useState(false);

  const handleSubmit = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      const response = await authService.login(values.username, values.password);
      if (response.access_token) {
        message.success("登录成功");
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
      if (pkceEnabled) {
        // PKCE模式
        const pkceParams = await PkceUtils.preparePkceParams();
        
        // 存储code_verifier
        PkceUtils.storeCodeVerifier(pkceParams.codeVerifier);
        
        // 跳转到SSO的第三方授权页面，携带PKCE参数
        window.location.href = `${API_BASE_URL}/oauth2/${platform}/authorize` +
          `?code_challenge=${encodeURIComponent(pkceParams.codeChallenge)}` +
          `&code_challenge_method=${encodeURIComponent(pkceParams.codeChallengeMethod)}`;
        
        console.log(`使用PKCE模式发起${platform}登录`);
      } else {
        // 传统模式
        window.location.href = `${API_BASE_URL}/oauth2/${platform}/authorize`;
        console.log(`使用传统模式发起${platform}登录`);
      }
    } catch (error) {
      console.error(`${platform}登录发起失败:`, error);
      message.error(`${platform}登录失败，请稍后重试`);
    }
  };

  // 处理授权码登录（PKCE模式）
  const handleAuthorizationCodeLogin = async () => {
    setPkceLoading(true);
    try {
      // 生成PKCE参数
      const pkceParams = await PkceUtils.preparePkceParams();
      
      // 存储code_verifier到sessionStorage
      PkceUtils.storeCodeVerifier(pkceParams.codeVerifier);
      
      // 生成随机状态参数
      const state = oauth2Service.generateState();
      
      // 构建授权URL
      const authUrl = oauth2Service.buildAuthorizationUrl({
        client_id: process.env.REACT_APP_OAUTH2_CLIENT_ID || 'webapp-client',
        redirect_uri: process.env.REACT_APP_OAUTH2_REDIRECT_URI || 'http://localhost:3000/callback',
        scope: process.env.REACT_APP_OAUTH2_SCOPE || 'read,write,openid,profile,offline_access',
        code_challenge: pkceParams.codeChallenge,
        state,
      });
      
      console.log('跳转到授权页面:', authUrl);
      // 跳转到SSO授权页面
      window.location.href = authUrl;
    } catch (error) {
      console.error('授权码登录失败:', error);
      message.error('授权码登录失败，请稍后重试');
      setPkceLoading(false);
    }
  };

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
            <Text type="secondary">或</Text>
          </Divider>

          <Form.Item style={{ marginBottom: 16 }}>
            <Button
              type="default"
              block
              size="large"
              icon={<SafetyOutlined />}
              loading={pkceLoading}
              onClick={handleAuthorizationCodeLogin}
              style={{
                height: 48,
                border: "1px solid #667eea",
                color: "#667eea"
              }}
            >
              {pkceLoading ? "准备中..." : "授权码登录"}
            </Button>
          </Form.Item>
        </Form>

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
      </Card>
    </div>
  );
};

export default Login;
