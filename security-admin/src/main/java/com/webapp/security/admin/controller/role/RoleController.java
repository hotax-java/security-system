package com.webapp.security.admin.controller.role;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.webapp.security.admin.controller.permission.vo.PermissionVO;
import com.webapp.security.admin.controller.role.dto.PermissionIdsDTO;
import com.webapp.security.admin.controller.role.dto.RoleCreateDTO;
import com.webapp.security.admin.controller.role.dto.RoleUpdateDTO;
import com.webapp.security.admin.controller.role.vo.RoleVO;
import com.webapp.security.admin.controller.user.dto.StatusDTO;
import com.webapp.security.admin.converter.PermissionConverter;
import com.webapp.security.admin.converter.RoleConverter;
import com.webapp.security.core.entity.SysPermission;
import com.webapp.security.core.entity.SysRole;
import com.webapp.security.core.model.PagedDTO;
import com.webapp.security.core.model.PagedResult;
import com.webapp.security.core.model.ResponseResult;
import com.webapp.security.core.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final SysRoleService roleService;
    private final RoleConverter roleConverter;
    private final PermissionConverter permissionConverter;

    @PostMapping("/paged")
    @PreAuthorize("hasAuthority('ROLE_QUERY')")
    public ResponseResult<PagedResult<RoleVO>> findRolePaged(@RequestBody PagedDTO paged) {
        Page<SysRole> page = new Page<>(paged.getPageNum(), paged.getPageSize());
        String keyword = paged.getKeyword();
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<SysRole>()
                .like(StrUtil.isNotBlank(keyword), SysRole::getRoleName, keyword)
                .or(StrUtil.isNotBlank(keyword), condition -> condition
                        .like(SysRole::getRoleCode, keyword));

        Page<SysRole> pageResult = roleService.page(page, queryWrapper);
        List<RoleVO> voList = roleConverter.toVOList(pageResult.getRecords());
        return ResponseResult.success(new PagedResult<>(voList, pageResult.getTotal()));
    }

    /**
     * 获取所有角色
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_QUERY')")
    public ResponseResult<List<RoleVO>> getAllRoles() {
        List<SysRole> roles = roleService.list();
        return ResponseResult.success(roleConverter.toVOList(roles));
    }

    /**
     * 根据ID获取角色
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_QUERY')")
    public ResponseResult<RoleVO> getRoleById(@PathVariable Long id) {
        SysRole role = roleService.getById(id);
        if (role == null) {
            return ResponseResult.failed("角色不存在");
        }
        return ResponseResult.success(roleConverter.toVO(role));
    }

    /**
     * 创建角色
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public ResponseResult<Void> createRole(@Validated @RequestBody RoleCreateDTO dto) {
        SysRole role = roleConverter.fromCreateDTO(dto);
        boolean success = roleService.createRole(role);
        return success ? ResponseResult.success(null, "角色创建成功") : ResponseResult.failed("角色创建失败");
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseResult<Void> updateRole(@PathVariable Long id, @Validated @RequestBody RoleUpdateDTO dto) {
        SysRole role = roleService.getById(id);
        if (role == null) {
            return ResponseResult.failed("角色不存在");
        }

        roleConverter.updateEntityFromDTO(dto, role);
        boolean success = roleService.updateRole(role);
        return success ? ResponseResult.success(null, "角色更新成功") : ResponseResult.failed("角色更新失败");
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public ResponseResult<Void> deleteRole(@PathVariable Long id) {
        boolean success = roleService.deleteRole(id);
        return success ? ResponseResult.success(null, "角色删除成功") : ResponseResult.failed("角色删除失败");
    }

    /**
     * 更新角色状态
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseResult<Void> updateRoleStatus(@PathVariable Long id, @Validated @RequestBody StatusDTO dto) {
        boolean success = roleService.updateRoleStatus(id, dto.getStatus());
        return success ? ResponseResult.success(null, "角色状态更新成功") : ResponseResult.failed("角色状态更新失败");
    }

    /**
     * 分配角色权限
     */
    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseResult<Void> assignPermissions(@PathVariable Long id, @Validated @RequestBody PermissionIdsDTO dto) {
        boolean success = roleService.assignPermissions(id, dto.getPermissionIds());
        return success ? ResponseResult.success(null, "权限分配成功") : ResponseResult.failed("权限分配失败");
    }

    /**
     * 获取角色权限
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_QUERY')")
    public ResponseResult<List<PermissionVO>> getRolePermissions(@PathVariable Long id) {
        List<SysPermission> permissions = roleService.getRolePermissionVOs(id);
        return ResponseResult.success(permissionConverter.toVOList(permissions));
    }

    /**
     * 获取启用的角色
     */
    @GetMapping("/enabled")
    @PreAuthorize("hasAuthority('ROLE_QUERY')")
    public ResponseResult<List<RoleVO>> getEnabledRoles() {
        List<SysRole> roles = roleService.getEnabledRoles();
        return ResponseResult.success(roleConverter.toVOList(roles));
    }
}
