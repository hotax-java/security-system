package com.webapp.security.admin.converter;

import com.webapp.security.admin.controller.sysresource.dto.ResourceDTO;
import com.webapp.security.admin.controller.sysresource.vo.ResourceVO;
import com.webapp.security.core.entity.SysResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ResourceConverter {
    ResourceVO toVO(SysResource entity);

    List<ResourceVO> toVOList(List<SysResource> list);

    @Mapping(target = "resourceId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    SysResource fromDTO(ResourceDTO dto);

    @Mapping(target = "resourceId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    void updateEntityFromDTO(ResourceDTO dto, @MappingTarget SysResource entity);
}