/**
 * 凭证-资源关系相关类型定义
 */

/**
 * 资源分配请求DTO
 */
export interface ResourceAssignDTO {
  resourceIds: number[];
  operator: string;
} 