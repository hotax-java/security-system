import { businessApi } from './api';

/**
 * 权限管理服务API
 */
export const permissionService = {
  /**
   * 获取权限列表（分页）
   * @param page 页码
   * @param size 每页大小
   * @param keyword 搜索关键词
   */
  getPermissions: async (page: number = 1, size: number = 10, keyword?: string) => {
    const params = { pageNum: page, pageSize: size };
    if (keyword) {
      (params as any).keyword = keyword;
    }
    return await businessApi.post('/api/permissions/paged', params);
  },

  /**
   * 获取所有权限（不分页）
   */
  getAllPermissions: async () => {
    return await businessApi.get('/api/permissions/all');
  },

  /**
   * 获取权限详情
   * @param id 权限ID
   */
  getPermissionById: async (id: number) => {
    return await businessApi.get(`/api/permissions/${id}`);
  },

  /**
   * 创建权限
   * @param permission 权限信息
   */
  createPermission: async (permission: any) => {
    return await businessApi.post('/api/permissions', permission);
  },

  /**
   * 更新权限
   * @param id 权限ID
   * @param permission 权限信息
   */
  updatePermission: async (id: number, permission: any) => {
    return await businessApi.put(`/api/permissions/${id}`, permission);
  },

  /**
   * 删除权限
   * @param id 权限ID
   */
  deletePermission: async (id: number) => {
    return await businessApi.delete(`/api/permissions/${id}`);
  },

  /**
   * 启用/禁用权限
   * @param id 权限ID
   * @param status 状态（1启用，0禁用）
   */
  togglePermissionStatus: async (id: number, status: number) => {
    return await businessApi.patch(`/api/permissions/${id}/status`, { status });
  },

  /**
   * 获取权限树
   */
  getPermissionTree: async () => {
    return await businessApi.get('/api/permissions/tree');
  }
};