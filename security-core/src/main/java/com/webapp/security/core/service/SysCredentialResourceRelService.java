package com.webapp.security.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.webapp.security.core.entity.SysCredentialResourceRel;

import java.util.List;

/**
 * 凭证资源关联服务接口
 */
public interface SysCredentialResourceRelService extends IService<SysCredentialResourceRel> {

    /**
     * 获取凭证关联的资源ID列表
     * 
     * @param credentialId 凭证ID
     * @return 资源ID列表
     * @throws RuntimeException 如果凭证不存在
     */
    List<Long> listResourceIdsByCredentialId(Long credentialId) throws RuntimeException;

    /**
     * 为凭证分配资源权限
     * 
     * @param credentialId 凭证ID
     * @param resourceIds  资源ID列表
     * @throws RuntimeException 如果凭证不存在或资源不存在
     */
    void assignResources(Long credentialId, List<Long> resourceIds) throws RuntimeException;

    /**
     * 替换凭证的资源关联（内部使用）
     * 
     * @param credentialId 凭证ID
     * @param resourceIds  资源ID列表
     */
    void replaceAssignments(Long credentialId, List<Long> resourceIds);
}