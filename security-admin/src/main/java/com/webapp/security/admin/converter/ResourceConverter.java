package com.webapp.security.admin.converter;

import com.webapp.security.admin.controller.sysresource.dto.ResourceDTO;
import com.webapp.security.admin.controller.sysresource.vo.ResourceVO;
import com.webapp.security.core.entity.SysResource;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ResourceConverter {
    ResourceVO toVO(SysResource entity);

    List<ResourceVO> toVOList(List<SysResource> list);

    SysResource fromDTO(ResourceDTO dto);

    void updateEntityFromDTO(ResourceDTO dto, @MappingTarget SysResource entity);
}