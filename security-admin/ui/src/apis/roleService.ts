import { businessApi } from './api';

/**
 * 角色管理服务API
 */
export const roleService = {
  /**
   * 获取角色列表（分页）
   * @param page 页码
   * @param size 每页大小
   * @param keyword 搜索关键词
   */
  getRoles: async (page: number = 1, size: number = 10, keyword?: string) => {
    const params = { page, size };
    if (keyword) {
      (params as any).keyword = keyword;
    }
    return await businessApi.post('/api/roles/paged', { params });
  },

  /**
   * 获取所有角色（不分页）
   */
  getAllRoles: async () => {
    return await businessApi.get('/api/roles/all');
  },

  /**
   * 获取角色详情
   * @param id 角色ID
   */
  getRoleById: async (id: number) => {
    return await businessApi.get(`/api/roles/${id}`);
  },

  /**
   * 创建角色
   * @param role 角色信息
   */
  createRole: async (role: any) => {
    return await businessApi.post('/api/roles', role);
  },

  /**
   * 更新角色
   * @param id 角色ID
   * @param role 角色信息
   */
  updateRole: async (id: number, role: any) => {
    return await businessApi.put(`/api/roles/${id}`, role);
  },

  /**
   * 删除角色
   * @param id 角色ID
   */
  deleteRole: async (id: number) => {
    return await businessApi.delete(`/api/roles/${id}`);
  },

  /**
   * 启用/禁用角色
   * @param id 角色ID
   * @param status 状态（1启用，0禁用）
   */
  toggleRoleStatus: async (id: number, status: number) => {
    return await businessApi.patch(`/api/roles/${id}/status`, { status });
  },

  /**
   * 获取角色权限
   * @param roleId 角色ID
   */
  getRolePermissions: async (roleId: number) => {
    return await businessApi.get(`/api/roles/${roleId}/permissions`);
  },

  /**
   * 分配角色权限
   * @param roleId 角色ID
   * @param permissionIds 权限ID列表
   */
  assignRolePermissions: async (roleId: number, permissionIds: number[]) => {
    return await businessApi.post(`/api/roles/${roleId}/permissions`, { permissionIds });
  },

  /**
   * 检查角色是否被用户使用
   * @param roleId 角色ID
   */
  checkRoleUsage: async (roleId: number) => {
    return await businessApi.get(`/api/roles/${roleId}/usage`);
  }
};