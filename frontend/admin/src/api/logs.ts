import type { operations } from './generated'
import http from './http'
import type {
  ApiResponse,
  LoginLogDetail,
  LoginLogSummary,
  OperationLogDetail,
  OperationLogSummary,
  PageResult,
} from './types'

export type OperationLogQueryParams = NonNullable<
  operations['listOperationLogs']['parameters']['query']
>

export type LoginLogQueryParams = NonNullable<operations['listLoginLogs']['parameters']['query']>

export async function fetchOperationLogs(
  params: OperationLogQueryParams,
): Promise<PageResult<OperationLogSummary>> {
  const response = await http.get<ApiResponse<PageResult<OperationLogSummary>>>(
    '/admin/audit/operation-logs',
    { params },
  )
  return response.data.data
}

export async function fetchOperationLog(id: number): Promise<OperationLogDetail> {
  const response = await http.get<ApiResponse<OperationLogDetail>>(
    `/admin/audit/operation-logs/${id}`,
  )
  return response.data.data
}

export async function fetchLoginLogs(
  params: LoginLogQueryParams,
): Promise<PageResult<LoginLogSummary>> {
  const response = await http.get<ApiResponse<PageResult<LoginLogSummary>>>(
    '/admin/audit/login-logs',
    { params },
  )
  return response.data.data
}

export async function fetchLoginLog(id: number): Promise<LoginLogDetail> {
  const response = await http.get<ApiResponse<LoginLogDetail>>(`/admin/audit/login-logs/${id}`)
  return response.data.data
}
