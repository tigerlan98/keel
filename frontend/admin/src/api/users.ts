import type { components, operations } from './generated'
import http from './http'
import type {
  ApiResponse,
  PageResult,
  RoleBrief,
  UserDetail,
  UserStatus,
  UserSummary,
} from './types'

export type UserQueryParams = NonNullable<operations['listUsers']['parameters']['query']>

export type CreateUserPayload = components['schemas']['CreateUserRequest']
export type UpdateUserPayload = components['schemas']['UpdateUserRequest']
export type ResetPasswordPayload = components['schemas']['ResetPasswordRequest']

export async function fetchUsers(params: UserQueryParams): Promise<PageResult<UserSummary>> {
  const response = await http.get<ApiResponse<PageResult<UserSummary>>>('/admin/iam/users', {
    params,
  })
  return response.data.data
}

export async function fetchUser(id: number): Promise<UserDetail> {
  const response = await http.get<ApiResponse<UserDetail>>(`/admin/iam/users/${id}`)
  return response.data.data
}

export async function createUser(payload: CreateUserPayload): Promise<UserDetail> {
  const response = await http.post<ApiResponse<UserDetail>>('/admin/iam/users', payload)
  return response.data.data
}

export async function updateUser(id: number, payload: UpdateUserPayload): Promise<UserDetail> {
  const response = await http.put<ApiResponse<UserDetail>>(`/admin/iam/users/${id}`, payload)
  return response.data.data
}

export async function updateUserStatus(
  id: number,
  status: UserStatus,
  version: number,
): Promise<UserDetail> {
  const response = await http.patch<ApiResponse<UserDetail>>(`/admin/iam/users/${id}/status`, {
    status,
    version,
  })
  return response.data.data
}

export async function resetUserPassword(id: number, payload: ResetPasswordPayload): Promise<void> {
  await http.post(`/admin/iam/users/${id}/reset-password`, payload)
}

export async function fetchUserRoles(id: number): Promise<RoleBrief[]> {
  const response = await http.get<ApiResponse<{ roles: RoleBrief[] }>>(`/admin/iam/users/${id}/roles`)
  return response.data.data.roles
}

export async function assignUserRoles(id: number, roleIds: number[]): Promise<void> {
  await http.put(`/admin/iam/users/${id}/roles`, { roleIds })
}

export async function forceLogout(id: number): Promise<void> {
  await http.delete(`/admin/iam/users/${id}/sessions`)
}

export async function forceLogoutDevice(id: number, device: string): Promise<void> {
  await http.delete(`/admin/iam/users/${id}/sessions/${device}`)
}
