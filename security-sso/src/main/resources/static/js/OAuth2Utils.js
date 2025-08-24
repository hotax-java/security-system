/**
 * OAuth2 URL构建工具类
 * 支持PKCE和非PKCE模式的URL构建
 */
class OAuth2Utils {

    /**
     * 构建PKCE模式的OAuth2授权URL
     * @param {string} platform OAuth平台名称
     * @param {string} clientId 客户端ID
     * @param {string} redirectUri 重定向URI
     * @param {string} scope 授权范围
     * @param {string} codeChallenge code_challenge参数
     * @param {string} state state参数
     * @returns {string} 完整的授权URL
     */
    static buildPkceAuthUrl(platform, clientId, redirectUri, scope, codeChallenge, state) {
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
     * 构建非PKCE模式的OAuth2授权URL
     * @param {string} platform OAuth平台名称
     * @param {string} clientId 客户端ID
     * @param {string} redirectUri 重定向URI
     * @param {string} scope 授权范围
     * @param {string} state state参数
     * @returns {string} 完整的授权URL
     */
    static buildBasicAuthUrl(platform, clientId, redirectUri, scope, state) {
        const baseUrl = `/oauth2/${platform}/authorize`;
        const params = new URLSearchParams({
            response_type: 'code',
            client_id: clientId,
            redirect_uri: redirectUri,
            scope: scope,
            state: state,
            access_type: 'offline'
        });
        
        return `${baseUrl}?${params.toString()}`;
    }

    /**
     * 处理PKCE模式的第三方登录
     * @param {string} platform OAuth平台名称
     * @param {Object} options 配置选项
     * @param {string} options.clientId 客户端ID，默认为'webapp-client'
     * @param {string} options.redirectUri 重定向URI，默认为'http://localhost:9000/test/callback'
     * @param {string} options.scope 授权范围，默认为'read write openid profile offline_access'
     */
    static async handlePkceLogin(platform, options = {}) {
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
            const pkceParams = await PkceUtils.generatePkceParams();
            
            // 生成state参数
            const state = PkceUtils.generateRandomString(32);
            
            // 存储到localStorage
            PkceUtils.storePkceParams(
                pkceParams.codeVerifier,
                pkceParams.codeChallenge,
                state,
                platform
            );
            
            // 构建授权URL
            const authUrl = this.buildPkceAuthUrl(
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
     * 处理非PKCE模式的第三方登录
     * @param {string} platform OAuth平台名称
     * @param {Object} options 配置选项
     * @param {string} options.clientId 客户端ID，默认为'webapp-client'
     * @param {string} options.redirectUri 重定向URI，默认为'http://localhost:9000/test/callback'
     * @param {string} options.scope 授权范围，默认为'read write openid profile offline_access'
     */
    static handleBasicLogin(platform, options = {}) {
        try {
            console.log(`开始${platform}基础OAuth2登录流程`);
            
            // 默认配置
            const config = {
                clientId: 'webapp-client',
                redirectUri: 'http://localhost:9000/test/callback',
                scope: 'read write openid profile offline_access',
                ...options
            };
            
            // 生成state参数
            const state = PkceUtils.generateRandomString(32);
            
            // 存储到localStorage
            PkceUtils.storeOAuthParams(state, platform);
            
            // 构建授权URL
            const authUrl = this.buildBasicAuthUrl(
                platform,
                config.clientId,
                config.redirectUri,
                config.scope,
                state
            );
            
            console.log(`跳转到${platform}授权页面:`, authUrl);
            
            // 跳转到授权页面
            window.location.href = authUrl;
            
        } catch (error) {
            console.error(`${platform}基础OAuth2登录失败:`, error);
            alert(`${platform}登录失败: ` + error.message);
        }
    }
}