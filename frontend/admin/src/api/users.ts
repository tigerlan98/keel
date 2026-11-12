import http from './http'
import type { ApiResponse, PageResult, UserStatus, UserSummary } from './types'

export interface UserQueryParams {
  page?: number
  size?: number
  sort?: string
  username?: string
  status?: UserStatus
}

export async function fetchUsers(params: UserQueryParams): Promise<PageResult<UserSummary>> {
  const response = await http.get<ApiResponse<PageResult<UserSummary>>>('/admin/iam/users', {
    params,
  })
  return response.data.data
}
