package com.webapp.security.admin.controller.syscredentialresourcerel;

import com.webapp.security.admin.controller.syscredentialresourcerel.dto.AssignDTO;
import com.webapp.security.core.model.ResponseResult;
import com.webapp.security.core.service.SysCredentialResourceRelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sys-credential-resource-rel")
@RequiredArgsConstructor
public class SysCredentialResourceRelController {

    private final SysCredentialResourceRelService relService;

    @GetMapping("/{credentialId}/resource-ids")
    @PreAuthorize("hasAuthority('OPENAPI_PERMISSION_QUERY')")
    public ResponseResult<List<Long>> listResourceIds(@PathVariable Long credentialId) {
        try {
            // 调用Service获取资源ID列表
            List<Long> resourceIds = relService.listResourceIdsByCredentialId(credentialId);
            return ResponseResult.success(resourceIds);
        } catch (Exception e) {
            return ResponseResult.failed("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/{credentialId}/assign")
    @PreAuthorize("hasAuthority('OPENAPI_PERMISSION_ASSIGN')")
    public ResponseResult<Void> assign(@PathVariable Long credentialId, @Validated @RequestBody AssignDTO dto) {
        try {
            // 调用Service分配资源
            relService.assignResources(credentialId, dto.getResourceIds());
            return ResponseResult.success(null, "授权成功");
        } catch (Exception e) {
            return ResponseResult.failed("授权失败: " + e.getMessage());
        }
    }
}