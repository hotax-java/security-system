package com.webapp.security.admin.converter;

import com.webapp.security.admin.controller.user.dto.UserCreateDTO;
import com.webapp.security.admin.controller.user.dto.UserUpdateDTO;
import com.webapp.security.admin.controller.user.vo.UserVO;
import com.webapp.security.core.entity.SysUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

/**
 * 用户数据转换器
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    /**
     * 将实体转换为VO
     */
    @Mapping(target = "statusName", source = "status", qualifiedByName = "statusToName")
    UserVO toVO(SysUser entity);

    /**
     * 将实体列表转换为VO列表
     */
    List<UserVO> toVOList(List<SysUser> entities);

    /**
     * 将创建DTO转换为实体
     */
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "deleted", constant = "0")
    SysUser fromCreateDTO(UserCreateDTO dto);

    /**
     * 使用更新DTO更新实体
     */
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntityFromDTO(UserUpdateDTO dto, @MappingTarget SysUser entity);

    /**
     * 状态转换为名称
     */
    @Named("statusToName")
    default String statusToName(Integer status) {
        return status != null && status == 1 ? "启用" : "禁用";
    }
}