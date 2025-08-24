package com.webapp.security.sso.auths.third;

public interface ThirdLoginRedisConstant {
    String GITHUB_STATE_PREFIX = "github:oauth2:state:";
    String ALIPAY_STATE_PREFIX = "alipay:oauth2:state:";
    String WECHAT_STATE_PREFIX = "wechat:oauth2:state:";
    Integer STATE_EXPIRE_SECONDS = 600;
}
