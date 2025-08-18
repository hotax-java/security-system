package com.webapp.security.admin.converter;

import com.webapp.security.admin.controller.permission.dto.PermissionCreateDTO;
import com.webapp.security.admin.controller.permission.dto.PermissionUpdateDTO;
import com.webapp.security.admin.controller.permission.vo.PermissionVO;
import com.webapp.security.core.entity.SysPermission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

/**
 * 权限数据转换器
 */
@Mapper(componentModel = "spring")
public interface PermissionConverter {

    /**
     * 将实体转换为VO
     */
    @Mapping(target = "statusName", source = "status", qualifiedByName = "statusToName")
    @Mapping(target = "permTypeName", source = "permType", qualifiedByName = "permTypeToName")
    @Mapping(target = "parentName", ignore = true) // parentName需要手动设置
    PermissionVO toVO(SysPermission entity);

    /**
     * 将实体列表转换为VO列表
     */
    List<PermissionVO> toVOList(List<SysPermission> entities);

    /**
     * 将创建DTO转换为实体
     */
    @Mapping(target = "permissionId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "deleted", constant = "0")
    @Mapping(target = "children", ignore = true)
    SysPermission fromCreateDTO(PermissionCreateDTO dto);

    /**
     * 使用更新DTO更新实体
     */
    @Mapping(target = "permissionId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "children", ignore = true)
    void updateEntityFromDTO(PermissionUpdateDTO dto, @MappingTarget SysPermission entity);

    /**
     * 状态转换为名称
     */
    @Named("statusToName")
    default String statusToName(Integer status) {
        return status != null && status == 1 ? "启用" : "禁用";
    }

    /**
     * 权限类型转换为名称
     */
    @Named("permTypeToName")
    default String permTypeToName(Integer permType) {
        if (permType == null)
            return "未知";
        switch (permType) {
            case 1:
                return "菜单";
            case 2:
                return "按钮";
            case 3:
                return "接口";
            default:
                return "未知";
        }
    }
}