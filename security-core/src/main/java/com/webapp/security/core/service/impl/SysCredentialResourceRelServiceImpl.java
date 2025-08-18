package com.webapp.security.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.webapp.security.core.entity.SysClientCredential;
import com.webapp.security.core.entity.SysCredentialResourceRel;
import com.webapp.security.core.entity.SysResource;
import com.webapp.security.core.mapper.SysClientCredentialMapper;
import com.webapp.security.core.mapper.SysCredentialResourceRelMapper;
import com.webapp.security.core.mapper.SysResourceMapper;
import com.webapp.security.core.service.SysCredentialResourceRelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysCredentialResourceRelServiceImpl
        extends ServiceImpl<SysCredentialResourceRelMapper, SysCredentialResourceRel>
        implements SysCredentialResourceRelService {

    private final SysClientCredentialMapper credentialMapper;
    private final SysResourceMapper resourceMapper;

    @Override
    public List<Long> listResourceIdsByCredentialId(Long credentialId) {
        // 验证凭证是否存在
        SysClientCredential credential = credentialMapper.selectById(credentialId);
        if (credential == null) {
            throw new RuntimeException("凭证不存在，ID: " + credentialId);
        }

        List<SysCredentialResourceRel> list = this.list(new LambdaQueryWrapper<SysCredentialResourceRel>()
                .eq(SysCredentialResourceRel::getCredentialId, credentialId));
        return list.stream().map(SysCredentialResourceRel::getResourceId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignResources(Long credentialId, List<Long> resourceIds) throws RuntimeException {
        // 验证凭证是否存在
        SysClientCredential credential = credentialMapper.selectById(credentialId);
        if (credential == null) {
            throw new RuntimeException("凭证不存在，ID: " + credentialId);
        }

        // 验证资源是否都存在
        if (resourceIds != null && !resourceIds.isEmpty()) {
            List<SysResource> resources = resourceMapper.selectBatchIds(resourceIds);
            if (resources.size() != resourceIds.size()) {
                throw new RuntimeException("部分资源不存在，请检查资源ID");
            }
        }

        // 执行替换操作
        replaceAssignments(credentialId, resourceIds);
    }

    @Override
    @Transactional
    public void replaceAssignments(Long credentialId, List<Long> resourceIds) {
        // 删除旧的
        this.remove(new LambdaQueryWrapper<SysCredentialResourceRel>()
                .eq(SysCredentialResourceRel::getCredentialId, credentialId));
        // 新增
        if (resourceIds != null && !resourceIds.isEmpty()) {
            for (Long rid : resourceIds) {
                SysCredentialResourceRel rel = new SysCredentialResourceRel();
                rel.setCredentialId(credentialId);
                rel.setResourceId(rid);
                this.save(rel);
            }
        }
    }
}