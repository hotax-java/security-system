import businessApi from './api';
import { PagedDTO, PagedResult, ResourceVO, ResourceDTO } from './types';

/**
 * API资源管理服务
 */
export const resourceService = {
  /**
   * 分页查询API资源
   */
  paged: async (pagedDTO: PagedDTO): Promise<PagedResult<ResourceVO>> => {
    return businessApi.post('/api/sys-resources/paged', pagedDTO);
  },

  /**
   * 获取所有API资源
   */
  all: async (): Promise<ResourceVO[]> => {
    return businessApi.get('/api/sys-resources/all');
  },

  /**
   * 创建API资源
   */
  create: async (resource: ResourceDTO): Promise<void> => {
    await businessApi.post('/api/sys-resources', resource);
  },

  /**
   * 更新API资源
   */
  update: async (id: number, resource: ResourceDTO): Promise<void> => {
    await businessApi.put(`/api/sys-resources/${id}`, resource);
  },

  /**
   * 删除API资源
   */
  remove: async (id: number): Promise<void> => {
    await businessApi.delete(`/api/sys-resources/${id}`);
  }
};

export default resourceService;
