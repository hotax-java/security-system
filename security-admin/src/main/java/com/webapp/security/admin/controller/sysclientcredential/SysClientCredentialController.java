package com.webapp.security.admin.controller.sysclientcredential;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.webapp.security.admin.controller.sysclientcredential.dto.CredentialCreateDTO;
import com.webapp.security.admin.controller.sysclientcredential.dto.CredentialCreateResultDTO;
import com.webapp.security.admin.controller.sysclientcredential.vo.CredentialVO;
import com.webapp.security.admin.converter.CredentialConverter;
import com.webapp.security.core.entity.SysClientCredential;
import com.webapp.security.core.model.PagedDTO;
import com.webapp.security.core.model.PagedResult;
import com.webapp.security.core.model.ResponseResult;
import com.webapp.security.core.service.SysClientCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import cn.hutool.core.util.StrUtil;

@RestController
@RequestMapping("/api/sys-client-credentials")
@RequiredArgsConstructor
public class SysClientCredentialController {

    private final SysClientCredentialService credentialService;
    private final CredentialConverter credentialConverter;

    /**
     * 分页查询客户端凭证
     */
    @PostMapping("/paged")
    @PreAuthorize("hasAuthority('OPENAPI_CREDENTIAL_QUERY')")
    public ResponseResult<PagedResult<CredentialVO>> paged(@RequestBody PagedDTO paged) {
        // 使用Controller层处理分页逻辑，便于扩展
        Page<SysClientCredential> page = new Page<>(paged.getPageNum(), paged.getPageSize());
        String keyword = paged.getKeyword();
        LambdaQueryWrapper<SysClientCredential> qw = new LambdaQueryWrapper<SysClientCredential>()
                .like(StrUtil.isNotBlank(keyword), SysClientCredential::getAppId, keyword)
                .or().like(StrUtil.isNotBlank(keyword), SysClientCredential::getRemark, keyword);

        // 调用基础Service方法执行查询
        Page<SysClientCredential> result = credentialService.page(page, qw);

        // 转换为VO并返回
        return ResponseResult.success(
                new PagedResult<>(credentialConverter.toVOList(result.getRecords()), result.getTotal()));
    }

    /**
     * 获取所有客户端凭证
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('OPENAPI_CREDENTIAL_QUERY')")
    public ResponseResult<List<CredentialVO>> all() {
        // 调用Service获取所有凭证
        List<SysClientCredential> credentials = credentialService.list();
        // 转换为VO并返回
        return ResponseResult.success(credentialConverter.toVOList(credentials));
    }

    /**
     * 生成临时的凭证信息，不保存到数据库
     */
    @GetMapping("/generate")
    @PreAuthorize("hasAuthority('OPENAPI_CREDENTIAL_CREATE')")
    public ResponseResult<CredentialCreateResultDTO> generateCredential() {
        // 调用Service生成临时凭证
        SysClientCredential credential = credentialService.generateCredential();
        // 转换为DTO并返回
        return ResponseResult.success(
                new CredentialCreateResultDTO(credential.getAppId(), credential.getPlainSecret()));
    }

    /**
     * 保存预先生成的凭证信息
     */
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('OPENAPI_CREDENTIAL_CREATE')")
    public ResponseResult<CredentialCreateResultDTO> saveCredential(@Validated @RequestBody CredentialCreateDTO req) {
        try {
            // 调用Service保存凭证
            SysClientCredential credential = credentialService.saveCredential(
                    req.getAppId(), req.getAppSecret(), req.getRemark());
            // 转换为DTO并返回
            return ResponseResult.success(
                    new CredentialCreateResultDTO(credential.getAppId(), credential.getPlainSecret()));
        } catch (Exception e) {
            return ResponseResult.failed(e.getMessage());
        }
    }

    /**
     * 创建客户端凭证（旧方法，保留兼容）
     */
    @PostMapping
    @PreAuthorize("hasAuthority('OPENAPI_CREDENTIAL_CREATE')")
    public ResponseResult<CredentialCreateResultDTO> create(@Validated @RequestBody CredentialCreateDTO req) {
        try {
            // 调用Service创建凭证
            SysClientCredential entity = credentialService.createCredential(req.getRemark());
            // 转换为DTO并返回
            return ResponseResult.success(
                    new CredentialCreateResultDTO(entity.getAppId(), entity.getPlainSecret()));
        } catch (Exception e) {
            return ResponseResult.failed(e.getMessage());
        }
    }

    /**
     * 更新凭证状态
     */
    @PatchMapping("/{appId}/status")
    @PreAuthorize("hasAuthority('OPENAPI_CREDENTIAL_UPDATE')")
    public ResponseResult<Void> updateStatus(@PathVariable String appId, @RequestParam Integer status) {
        try {
            // 调用Service更新状态
            credentialService.updateStatus(appId, status);
            return ResponseResult.success(null, "更新成功");
        } catch (RuntimeException e) {
            return ResponseResult.failed(e.getMessage());
        } catch (Exception e) {
            return ResponseResult.failed("更新失败: " + e.getMessage());
        }
    }
}