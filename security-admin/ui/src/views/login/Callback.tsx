import { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Card, message, Spin, Typography, Avatar, Button } from 'antd';
import { UserOutlined, GithubOutlined, WechatOutlined } from '@ant-design/icons';
import { businessApi } from '../../apis/api';
import { TokenManager } from '../../services/tokenManager';
import { AuthConfigService } from '../../services/authConfigService';
import { PkceUtils } from '../../utils/pkceUtils';
import { PkceApiService } from '../../services/pkceApiService';
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
    const [platform, setPlatform] = useState<'github' | 'wechat' | 'alipay' | ''>('');

    useEffect(() => {
        debugger
        // 解析URL参数
        const params = new URLSearchParams(location.search);
        const code = params.get('code');
        const state = params.get('state') || '';
        const clientId = params.get('clientId') || '';
        const platform = params.get('platform');

        if (!code) {
            setError('授权码不存在，请重新登录');
            return;
        }

        // state参数包含平台信息
        if (platform) {
            setPlatform(state as 'github' | 'wechat' | 'alipay' | '');
        }

        // 检查是否启用PKCE
        checkPkceAndExchangeToken(code, state, clientId);
    }, [location.search]);

    // 检查是否启用PKCE并交换令牌
    const checkPkceAndExchangeToken = async (code: string, state: string, clientId: string) => {
        try {
            // 获取PKCE配置
            const pkceEnabled = await AuthConfigService.isPkceEnabled();

            if (pkceEnabled) {
                // PKCE模式：通过API接口根据state获取code_verifier
                try {
                    const codeVerifier = await PkceApiService.getCodeVerifier(state);
                    console.log('成功从后端获取code_verifier');
                    // 使用PKCE模式交换令牌
                    exchangeTokenWithPkce(code, state, clientId, codeVerifier);
                } catch (error) {
                    console.error('从后端获取code_verifier失败:', error);
                    setError('获取PKCE参数失败，请重新登录');
                    setLoading(false);
                    return;
                }
            } else {
                // 传统模式交换令牌
                exchangeTokenWithCode(code, state, clientId);
            }
        } catch (error) {
            console.error('获取PKCE配置失败，将使用传统模式:', error);
            exchangeTokenWithCode(code, state, clientId);
        }
    };

    // 通过Admin后端代理获取token (传统模式)
    const exchangeTokenWithCode = async (code: string, state: string, clientId: string) => {
        try {
            setLoading(true);

            // 使用businessApi调用后端代理接口
            const response:TokenResponse = await businessApi.post('/api/oauth2/token', {
                code: code,
                state: state,
                clientId: clientId || 'webapp-client'
            });

            if (response && response.access_token) {
                // 清除可能存在的PKCE参数
                PkceUtils.clearPkceParamsByState(state);
                
                handleLoginSuccess(response);
            } else {
                setError('Token获取失败');
                setLoading(false);
            }
        } catch (error: any) {
            console.error('传统Token获取失败:', error);
            setError(error.response?.data?.message || 'Token获取失败');
            setLoading(false);
            
            // 清除可能存在的PKCE参数
            PkceUtils.clearPkceParamsByState(state);
        }
    };

    // 使用PKCE模式获取token
    const exchangeTokenWithPkce = async (code: string, state: string, clientId: string, codeVerifier: string) => {
        try {
            setLoading(true);
            console.log('使用PKCE模式交换令牌');

            // 使用businessApi调用后端代理接口，附加codeVerifier
            const response:TokenResponse = await businessApi.post('/api/oauth2/token', {
                code: code,
                state: state,
                clientId: clientId || 'webapp-client',
                codeVerifier: codeVerifier  // 添加codeVerifier参数
            });

            if (response && response.access_token) {
                // 清除后端存储的PKCE参数
                try {
                    await PkceApiService.cleanupPkceParams(state);
                } catch (error) {
                    console.warn('清理PKCE参数失败:', error);
                }

                handleLoginSuccess(response);
            } else {
                setError('Token获取失败');
                setLoading(false);
            }
        } catch (error: any) {
            console.error('PKCE Token获取失败:', error);
            setError(error.response?.data?.message || 'Token获取失败');
            setLoading(false);

            // 清除后端存储的PKCE参数
            try {
                await PkceApiService.cleanupPkceParams(state);
            } catch (cleanupError) {
                console.warn('清理PKCE参数失败:', cleanupError);
            }
        }
    };

    const handleLoginSuccess = (tokenData: TokenResponse) => {
        // 提取完整的token信息
        debugger
        const { access_token, refresh_token, expires_in } = tokenData;

        // 解析过期时间，确保它是一个数字
        let validExpiresIn = expires_in;
        if (typeof expires_in === 'string') {
            validExpiresIn = parseInt(expires_in, 10);
        }

        // 使用TokenManager保存完整的token信息
        TokenManager.saveTokens(access_token, refresh_token, validExpiresIn);

        // 登录成功后清理后端存储的PKCE参数
        // 注意：这里不需要再次清理，因为在exchangeTokenWithPkce中已经清理过了
        console.log('OAuth2回调登录成功');

        // 调用登录回调
        onLogin({ username: `${platform || 'third_party'}_user` }, access_token);

        // 跳转到首页
        navigate('/dashboard');
        message.success('登录成功');
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