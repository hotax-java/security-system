package com.webapp.security.sso.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.webapp.security.core.entity.SysClientCredential;
import com.webapp.security.core.entity.SysCredentialResourceRel;
import com.webapp.security.core.entity.SysResource;
import com.webapp.security.core.service.SysClientCredentialService;
import com.webapp.security.core.service.SysCredentialResourceRelService;
import com.webapp.security.core.service.SysResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 令牌自省服务
 * 处理令牌自省请求并添加权限信息
 */
@Service
public class TokenIntrospectionService {

    private static final Logger logger = LoggerFactory.getLogger(TokenIntrospectionService.class);

    private final SysClientCredentialService clientCredentialService;
    private final SysCredentialResourceRelService credentialResourceRelService;
    private final SysResourceService resourceService;

    @Autowired
    public TokenIntrospectionService(
            SysClientCredentialService clientCredentialService,
            SysCredentialResourceRelService credentialResourceRelService,
            SysResourceService resourceService) {
        this.clientCredentialService = clientCredentialService;
        this.credentialResourceRelService = credentialResourceRelService;
        this.resourceService = resourceService;
    }

    /**
     * 处理令牌自省请求
     *
     * @param authorization 授权信息
     * @return 令牌自省结果
     */
    public Map<String, Object> introspect(OAuth2Authorization authorization) {
        try {
            // 获取令牌属性
            Map<String, Object> claims = new HashMap<>();
            claims.put("active", true);

            String clientId = authorization.getRegisteredClientId();
            claims.put("client_id", clientId);

            // 如果有主体名称，添加到声明中
            if (authorization.getPrincipalName() != null) {
                claims.put("username", authorization.getPrincipalName());
            }

            OAuth2Authorization.Token<?> accessToken = authorization.getToken(OAuth2TokenType.ACCESS_TOKEN.getValue());
            if (accessToken != null) {
                if (accessToken.getToken().getIssuedAt() != null) {
                    claims.put("iat", accessToken.getToken().getIssuedAt().getEpochSecond());
                }
                if (accessToken.getToken().getExpiresAt() != null) {
                    claims.put("exp", accessToken.getToken().getExpiresAt().getEpochSecond());
                }
            }

            // 添加受众
            claims.put("aud", Arrays.asList("api://default"));
            String appId = authorization.getAttribute("app_id");
            // 从数据库获取权限信息
            //List<String> permissions = getPermissionsFromDatabase(appId);
            //if (!permissions.isEmpty()) {
            //    claims.put("authorities", String.join(" ", permissions));
            //    claims.put("scope", String.join(" ", permissions));
            //} else {
            //    logger.warn("No permissions found for client: {}", clientId);
            //}

            String permissions = authorization.getAttribute("permissions");
            if (permissions != null) {
                claims.put("authorities", permissions);
                claims.put("scope", permissions);
            }

            logger.info("Token introspection completed successfully with claims: {}", claims);
            return claims;
        } catch (Exception e) {
            logger.error("Error during token introspection", e);
            Map<String, Object> response = new HashMap<>();
            response.put("active", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    /**
     * 从数据库获取客户端对应的权限
     *
     * @param appId 客户端ID
     * @return 权限列表
     */
    public List<String> getPermissionsFromDatabase(String appId) {
        logger.info("Getting permissions from database for client: {}", appId);

        List<String> permissions = new ArrayList<>();
        try {
            // 1. 首先根据clientId查找对应的凭证
            logger.info("Looking up credential for appId: {}", appId);
            SysClientCredential credential = clientCredentialService.getOne(
                    new LambdaQueryWrapper<SysClientCredential>()
                            .eq(SysClientCredential::getAppId, appId)
                            .eq(SysClientCredential::getStatus, 1));

            if (credential == null) {
                logger.warn("No active credential found for client_id: {}", appId);
                return permissions;
            }

            logger.info("Found credential: id={}, app_id={} for client_id: {}",
                    credential.getId(), credential.getAppId(), appId);

            // 2. 获取凭证对应的资源ID列表
            List<Long> resourceIds = new ArrayList<>();
            // 查询关联表
            logger.info("Looking up resource relations for credential id: {}", credential.getId());
            List<SysCredentialResourceRel> relations = credentialResourceRelService.list(
                    new LambdaQueryWrapper<SysCredentialResourceRel>()
                            .eq(SysCredentialResourceRel::getCredentialId, credential.getId()));

            logger.info("Found {} resource relations for credential id: {}", relations.size(), credential.getId());

            // 提取资源ID
            for (SysCredentialResourceRel rel : relations) {
                resourceIds.add(rel.getResourceId());
                logger.debug("Added resource id: {} to list", rel.getResourceId());
            }

            if (resourceIds.isEmpty()) {
                logger.warn("No resources assigned to credential: {}", credential.getId());
                return permissions;
            }

            logger.info("Found {} resource IDs: {}", resourceIds.size(), resourceIds);

            // 3. 获取资源详情
            logger.info("Looking up resource details for resource ids: {}", resourceIds);
            List<SysResource> resources = resourceService.listByIds(resourceIds)
                    .stream()
                    .filter(r -> r.getStatus() == 1) // 只获取启用的资源
                    .collect(Collectors.toList());

            logger.info("Found {} active resources", resources.size());

            // 4. 提取资源权限代码
            for (SysResource resource : resources) {
                permissions.add(resource.getResourceCode());
                logger.debug("Added permission: {}", resource.getResourceCode());
            }

            logger.info("Final permissions list: {}", permissions);
        } catch (Exception e) {
            logger.error("Error getting permissions for client: " + appId, e);
            logger.error("Exception details:", e);
        }

        return permissions;
    }
}
