package com.webapp.security.admin.converter;

import com.webapp.security.admin.controller.sysclientcredential.vo.CredentialVO;
import com.webapp.security.core.entity.SysClientCredential;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CredentialConverter {
    CredentialVO toVO(SysClientCredential entity);

    List<CredentialVO> toVOList(List<SysClientCredential> list);
}