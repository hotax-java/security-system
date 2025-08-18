package com.webapp.security.admin.controller.permission;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.webapp.security.admin.controller.permission.dto.PermissionCreateDTO;
import com.webapp.security.admin.controller.permission.dto.PermissionUpdateDTO;
import com.webapp.security.admin.controller.permission.vo.PermissionVO;
import com.webapp.security.admin.controller.role.vo.RoleVO;
import com.webapp.security.admin.controller.user.dto.StatusDTO;
import com.webapp.security.admin.converter.PermissionConverter;
import com.webapp.security.core.entity.SysPermission;
import com.webapp.security.core.entity.SysRole;
import com.webapp.security.core.model.PagedDTO;
import com.webapp.security.core.model.PagedResult;
import com.webapp.security.core.model.ResponseResult;
import com.webapp.security.core.service.SysPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 权限管理控制器
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final SysPermissionService permissionService;
    private final PermissionConverter permissionConverter;

    @PostMapping("/paged")
    @PreAuthorize("hasAuthority('PERMISSION_QUERY')")
    public ResponseResult<PagedResult<PermissionVO>> findPermissionPaged(@RequestBody PagedDTO paged) {
        Page<SysPermission> page = new Page<>(paged.getPageNum(), paged.getPageSize());
        String keyword = paged.getKeyword();
        Page<SysPermission> pageResult = permissionService.page(page, new LambdaQueryWrapper<SysPermission>()
                .like(StrUtil.isNotBlank(keyword), SysPermission::getPermName, keyword)
                .or(StrUtil.isNotBlank(keyword), condition -> condition
                        .like(SysPermission::getPermCode, keyword)));
        List<PermissionVO> voList = permissionConverter.toVOList(pageResult.getRecords());
        return ResponseResult.success(new PagedResult<>(voList, pageResult.getTotal()));
    }

    /**
     * 获取所有权限
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('PERMISSION_QUERY')")
    public ResponseResult<List<PermissionVO>> getAllPermissions() {
        List<SysPermission> permissions = permissionService.list();
        List<PermissionVO> voList = permissionConverter.toVOList(permissions);
        return ResponseResult.success(enrichPermissionVOs(voList));
    }

    /**
     * 根据ID获取权限
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_QUERY')")
    public ResponseResult<PermissionVO> getPermissionById(@PathVariable Long id) {
        SysPermission permission = permissionService.getById(id);
        if (permission == null) {
            return ResponseResult.failed("权限不存在");
        }
        PermissionVO vo = permissionConverter.toVO(permission);

        // 设置父权限名称
        if (permission.getParentId() != null) {
            SysPermission parent = permissionService.getById(permission.getParentId());
            if (parent != null) {
                vo.setParentName(parent.getPermName());
            }
        }

        return ResponseResult.success(vo);
    }

    /**
     * 创建权限
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSION_CREATE')")
    public ResponseResult<Void> createPermission(@Validated @RequestBody PermissionCreateDTO dto) {
        SysPermission permission = permissionConverter.fromCreateDTO(dto);
        boolean success = permissionService.createPermission(permission);
        return success ? ResponseResult.success(null, "权限创建成功") : ResponseResult.failed("权限创建失败");
    }

    /**
     * 更新权限
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_UPDATE')")
    public ResponseResult<Void> updatePermission(@PathVariable Long id,
            @Validated @RequestBody PermissionUpdateDTO dto) {
        SysPermission permission = permissionService.getById(id);
        if (permission == null) {
            return ResponseResult.failed("权限不存在");
        }

        permissionConverter.updateEntityFromDTO(dto, permission);
        boolean success = permissionService.updatePermission(permission);
        return success ? ResponseResult.success(null, "权限更新成功") : ResponseResult.failed("权限更新失败");
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSION_DELETE')")
    public ResponseResult<Void> deletePermission(@PathVariable Long id) {
        boolean success = permissionService.deletePermission(id);
        return success ? ResponseResult.success(null, "权限删除成功") : ResponseResult.failed("权限删除失败");
    }

    /**
     * 更新权限状态
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERMISSION_UPDATE')")
    public ResponseResult<Void> updatePermissionStatus(@PathVariable Long id, @Validated @RequestBody StatusDTO dto) {
        boolean success = permissionService.updatePermissionStatus(id, dto.getStatus());
        return success ? ResponseResult.success(null, "权限状态更新成功") : ResponseResult.failed("权限状态更新失败");
    }

    /**
     * 获取菜单权限树
     */
    @GetMapping("/menu-tree")
    @PreAuthorize("hasAuthority('PERMISSION_QUERY')")
    public ResponseResult<List<PermissionVO>> getMenuPermissions() {
        List<SysPermission> permissions = permissionService.getMenuPermissions();
        List<PermissionVO> voList = permissionConverter.toVOList(permissions);
        return ResponseResult.success(enrichPermissionVOs(voList));
    }

    /**
     * 获取子权限
     */
    @GetMapping("/{parentId}/children")
    @PreAuthorize("hasAuthority('PERMISSION_QUERY')")
    public ResponseResult<List<PermissionVO>> getChildPermissions(@PathVariable Long parentId) {
        List<SysPermission> permissions = permissionService.getChildPermissions(parentId);
        List<PermissionVO> voList = permissionConverter.toVOList(permissions);
        return ResponseResult.success(enrichPermissionVOs(voList));
    }

    /**
     * 获取启用的权限
     */
    @GetMapping("/enabled")
    @PreAuthorize("hasAuthority('PERMISSION_QUERY')")
    public ResponseResult<List<PermissionVO>> getEnabledPermissions() {
        List<SysPermission> permissions = permissionService.getEnabledPermissions();
        List<PermissionVO> voList = permissionConverter.toVOList(permissions);
        return ResponseResult.success(enrichPermissionVOs(voList));
    }

    /**
     * 丰富权限VO，设置父权限名称
     */
    private List<PermissionVO> enrichPermissionVOs(List<PermissionVO> voList) {
        // 1. 提取所有权限ID和对应的VO
        Map<Long, PermissionVO> voMap = voList.stream()
                .collect(Collectors.toMap(PermissionVO::getPermissionId, Function.identity()));

        // 2. 设置父权限名称
        voList.forEach(vo -> {
            if (vo.getParentId() != null && voMap.containsKey(vo.getParentId())) {
                vo.setParentName(voMap.get(vo.getParentId()).getPermName());
            }
        });

        return voList;
    }
}