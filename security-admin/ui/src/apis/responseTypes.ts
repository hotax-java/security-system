/**
 * 定义API响应类型，用于解决TypeScript类型错误
 */

/**
 * 统一响应结果包装类型
 */
export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
}

/**
 * 分页结果类型
 */
export interface PagedResult<T> {
  records: T[];
  total: number;
}

/**
 * 客户端凭证VO
 */
export interface CredentialVO {
  id: number;
  appId: string;
  clientId: string;
  status: number;
  creatorUsername: string;
  remark?: string;
  createTime?: string;
}

/**
 * 创建凭证结果DTO
 */
export interface CredentialCreateResultDTO {
  appId: string;
  appSecret: string;
}

/**
 * API资源VO
 */
export interface ResourceVO {
  resourceId: number;
  resourceCode: string;
  resourceName: string;
  resourcePath: string;
  method: string;
  qpsLimit?: number;
  burstCapacity?: number;
  dailyQuota?: number;
  concurrencyLimit?: number;
  status: number;
  createTime?: string;
  updateTime?: string;
} 