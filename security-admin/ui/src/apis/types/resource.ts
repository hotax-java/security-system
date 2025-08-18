/**
 * API资源相关类型定义
 */

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

/**
 * API资源创建/更新DTO
 */
export interface ResourceDTO {
  resourceCode: string;
  resourceName: string;
  resourcePath: string;
  method: string;
  qpsLimit?: number;
  burstCapacity?: number;
  dailyQuota?: number;
  concurrencyLimit?: number;
  status: number;
} 