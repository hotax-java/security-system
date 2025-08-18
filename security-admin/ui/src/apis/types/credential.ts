/**
 * 客户端凭证相关类型定义
 */

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
 * 创建凭证请求DTO
 */
export interface CredentialCreateDTO {
  remark: string;
}

/**
 * 创建凭证结果DTO
 */
export interface CredentialCreateResultDTO {
  appId?: string;
  appSecret?: string;
}

/**
 * 保存凭证请求DTO
 */
export interface CredentialSaveDTO {
  appId: string;
  appSecret: string;
  remark: string;
}

/**
 * 更新凭证状态请求
 */
export interface CredentialStatusUpdateDTO {
  status: number;
  operator: string;
} 