export interface FieldErrorItem {
  field: string
  message: string
}

export interface ValidationErrorData {
  fieldErrors?: FieldErrorItem[]
}

export interface ApiResponse<T> {
  code: string
  message: string
  data: T
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

/** 门户公开站点配置，仅包含系统名称与 Logo 地址。 */
export interface PublicSiteConfig {
  systemName: string
  logoUrl?: string | null
}
