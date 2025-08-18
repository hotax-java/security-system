/**
 * 基础API类型定义
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
 * 分页请求DTO
 */
export interface PagedDTO {
  pageNum: number;
  pageSize: number;
  keyword?: string;
}

/**
 * 分页结果类型
 */
export interface PagedResult<T> {
  records: T[];
  total: number;
} 