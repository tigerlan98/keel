import type { components, operations } from './generated'
import http from './http'
import type { ApiResponse, PageResult, RoleDetail, RoleSummary } from './types'

export type RoleQueryParams = NonNullable<operations['listRoles']['parameters']['query']>

export type CreateRolePayload = components['schemas']['CreateRoleRequest']
export type UpdateRolePayload = components['schemas']['UpdateRoleRequest']

export async function fetchRoles(params: RoleQueryParams): Promise<PageResult<RoleSummary>> {
  const response = await http.get<ApiResponse<PageResult<RoleSummary>>>('/admin/iam/roles', {
    params,
  })
  return response.data.data
}

export async function fetchRole(id: number): Promise<RoleDetail> {
  const response = await http.get<ApiResponse<RoleDetail>>(`/admin/iam/roles/${id}`)
  return response.data.data
}

export async function createRole(payload: CreateRolePayload): Promise<RoleDetail> {
  const response = await http.post<ApiResponse<RoleDetail>>('/admin/iam/roles', payload)
  return response.data.data
}

export async function updateRole(id: number, payload: UpdateRolePayload): Promise<RoleDetail> {
  const response = await http.put<ApiResponse<RoleDetail>>(`/admin/iam/roles/${id}`, payload)
  return response.data.data
}

export async function deleteRole(id: number): Promise<void> {
  await http.delete(`/admin/iam/roles/${id}`)
}

export async function fetchRoleResourceIds(id: number): Promise<number[]> {
  const response = await http.get<ApiResponse<{ resourceIds: number[] }>>(
    `/admin/iam/roles/${id}/resources`,
  )
  return response.data.data.resourceIds
}

export async function replaceRoleResources(id: number, resourceIds: number[]): Promise<void> {
  await http.put(`/admin/iam/roles/${id}/resources`, { resourceIds })
}
