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
