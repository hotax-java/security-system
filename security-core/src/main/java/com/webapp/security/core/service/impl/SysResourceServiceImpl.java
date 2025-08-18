package com.webapp.security.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.webapp.security.core.entity.SysResource;
import com.webapp.security.core.mapper.SysResourceMapper;
import com.webapp.security.core.service.SysResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SysResourceServiceImpl extends ServiceImpl<SysResourceMapper, SysResource> implements SysResourceService {

    private final SysResourceMapper resourceMapper;

    @Override
    public SysResource createResource(SysResource resource) throws RuntimeException {
        boolean success = this.save(resource);
        if (!success) {
            throw new RuntimeException("创建资源失败");
        }
        return resource;
    }

    @Override
    public SysResource updateResource(Long id, SysResource resource) throws RuntimeException {
        // 检查资源是否存在
        SysResource existingResource = resourceMapper.selectById(id);
        if (existingResource == null) {
            throw new RuntimeException("资源不存在，ID: " + id);
        }

        // 设置ID
        resource.setResourceId(id);

        // 更新资源
        boolean success = this.updateById(resource);
        if (!success) {
            throw new RuntimeException("更新资源失败，ID: " + id);
        }

        return resource;
    }

    @Override
    public void deleteResource(Long id) throws RuntimeException {
        // 检查资源是否存在
        SysResource existingResource = resourceMapper.selectById(id);
        if (existingResource == null) {
            throw new RuntimeException("资源不存在，ID: " + id);
        }

        // 删除资源
        boolean success = this.removeById(id);
        if (!success) {
            throw new RuntimeException("删除资源失败，ID: " + id);
        }
    }
}