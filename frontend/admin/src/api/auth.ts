import http, { fetchCsrfToken } from './http'
import type { ApiResponse, LoginResponse, MeResponse } from './types'

export async function login(username: string, password: string): Promise<LoginResponse> {
  const csrfToken = await fetchCsrfToken()
  const response = await http.post<ApiResponse<LoginResponse>>(
    '/admin/auth/login',
    { username, password, device: 'WEB' },
    { headers: { 'X-XSRF-TOKEN': csrfToken } },
  )
  return response.data.data
}

export async function fetchMe(): Promise<MeResponse> {
  const response = await http.get<ApiResponse<MeResponse>>('/admin/auth/me')
  return response.data.data
}

export async function logout(): Promise<void> {
  const csrfToken = await fetchCsrfToken()
  await http.post('/admin/auth/logout', null, {
    headers: { 'X-XSRF-TOKEN': csrfToken },
  })
}
