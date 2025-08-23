import React, { useEffect, useState } from 'react';
import { Card, Result, Button, Typography, Alert, Spin, Divider } from 'antd';
import { CheckCircleOutlined, ExclamationCircleOutlined, HomeOutlined, ReloadOutlined } from '@ant-design/icons';
import { exchangeCodeForToken } from '../utils/apiClient';
// 更新导入，使用我们创建的tokenService
import { saveTokens } from '../services/tokenService';

const { Title, Paragraph, Text } = Typography;

/**
 * OAuth2授权回调处理组件
 * 负责处理授权码回调，交换Token，并显示结果
 */
const OAuth2Callback: React.FC = () => {
  // 状态定义
  const [loading, setLoading] = useState<boolean>(true);
  const [success, setSuccess] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [tokenData, setTokenData] = useState<any>(null);
  const [code, setCode] = useState<string | null>(null);
  
  useEffect(() => {
    // 处理授权码回调
    const handleCallback = async () => {
      try {
        // 从URL中获取授权码和state
        const urlParams = new URLSearchParams(window.location.search);
        const authCode = urlParams.get('code');
        const state = urlParams.get('state');
        const errorParam = urlParams.get('error');
        const errorDescription = urlParams.get('error_description');
        
        // 检查是否有错误
        if (errorParam) {
          throw new Error(`授权失败: ${errorParam}${errorDescription ? ` - ${errorDescription}` : ''}`);
        }
        
        // 检查是否有授权码
        if (!authCode) {
          throw new Error('未收到授权码');
        }
        
        setCode(authCode);
        console.log('成功获取授权码:', authCode);
        
        // 从sessionStorage中获取code_verifier
        const codeVerifier = sessionStorage.getItem('pkce_code_verifier');
        if (!codeVerifier) {
          throw new Error('PKCE参数丢失，无法完成授权。请返回重新发起授权流程。');
        }
        
        // 设置重定向URI，使用当前页面URL
        const redirectUri = `${window.location.origin}/test/oauth2-callback`;
        
        console.log('发起Token交换请求:', {
          tokenEndpoint: '/oauth2/token',
          grantType: 'authorization_code',
          redirectUri: redirectUri,
          codeLength: authCode.length,
          codeVerifierLength: codeVerifier.length
        });
        
        // 使用封装的exchangeCodeForToken函数交换Token
        const responseData = await exchangeCodeForToken(
          authCode,
          codeVerifier,
          'webapp-client',
          redirectUri
        );
        
        // 处理响应
        console.log('Token交换成功:', responseData);
        setTokenData(responseData);
        
        // 保存Token到localStorage
        saveTokens(responseData);
        
        // 清除临时PKCE参数
        sessionStorage.removeItem('pkce_code_verifier');
        sessionStorage.removeItem('pkce_state');
        
        setSuccess(true);
        setLoading(false);
      } catch (error: any) {
        console.error('Token交换失败:', error);
        setError(error.message || '授权过程发生错误');
        setLoading(false);
      }
    };
    
    handleCallback();
  }, []);
  
  // 返回发起页面
  const goToInitiator = () => {
    window.location.href = '/test/admin-auth-initiator';
  };
  
  // 返回首页
  const goToHome = () => {
    window.location.href = '/';
  };
  
  if (loading) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        minHeight: '100vh',
        padding: '20px'
      }}>
        <Card style={{ width: '100%', maxWidth: 500 }}>
          <div style={{ textAlign: 'center', padding: '30px 0' }}>
            <Spin size="large" />
            <Paragraph style={{ marginTop: 20 }}>
              正在处理授权响应，交换Token中...
            </Paragraph>
            {code && (
              <Alert
                message="已获取授权码"
                description={`授权码: ${code.substring(0, 10)}...`}
                type="success"
                showIcon
                style={{ marginTop: 20, textAlign: 'left' }}
              />
            )}
          </div>
        </Card>
      </div>
    );
  }
  
  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      minHeight: '100vh',
      padding: '20px'
    }}>
      <Card 
        style={{ width: '100%', maxWidth: 600 }}
        title={
          <Title level={3} style={{ marginBottom: 0, textAlign: 'center' }}>
            OAuth2授权结果
          </Title>
        }
      >
        {success ? (
          <>
            <Result
              status="success"
              icon={<CheckCircleOutlined />}
              title="授权成功"
              subTitle="已成功完成OAuth2授权码流程并获取Token"
              extra={[
                <Button 
                  type="primary" 
                  key="home" 
                  icon={<HomeOutlined />}
                  onClick={goToHome}
                >
                  返回首页
                </Button>,
                <Button 
                  key="again" 
                  icon={<ReloadOutlined />}
                  onClick={goToInitiator}
                >
                  再次测试
                </Button>
              ]}
            />
            
            {tokenData && (
              <>
                <Divider>Token信息</Divider>
                <div style={{ background: '#f5f5f5', padding: 16, borderRadius: 4, maxHeight: 300, overflow: 'auto' }}>
                  <pre style={{ margin: 0 }}>
                    {JSON.stringify(tokenData, null, 2)}
                  </pre>
                </div>
                
                <Alert
                  message="Token已保存"
                  description="Token已成功保存到localStorage中，可用于后续API请求的授权"
                  type="info"
                  showIcon
                  style={{ marginTop: 20 }}
                />
              </>
            )}
          </>
        ) : (
          <Result
            status="error"
            icon={<ExclamationCircleOutlined />}
            title="授权失败"
            subTitle={error || '授权过程发生错误'}
            extra={[
              <Button 
                type="primary" 
                key="retry"
                onClick={goToInitiator}
              >
                重新发起授权
              </Button>,
              <Button key="home" onClick={goToHome}>返回首页</Button>
            ]}
          />
        )}
      </Card>
    </div>
  );
};

export default OAuth2Callback; 