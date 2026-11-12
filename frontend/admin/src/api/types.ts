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

export interface UserBrief {
  id: number
  username: string
  nickname: string
}

export interface MenuNode {
  id: number
  parentId: number
  type: string
  code: string
  name: string
  path: string | null
  component: string | null
  icon: string | null
  sort: number
  visible: boolean
  children: MenuNode[]
}

export interface LoginResponse {
  user: UserBrief
  superAdmin: boolean
  permissions: string[]
  menus: MenuNode[]
  mustChangePassword: boolean
  token?: string | null
}

export interface MeResponse {
  user: UserBrief
  superAdmin: boolean
  permissions: string[]
  menus: MenuNode[]
  mustChangePassword: boolean
}

export type UserStatus = 'ENABLED' | 'DISABLED'

export interface UserSummary {
  id: number
  username: string
  nickname: string
  status: UserStatus
  lastLoginAt: string | null
  createdAt: string
}
