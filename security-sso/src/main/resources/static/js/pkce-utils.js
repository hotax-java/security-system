/**
 * PKCE (Proof Key for Code Exchange) 工具类
 * 用于OAuth2授权码流程的安全增强
 */
class PkceUtils {
    
    /**
     * 生成随机字符串
     * @param {number} length 字符串长度
     * @returns {string} 随机字符串
     */
    static generateRandomString(length) {
        const charset = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';
        let result = '';
        for (let i = 0; i < length; i++) {
            result += charset.charAt(Math.floor(Math.random() * charset.length));
        }
        return result;
    }

    /**
     * SHA256 哈希函数
     * @param {string} plain 待哈希的字符串
     * @returns {Promise<ArrayBuffer>} 哈希结果
     */
    static async sha256(plain) {
        const encoder = new TextEncoder();
        const data = encoder.encode(plain);
        const hash = await crypto.subtle.digest('SHA-256', data);
        return hash;
    }

    /**
     * Base64 URL 编码
     * @param {ArrayBuffer} arrayBuffer 待编码的数据
     * @returns {string} Base64 URL编码结果
     */
    static base64UrlEncode(arrayBuffer) {
        const bytes = new Uint8Array(arrayBuffer);
        let binary = '';
        for (let i = 0; i < bytes.byteLength; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return btoa(binary)
            .replace(/\+/g, '-')
            .replace(/\//g, '_')
            .replace(/=/g, '');
    }

    /**
     * 生成PKCE参数
     * @returns {Promise<Object>} 包含codeVerifier和codeChallenge的对象
     */
    static async generatePkceParams() {
        try {
            // 生成 code_verifier (43-128 字符)
            const codeVerifier = this.generateRandomString(128);

            // 生成 code_challenge
            const hashed = await this.sha256(codeVerifier);
            const codeChallenge = this.base64UrlEncode(hashed);

            console.log('PKCE Parameters Generated:');
            console.log('code_verifier:', codeVerifier);
            console.log('code_challenge:', codeChallenge);

            return {
                codeVerifier: codeVerifier,
                codeChallenge: codeChallenge
            };
        } catch (error) {
            console.error('PKCE 参数生成失败:', error);
            throw error;
        }
    }

    /**
     * 存储PKCE参数到localStorage
     * @param {string} codeVerifier code_verifier参数
     * @param {string} codeChallenge code_challenge参数
     * @param {string} state state参数
     * @param {string} platform OAuth平台名称
     */
    static storePkceParams(codeVerifier, codeChallenge, state, platform) {
        localStorage.setItem('code_verifier', codeVerifier);
        localStorage.setItem('code_challenge', codeChallenge);
        localStorage.setItem('state', state);
        localStorage.setItem('oauth_platform', platform);
        
        console.log('PKCE参数已存储到localStorage');
        console.log('platform:', platform);
        console.log('state:', state);
    }

    /**
     * 从localStorage获取PKCE参数
     * @returns {Object} PKCE参数对象
     */
    static getPkceParams() {
        return {
            codeVerifier: localStorage.getItem('code_verifier'),
            codeChallenge: localStorage.getItem('code_challenge'),
            state: localStorage.getItem('state'),
            platform: localStorage.getItem('oauth_platform')
        };
    }

    /**
     * 清除localStorage中的PKCE参数
     */
    static clearPkceParams() {
        localStorage.removeItem('code_verifier');
        localStorage.removeItem('code_challenge');
        localStorage.removeItem('state');
        localStorage.removeItem('oauth_platform');
        console.log('PKCE参数已从localStorage清除');
    }

    /**
     * 构建OAuth2授权URL
     * @param {string} platform OAuth平台名称
     * @param {string} clientId 客户端ID
     * @param {string} redirectUri 重定向URI
     * @param {string} scope 授权范围
     * @param {string} codeChallenge code_challenge参数
     * @param {string} state state参数
     * @returns {string} 完整的授权URL
     */
    static buildAuthUrl(platform, clientId, redirectUri, scope, codeChallenge, state) {
        const baseUrl = `/oauth2/${platform}/authorize`;
        const params = new URLSearchParams({
            response_type: 'code',
            client_id: clientId,
            redirect_uri: redirectUri,
            scope: scope,
            code_challenge_method: 'S256',
            code_challenge: codeChallenge,
            state: state,
            access_type: 'offline'
        });
        
        return `${baseUrl}?${params.toString()}`;
    }

    /**
     * 处理第三方登录（通用方法）
     * @param {string} platform OAuth平台名称
     * @param {Object} options 配置选项
     * @param {string} options.clientId 客户端ID，默认为'webapp-client'
     * @param {string} options.redirectUri 重定向URI，默认为'http://localhost:9000/test/callback'
     * @param {string} options.scope 授权范围，默认为'read write openid profile offline_access'
     */
    static async handleThirdPartyLogin(platform, options = {}) {
        try {
            console.log(`开始${platform}PKCE登录流程`);
            
            // 默认配置
            const config = {
                clientId: 'webapp-client',
                redirectUri: 'http://localhost:9000/test/callback',
                scope: 'read write openid profile offline_access',
                ...options
            };
            
            // 生成PKCE参数
            const pkceParams = await this.generatePkceParams();
            
            // 生成state参数
            const state = this.generateRandomString(32);
            
            // 存储到localStorage
            this.storePkceParams(
                pkceParams.codeVerifier,
                pkceParams.codeChallenge,
                state,
                platform
            );
            
            // 构建授权URL
            const authUrl = this.buildAuthUrl(
                platform,
                config.clientId,
                config.redirectUri,
                config.scope,
                pkceParams.codeChallenge,
                state
            );
            
            console.log(`跳转到${platform}授权页面:`, authUrl);
            
            // 跳转到授权页面
            window.location.href = authUrl;
            
        } catch (error) {
            console.error(`${platform}PKCE登录失败:`, error);
            alert(`${platform}登录失败: ` + error.message);
        }
    }

    /**
     * 支付宝登录
     * @param {Object} options 配置选项
     */
    static async alipayLogin(options = {}) {
        await this.handleThirdPartyLogin('alipay', options);
    }

    /**
     * 微信登录
     * @param {Object} options 配置选项
     */
    static async wechatLogin(options = {}) {
        await this.handleThirdPartyLogin('wechat', options);
    }

    /**
     * GitHub登录
     * @param {Object} options 配置选项
     */
    static async githubLogin(options = {}) {
        await this.handleThirdPartyLogin('github', options);
    }
}

// 导出到全局作用域，方便在HTML页面中使用
if (typeof window !== 'undefined') {
    window.PkceUtils = PkceUtils;
}

// 如果支持模块导出，也提供模块导出
if (typeof module !== 'undefined' && module.exports) {
    module.exports = PkceUtils;
}