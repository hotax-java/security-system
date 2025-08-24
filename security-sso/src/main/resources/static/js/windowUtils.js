// 导出到全局作用域，方便在HTML页面中使用
if (typeof window !== 'undefined') {
    window.PkceUtils = PkceUtils;
    window.OAuth2Utils = OAuth2Utils;
}

// 如果支持模块导出，也提供模块导出
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { PkceUtils, OAuth2Utils };
}