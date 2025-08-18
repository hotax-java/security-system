package com.webapp.security.admin.converter;

import com.webapp.security.admin.controller.role.dto.RoleCreateDTO;
import com.webapp.security.admin.controller.role.dto.RoleUpdateDTO;
import com.webapp.security.admin.controller.role.vo.RoleVO;
import com.webapp.security.core.entity.SysRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

/**
 * 角色数据转换器
 */
@Mapper(componentModel = "spring")
public interface RoleConverter {

    /**
     * 将实体转换为VO
     */
    @Mapping(target = "statusName", source = "status", qualifiedByName = "statusToName")
    RoleVO toVO(SysRole entity);

    /**
     * 将实体列表转换为VO列表
     */
    List<RoleVO> toVOList(List<SysRole> entities);

    /**
     * 将创建DTO转换为实体
     */
    @Mapping(target = "roleId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "deleted", constant = "0")
    SysRole fromCreateDTO(RoleCreateDTO dto);

    /**
     * 使用更新DTO更新实体
     */
    @Mapping(target = "roleId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntityFromDTO(RoleUpdateDTO dto, @MappingTarget SysRole entity);

    /**
     * 状态转换为名称
     */
    @Named("statusToName")
    default String statusToName(Integer status) {
        return status != null && status == 1 ? "启用" : "禁用";
    }
}