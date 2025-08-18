import apiClient from './api';
import { PagedDTO, PagedResult, CredentialVO, CredentialCreateResultDTO, CredentialSaveDTO } from './types';

/**
 * 客户端凭证管理API服务
 * 
 * 需要后端提供的新API:
 * 
 * 1. /api/sys-client-credentials/generate - GET
 *    功能：生成临时的AppID和AppSecret，但不保存到数据库
 *    返回：{ appId: string, appSecret: string }
 * 
 * 2. /api/sys-client-credentials/save - POST
 *    功能：保存已生成的凭证信息
 *    参数：{ appId: string, appSecret: string, remark: string }
 *    返回：保存后的凭证信息
 */
export const credentialService = {
  /**
   * 生成临时凭证信息（不保存）
   */
  generateCredential: async (): Promise<CredentialCreateResultDTO> => {
    return apiClient.get('/api/sys-client-credentials/generate');
  },
  
  /**
   * 保存已生成的凭证信息
   */
  saveCredential: async (data: CredentialSaveDTO): Promise<any> => {
    return apiClient.post('/api/sys-client-credentials/save', data);
  },
  /**
   * 分页查询客户端凭证
   */
  paged: async (pagedDTO: PagedDTO): Promise<PagedResult<CredentialVO>> => {
    return apiClient.post('/api/sys-client-credentials/paged', pagedDTO);
  },

  /**
   * 获取所有客户端凭证
   */
  all: async (): Promise<CredentialVO[]> => {
    return apiClient.get('/api/sys-client-credentials/all');
  },

  /**
   * 创建客户端凭证
   */
  create: async (remark: string): Promise<CredentialCreateResultDTO> => {
    return apiClient.post('/api/sys-client-credentials', {
      remark
    });
  },



  /**
   * 更新客户端凭证状态
   */
  updateStatus: async (appId: string, status: number): Promise<void> => {
    await apiClient.patch(`/api/sys-client-credentials/${appId}/status?status=${status}`);
  },
  
  /**
   * 获取客户端凭证已授权的资源ID列表
   */
  getResourceIds: async (credentialId: number): Promise<number[]> => {
    return apiClient.get(`/api/sys-credential-resource-rel/${credentialId}/resource-ids`);
  },

  /**
   * 为客户端凭证分配API资源权限
   */
  assignResources: async (credentialId: number, resourceIds: number[]): Promise<void> => {
    await apiClient.post(`/api/sys-credential-resource-rel/${credentialId}/assign`, {
      resourceIds
    });
  }
};

export default credentialService; 