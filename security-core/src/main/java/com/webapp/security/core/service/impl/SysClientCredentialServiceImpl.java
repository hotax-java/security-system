package com.webapp.security.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.webapp.security.core.entity.SysClientCredential;
import com.webapp.security.core.mapper.SysClientCredentialMapper;
import com.webapp.security.core.service.SysClientCredentialService;
import com.webapp.security.core.config.ClientIdConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class SysClientCredentialServiceImpl extends ServiceImpl<SysClientCredentialMapper, SysClientCredential>
        implements SysClientCredentialService {

    private final SysClientCredentialMapper credentialMapper;
    private final PasswordEncoder passwordEncoder;
    private final ClientIdConfig clientIdConfig;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public SysClientCredential findByAppId(String appId) {
        return credentialMapper.findByAppId(appId);
    }

    @Override
    public boolean disableByAppId(String appId) {
        SysClientCredential cred = credentialMapper.findByAppId(appId);
        if (cred == null)
            return false;
        cred.setStatus(0);
        return credentialMapper.updateById(cred) > 0;
    }

    @Override
    public SysClientCredential createCredential(String remark) {
        String appId = generateId();
        String plain = generateSecret();
        SysClientCredential entity = new SysClientCredential();
        entity.setAppId(appId);
        entity.setAppSecret(passwordEncoder.encode(plain));
        entity.setClientId(clientIdConfig.getApiClientId()); // 使用配置的API客户端ID
        entity.setRemark(remark);
        entity.setStatus(1);
        this.save(entity);

        // 临时保存明文密钥以返回给客户端
        entity.setPlainSecret(plain);

        return entity;
    }

    @Override
    public SysClientCredential generateCredential() {
        String appId = generateId();
        String plainSecret = generateSecret();

        SysClientCredential entity = new SysClientCredential();
        entity.setAppId(appId);
        entity.setPlainSecret(plainSecret); // 设置明文密钥，但不保存到数据库

        return entity;
    }

    @Override
    public SysClientCredential saveCredential(String appId, String plainSecret, String remark) {
        // 验证appId是否已存在
        SysClientCredential existing = credentialMapper.findByAppId(appId);
        if (existing != null) {
            throw new IllegalArgumentException("AppID已存在");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;

        SysClientCredential entity = new SysClientCredential();
        entity.setAppId(appId);
        entity.setAppSecret(passwordEncoder.encode(plainSecret));
        entity.setClientId(clientIdConfig.getApiClientId()); // 使用配置的API客户端ID
        entity.setCreateBy(username);
        entity.setUpdateBy(username);
        entity.setRemark(remark);
        entity.setStatus(1);
        this.save(entity);

        // 临时保存明文密钥以返回给客户端
        entity.setPlainSecret(plainSecret);

        return entity;
    }

    @Override
    public void updateStatus(String appId, Integer status) {
        SysClientCredential cred = credentialMapper.findByAppId(appId);
        if (cred == null)
            throw new RuntimeException("凭证不存在: " + appId);
        cred.setStatus(status);
        boolean updated = credentialMapper.updateById(cred) > 0;
        if (!updated) {
            throw new RuntimeException("更新状态失败");
        }
    }

    private String generateId() {
        byte[] b = new byte[12];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private String generateSecret() {
        byte[] b = new byte[24];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}