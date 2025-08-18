package com.webapp.security.admin.controller.sysresource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.webapp.security.admin.controller.sysresource.dto.ResourceDTO;
import com.webapp.security.admin.controller.sysresource.vo.ResourceVO;
import com.webapp.security.admin.converter.ResourceConverter;
import com.webapp.security.core.entity.SysResource;
import com.webapp.security.core.model.PagedDTO;
import com.webapp.security.core.model.PagedResult;
import com.webapp.security.core.model.ResponseResult;
import com.webapp.security.core.service.SysResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import cn.hutool.core.util.StrUtil;

@RestController
@RequestMapping("/api/sys-resources")
@RequiredArgsConstructor
public class SysResourceController {

    private final SysResourceService resourceService;
    private final ResourceConverter resourceConverter;

    @PostMapping("/paged")
    @PreAuthorize("hasAuthority('OPENAPI_RESOURCE_QUERY')")
    public ResponseResult<PagedResult<ResourceVO>> paged(@RequestBody PagedDTO paged) {
        // 使用Controller层处理分页逻辑，便于扩展
        Page<SysResource> page = new Page<>(paged.getPageNum(), paged.getPageSize());
        String keyword = paged.getKeyword();
        LambdaQueryWrapper<SysResource> qw = new LambdaQueryWrapper<SysResource>()
                .like(StrUtil.isNotBlank(keyword), SysResource::getResourceCode, keyword)
                .or(StrUtil.isNotBlank(keyword), c -> c.like(SysResource::getResourceName, keyword)
                        .or().like(SysResource::getResourcePath, keyword));

        // 调用基础Service方法执行查询
        Page<SysResource> result = resourceService.page(page, qw);

        // 转换为VO并返回
        return ResponseResult.success(
                new PagedResult<>(resourceConverter.toVOList(result.getRecords()), result.getTotal()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('OPENAPI_RESOURCE_QUERY')")
    public ResponseResult<List<ResourceVO>> all() {
        // 调用Service获取所有资源
        List<SysResource> resources = resourceService.list();
        // 转换为VO并返回
        return ResponseResult.success(resourceConverter.toVOList(resources));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('OPENAPI_RESOURCE_CREATE')")
    public ResponseResult<Void> create(@Validated @RequestBody ResourceDTO dto) {
        try {
            // 转换DTO到实体
            SysResource resource = resourceConverter.fromDTO(dto);
            // 调用Service创建资源
            resourceService.createResource(resource);
            return ResponseResult.success(null, "创建成功");
        } catch (Exception e) {
            return ResponseResult.failed("创建失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('OPENAPI_RESOURCE_UPDATE')")
    public ResponseResult<Void> update(@PathVariable("id") Long id, @Validated @RequestBody ResourceDTO dto) {
        try {
            // 转换DTO到实体
            SysResource resource = resourceConverter.fromDTO(dto);
            // 调用Service更新资源
            resourceService.updateResource(id, resource);
            return ResponseResult.success(null, "更新成功");
        } catch (Exception e) {
            return ResponseResult.failed("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('OPENAPI_RESOURCE_DELETE')")
    public ResponseResult<Void> delete(@PathVariable("id") Long id) {
        try {
            // 调用Service删除资源
            resourceService.deleteResource(id);
            return ResponseResult.success(null, "删除成功");
        } catch (Exception e) {
            return ResponseResult.failed("删除失败: " + e.getMessage());
        }
    }
}