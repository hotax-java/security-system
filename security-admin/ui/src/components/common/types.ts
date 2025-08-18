/**
 * 分页请求DTO
 */
export interface PagedDTO {
  pageNum: number;
  pageSize: number;
  keyword?: string;
}

/**
 * 分页结果
 */
export interface PagedResult<T> {
  records: T[];
  total: number;
}

/**
 * 统一响应结果
 */
export interface ResponseResult<T> {
  code: string;
  message: string;
  data: T;
} 