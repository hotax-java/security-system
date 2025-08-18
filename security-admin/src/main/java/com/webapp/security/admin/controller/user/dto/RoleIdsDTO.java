package com.webapp.security.admin.controller.user.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 角色ID列表DTO
 */
@Data
public class RoleIdsDTO {
    /**
     * 角色ID列表
     */
    //@NotEmpty(message = "角色ID列表不能为空")
    private List<Long> roleIds;
}