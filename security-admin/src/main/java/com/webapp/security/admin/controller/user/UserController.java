package com.webapp.security.admin.controller.user;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.webapp.security.admin.controller.user.dto.*;
import com.webapp.security.admin.controller.user.vo.UserVO;
import com.webapp.security.admin.converter.UserConverter;
import com.webapp.security.core.entity.SysUser;
import com.webapp.security.core.model.PagedDTO;
import com.webapp.security.core.model.PagedResult;
import com.webapp.security.core.model.ResponseResult;
import com.webapp.security.core.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService userService;
    private final UserConverter userConverter;

    @PostMapping("/paged")
    @PreAuthorize("hasAuthority('USER_QUERY')")
    public ResponseResult<PagedResult<UserVO>> findUserPaged(@RequestBody PagedDTO paged) {
        Page<SysUser> page = new Page<>(paged.getPageNum(), paged.getPageSize());
        String keyword = paged.getKeyword();
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<SysUser>()
                .like(StrUtil.isNotBlank(keyword), SysUser::getUsername, keyword)
                .or(StrUtil.isNotBlank(keyword), condition -> condition
                        .like(SysUser::getRealName, keyword));

        Page<SysUser> pageResult = userService.page(page, queryWrapper);
        List<UserVO> voList = userConverter.toVOList(pageResult.getRecords());
        return ResponseResult.success(new PagedResult<>(voList, pageResult.getTotal()));
    }

    /**
     * 获取所有用户
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('USER_QUERY')")
    public ResponseResult<List<UserVO>> getAllUsers() {
        List<SysUser> users = userService.list();
        return ResponseResult.success(userConverter.toVOList(users));
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_QUERY')")
    public ResponseResult<UserVO> getUserById(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) {
            return ResponseResult.failed("用户不存在");
        }
        return ResponseResult.success(userConverter.toVO(user));
    }

    /**
     * 创建用户
     */
    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseResult<Void> createUser(@Validated @RequestBody UserCreateDTO dto) {
        SysUser user = userConverter.fromCreateDTO(dto);
        boolean success = userService.createUser(user);
        return success ? ResponseResult.success(null, "用户创建成功") : ResponseResult.failed("用户创建失败");
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseResult<Void> updateUser(@PathVariable Long id, @Validated @RequestBody UserUpdateDTO dto) {
        SysUser user = userService.getById(id);
        if (user == null) {
            return ResponseResult.failed("用户不存在");
        }

        userConverter.updateEntityFromDTO(dto, user);
        boolean success = userService.updateUser(user);
        return success ? ResponseResult.success(null, "用户更新成功") : ResponseResult.failed("用户更新失败");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseResult<Void> deleteUser(@PathVariable Long id) {
        boolean success = userService.deleteUser(id);
        return success ? ResponseResult.success(null, "用户删除成功") : ResponseResult.failed("用户删除失败");
    }

    /**
     * 更新用户状态
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseResult<Void> updateUserStatus(@PathVariable Long id, @Validated @RequestBody StatusDTO dto) {
        boolean success = userService.updateUserStatus(id, dto.getStatus());
        return success ? ResponseResult.success(null, "用户状态更新成功") : ResponseResult.failed("用户状态更新失败");
    }

    /**
     * 分配用户角色
     */
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseResult<Void> assignRoles(@PathVariable Long id, @Validated @RequestBody RoleIdsDTO dto) {
        boolean success = userService.assignRoles(id, dto.getRoleIds());
        return success ? ResponseResult.success(null, "角色分配成功") : ResponseResult.failed("角色分配失败");
    }

    /**
     * 获取用户权限
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('USER_QUERY')")
    public ResponseResult<List<String>> getUserPermissions(@PathVariable Long id) {
        List<String> permissions = userService.getUserPermissions(id);
        return ResponseResult.success(permissions);
    }

    /**
     * 获取用户角色
     */
    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USER_QUERY')")
    public ResponseResult<List<String>> getUserRoles(@PathVariable Long id) {
        List<String> roles = userService.getUserRoles(id);
        return ResponseResult.success(roles);
    }
}