package com.webapp.security.admin.controller.role.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 权限ID列表DTO
 */
@Data
public class PermissionIdsDTO {
    /**
     * 权限ID列表
     */
    @NotEmpty(message = "权限ID列表不能为空")
    private List<Long> permissionIds;
}