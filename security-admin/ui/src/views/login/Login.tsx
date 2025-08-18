import React, { useState } from "react";
import { Form, Input, Button, Card, message, Typography, Space, Divider } from "antd";
import { UserOutlined, LockOutlined, LoginOutlined, WechatOutlined, GithubOutlined } from "@ant-design/icons";
import { authService } from "../../services/authService";

// API基础URL
const API_BASE_URL = process.env.REACT_APP_AUTH_BASE_URL || 'http://localhost:9000';

const { Title, Text } = Typography;

interface LoginProps {
  onLogin: (userData: any, token: string) => void;
}

const Login: React.FC<LoginProps> = ({ onLogin }) => {
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();

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

  const handleWechatLogin = () => {
    // 跳转到微信授权页面
    window.location.href = `${API_BASE_URL}/oauth2/wechat/authorize`;
  };
  
  const handleGithubLogin = () => {
    // 跳转到GitHub授权页面
    window.location.href = `${API_BASE_URL}/oauth2/github/authorize`;
  };
  
  const handleAlipayLogin = () => {
    // 跳转到支付宝授权页面
    window.location.href = `${API_BASE_URL}/oauth2/alipay/authorize`;
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
            <Text type="secondary">第三方账号登录</Text>
          </Divider>

          <div style={{ display: 'flex', justifyContent: 'center', gap: '24px', marginTop: '16px' }}>
            <Button 
              type="text" 
              shape="circle" 
              size="large"
              icon={<WechatOutlined style={{ fontSize: '24px', color: '#07C160' }} />} 
              onClick={handleWechatLogin}
              title="微信登录"
            />
            <Button 
              type="text" 
              shape="circle" 
              size="large"
              icon={<GithubOutlined style={{ fontSize: '24px' }} />} 
              onClick={handleGithubLogin}
              title="GitHub登录"
            />
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
          </div>
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
