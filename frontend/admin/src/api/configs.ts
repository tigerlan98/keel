import type { components, operations } from './generated'
import http from './http'
import type { ApiResponse, ConfigDetail, ConfigSummary, PageResult } from './types'

export type ConfigQueryParams = NonNullable<operations['listConfigs']['parameters']['query']>

export type UpdateConfigPayload = components['schemas']['UpdateConfigRequest']

export async function fetchConfigs(params: ConfigQueryParams): Promise<PageResult<ConfigSummary>> {
  const response = await http.get<ApiResponse<PageResult<ConfigSummary>>>('/admin/system/configs', {
    params,
  })
  return response.data.data
}

export async function fetchConfig(id: number): Promise<ConfigDetail> {
  const response = await http.get<ApiResponse<ConfigDetail>>(`/admin/system/configs/${id}`)
  return response.data.data
}

export async function updateConfig(id: number, payload: UpdateConfigPayload): Promise<ConfigDetail> {
  const response = await http.put<ApiResponse<ConfigDetail>>(
    `/admin/system/configs/${id}`,
    payload,
  )
  return response.data.data
}
