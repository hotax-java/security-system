package com.webapp.security.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.webapp.security.core.entity.SysResource;

/**
 * API资源服务接口
 */
public interface SysResourceService extends IService<SysResource> {

    /**
     * 创建API资源
     * 
     * @param resource 资源实体
     * @return 创建的资源
     * @throws RuntimeException 如果创建失败
     */
    SysResource createResource(SysResource resource) throws RuntimeException;

    /**
     * 更新API资源
     * 
     * @param id       资源ID
     * @param resource 资源实体
     * @return 更新后的资源
     * @throws RuntimeException 如果资源不存在或更新失败
     */
    SysResource updateResource(Long id, SysResource resource) throws RuntimeException;

    /**
     * 删除API资源
     * 
     * @param id 资源ID
     * @throws RuntimeException 如果资源不存在或删除失败
     */
    void deleteResource(Long id) throws RuntimeException;
}