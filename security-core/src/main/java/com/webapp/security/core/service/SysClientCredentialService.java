package com.webapp.security.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.webapp.security.core.entity.SysClientCredential;

public interface SysClientCredentialService extends IService<SysClientCredential> {

    /**
     * 根据AppId查找凭证
     */
    SysClientCredential findByAppId(String appId);

    /**
     * 禁用凭证
     */
    boolean disableByAppId(String appId);

    /**
     * 在服务中生成 appId 与 appSecret（加密存储），并返回持久化后的实体
     */
    SysClientCredential createCredential(String remark);

    /**
     * 生成临时凭证信息，不保存到数据库
     * 
     * @return 包含appId和明文appSecret的凭证对象
     */
    SysClientCredential generateCredential();

    /**
     * 保存预先生成的凭证信息
     * 
     * @param appId       AppID
     * @param plainSecret 明文AppSecret
     * @param remark      备注信息
     * @return 保存后的凭证对象
     */
    SysClientCredential saveCredential(String appId, String plainSecret, String remark);

    /**
     * 更新凭证状态
     * 
     * @param appId  应用ID
     * @param status 状态值（1:启用, 0:禁用）
     * @throws RuntimeException 如果凭证不存在
     */
    void updateStatus(String appId, Integer status) throws RuntimeException;
}