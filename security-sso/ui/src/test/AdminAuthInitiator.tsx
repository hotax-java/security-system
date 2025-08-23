import React, { useState } from 'react';
import { Card, Button, Typography, Spin, message } from 'antd';
import { LoginOutlined } from '@ant-design/icons';
import { generateRandomString, generateCodeVerifier, generateCodeChallenge } from '../utils/pkceUtils';

const { Title, Paragraph, Text } = Typography;

/**
 * 模拟Admin UI发起OAuth2授权的组件
 * 仅用于测试OAuth2授权码流程
 */
const AdminAuthInitiator: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(false);

  // 发起OAuth2授权
  const initiateAuth = async () => {
    try {
        debugger
      setLoading(true);
      
      // 生成PKCE参数
      const codeVerifier = generateCodeVerifier();
      const codeChallenge = await generateCodeChallenge(codeVerifier);
      
      // 生成随机state
      const state = generateRandomString(16);
      
      // 保存code_verifier到sessionStorage，供回调页面使用
      sessionStorage.setItem('pkce_code_verifier', codeVerifier);
      sessionStorage.setItem('pkce_state', state);
      
      console.log('PKCE 参数已生成:');
      console.log('- Code Verifier (部分):', codeVerifier.substring(0, 10) + '...');
      console.log('- Code Challenge (部分):', codeChallenge.substring(0, 10) + '...');
      
      // 构建授权请求URL
      const params = new URLSearchParams({
        response_type: 'code',
        client_id: 'webapp-client',
        redirect_uri: `${window.location.origin}/test/oauth2-callback`,
        scope: 'read write openid profile offline_access',
        state: state,
        code_challenge: codeChallenge,
        code_challenge_method: 'S256',
        access_type: 'offline'  // 确保返回refresh_token
      });
      
      // 跳转到授权端点
      const authUrl = `/oauth2/authorize?${params.toString()}`;
      console.log('跳转到授权端点:', authUrl);
      
      window.location.href = authUrl;
    } catch (error) {
      console.error('发起授权失败:', error);
      message.error('授权请求失败，请稍后重试');
      setLoading(false);
    }
  };
  
  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      minHeight: '100vh',
      padding: '20px'
    }}>
      <Card 
        style={{ 
          width: '100%', 
          maxWidth: 500,
          boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
        }}
        title={
          <Title level={3} style={{ marginBottom: 0, textAlign: 'center' }}>
            模拟Admin UI发起授权
          </Title>
        }
      >
        <Spin spinning={loading} tip="正在准备授权请求...">
          <Paragraph style={{ marginBottom: 24 }}>
            点击下方按钮，模拟Admin UI应用发起OAuth2授权码流程（带PKCE扩展）。
            流程将经过以下步骤：
          </Paragraph>
          
          <ul style={{ marginBottom: 24 }}>
            <li>生成PKCE参数（code_verifier和code_challenge）</li>
            <li>跳转到SSO的/oauth2/authorize端点</li>
            <li>未登录时会自动跳转到登录页面</li>
            <li>登录成功后获取授权码并跳转回回调页面</li>
            <li>回调页面使用授权码换取token</li>
          </ul>
          
          <div style={{ textAlign: 'center', marginTop: 24 }}>
            <Button 
              type="primary" 
              icon={<LoginOutlined />}
              size="large"
              onClick={initiateAuth}
              disabled={loading}
              style={{ height: '48px', minWidth: '200px' }}
            >
              发起授权登录
            </Button>
          </div>
          
          <div style={{ marginTop: 24, padding: 16, background: '#f9f9f9', borderRadius: 4 }}>
            <Text type="secondary" style={{ fontSize: '13px' }}>
              <strong>配置信息：</strong>
              <br />
              client_id: webapp-client
              <br />
              redirect_uri: {window.location.origin}/test/oauth2-callback
              <br />
              scope: read write openid profile offline_access
            </Text>
          </div>
        </Spin>
      </Card>
    </div>
  );
};

export default AdminAuthInitiator; 