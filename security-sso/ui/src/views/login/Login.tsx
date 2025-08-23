import React, { useState } from 'react';
import { Form, Input, Button, Card, Typography, message, Spin } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { loginWithCredentials } from '../../utils/apiClient';

const { Title, Paragraph } = Typography;

interface LoginFormValues {
  username: string;
  password: string;
}

const Login: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(false);
  const [form] = Form.useForm();

  // 从URL获取重定向参数
  const getRedirectUri = () => {
    const params = new URLSearchParams(window.location.search);
    return params.get('redirect_uri') || '/';
  };

  const handleSubmit = async (values: LoginFormValues) => {
    try {
      setLoading(true);
        debugger
      // 使用封装的loginWithCredentials函数进行登录
      const response = await loginWithCredentials({
        username: values.username,
        password: values.password
      });

      // 登录成功，重定向到原始请求页面
      if (response.status === 200 || response.status === 302) {
        const redirectUri = getRedirectUri();
        window.location.href = redirectUri;
      } else {
        message.error('登录失败，请检查用户名和密码');
      }
    } catch (error) {
      console.error('登录请求出错:', error);
      message.error('登录失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      minHeight: '100vh',
      padding: '20px',
      background: '#f0f2f5'
    }}>
      <Card 
        style={{ 
          width: '100%', 
          maxWidth: 400,
          boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <Title level={2} style={{ marginBottom: 0 }}>统一认证中心</Title>
          <Paragraph type="secondary">使用您的账号密码登录</Paragraph>
        </div>

        <Spin spinning={loading}>
          <Form
            form={form}
            name="login_form"
            layout="vertical"
            initialValues={{
              username: 'admin',  // 默认填充测试账号
              password: 'admin123'
            }}
            onFinish={handleSubmit}
          >
            <Form.Item
              name="username"
              rules={[{ required: true, message: '请输入用户名' }]}
            >
              <Input
                prefix={<UserOutlined />}
                size="large"
                placeholder="用户名"
              />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                size="large"
                placeholder="密码"
              />
            </Form.Item>

            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                size="large"
                loading={loading}
                style={{ width: '100%' }}
              >
                登录
              </Button>
            </Form.Item>
          </Form>
          
          <div style={{ textAlign: 'center', marginTop: 12 }}>
            <Paragraph type="secondary" style={{ fontSize: 12 }}>
              测试账户：admin / admin123
            </Paragraph>
          </div>
        </Spin>
      </Card>
    </div>
  );
};

export default Login;