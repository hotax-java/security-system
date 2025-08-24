/**
 * PKCE (Proof Key for Code Exchange) 工具类
 * 专注于PKCE参数的生成和存储
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
     * 存储OAuth参数到localStorage（非PKCE模式）
     * @param {string} state state参数
     * @param {string} platform OAuth平台名称
     */
    static storeOAuthParams(state, platform) {
        localStorage.setItem('state', state);
        localStorage.setItem('oauth_platform', platform);
        
        console.log('OAuth参数已存储到localStorage');
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
}