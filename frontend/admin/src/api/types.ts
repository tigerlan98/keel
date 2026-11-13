import type { components } from './generated'

type Schemas = components['schemas']

/**
 * 统一响应与分页包装为泛型契约；OpenAPI 无法表达泛型，故保留手写结构。
 * 具体 DTO 字段一律来自 OpenAPI 快照（`generated.ts`），禁止手写。
 */
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

export interface FieldErrorItem {
  field: string
  message: string
}

export interface ValidationErrorData {
  fieldErrors?: FieldErrorItem[]
}

export type UserBrief = Schemas['UserBrief']
export type MenuNode = Schemas['MenuNode']
export type LoginResponse = Schemas['LoginResponse']
export type MeResponse = Schemas['MeResponse']
export type UserStatus = NonNullable<Schemas['UserSummary']['status']>
export type UserSummary = Schemas['UserSummary']
export type UserDetail = Schemas['UserDetail']

export type RoleStatus = NonNullable<Schemas['RoleSummary']['status']>
export type RoleSummary = Schemas['RoleSummary']
export type RoleDetail = Schemas['RoleDetail']
export type RoleBrief = Schemas['RoleBrief']

export type ResourceType = NonNullable<Schemas['ResourceNode']['type']>
export type ResourceStatus = NonNullable<Schemas['ResourceNode']['status']>
export type ResourceNode = Schemas['ResourceNode']
export type ResourceDetail = Schemas['ResourceDetail']

export type ConfigSummary = Schemas['ConfigSummary']
export type ConfigDetail = Schemas['ConfigDetail']

export type LoginAction = NonNullable<Schemas['LoginLogSummary']['action']>
export type OperationLogSummary = Schemas['OperationLogSummary']
export type OperationLogDetail = Schemas['OperationLogDetail']
export type LoginLogSummary = Schemas['LoginLogSummary']
export type LoginLogDetail = Schemas['LoginLogDetail']
