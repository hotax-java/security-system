import { businessApi } from './api';

/**
 * 用户管理服务API
 */
export const userService = {
  /**
   * 获取用户列表（分页）
   * @param page 页码
   * @param size 每页大小
   * @param keyword 搜索关键词
   */
  getUsers: async (page: number = 1, size: number = 10, keyword?: string) => {
    const params = { page, size };
    if (keyword) {
      (params as any).keyword = keyword;
    }
    return await businessApi.post('/api/users/paged', { params });
  },

  /**
   * 获取所有用户（不分页）
   */
  getAllUsers: async () => {
    return await businessApi.get('/api/users/all');
  },

  /**
   * 获取用户详情
   * @param id 用户ID
   */
  getUserById: async (id: number) => {
    return await businessApi.get(`/api/users/${id}`);
  },

  /**
   * 创建用户
   * @param user 用户信息
   */
  createUser: async (user: any) => {
    return await businessApi.post('/api/users', user);
  },

  /**
   * 更新用户
   * @param id 用户ID
   * @param user 用户信息
   */
  updateUser: async (id: number, user: any) => {
    return await businessApi.put(`/api/users/${id}`, user);
  },

  /**
   * 删除用户
   * @param id 用户ID
   */
  deleteUser: async (id: number) => {
    return await businessApi.delete(`/api/users/${id}`);
  },

  /**
   * 启用/禁用用户
   * @param id 用户ID
   * @param status 状态（1启用，0禁用）
   */
  toggleUserStatus: async (id: number, status: number) => {
    return await businessApi.patch(`/api/users/${id}/status`, { status });
  },

  /**
   * 获取用户角色
   * @param userId 用户ID
   */
  getUserRoles: async (userId: number) => {
    return await businessApi.get(`/api/users/${userId}/roles`);
  },

  /**
   * 分配用户角色
   * @param userId 用户ID
   * @param roleIds 角色ID列表
   */
  assignUserRoles: async (userId: number, roleIds: number[]) => {
    return await businessApi.post(`/api/users/${userId}/roles`, { roleIds });
  }
};