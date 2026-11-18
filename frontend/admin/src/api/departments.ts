import type { components } from './generated'
import type { ApiResponse } from './types'
import http from './http'
export type DepartmentNode = components['schemas']['DepartmentNode']
export type DepartmentOption = components['schemas']['DepartmentOption']
export type DepartmentDetail = components['schemas']['DepartmentDetail']
export type CreateDepartmentPayload = components['schemas']['CreateDepartmentRequest']
export type UpdateDepartmentPayload = components['schemas']['UpdateDepartmentRequest']
export type DepartmentStatus = DepartmentNode['status']
export async function fetchDepartmentTree(): Promise<DepartmentNode[]> {
  return (await http.get<ApiResponse<DepartmentNode[]>>('/admin/iam/departments/tree')).data.data
}
export async function fetchDepartmentOptions(): Promise<DepartmentOption[]> {
  return (await http.get<ApiResponse<DepartmentOption[]>>('/admin/iam/departments/options')).data
    .data
}
export async function fetchDepartment(id: number): Promise<DepartmentDetail> {
  return (await http.get<ApiResponse<DepartmentDetail>>(`/admin/iam/departments/${id}`)).data.data
}
export async function createDepartment(
  payload: CreateDepartmentPayload,
): Promise<DepartmentDetail> {
  return (await http.post<ApiResponse<DepartmentDetail>>('/admin/iam/departments', payload)).data
    .data
}
export async function updateDepartment(
  id: number,
  payload: UpdateDepartmentPayload,
): Promise<DepartmentDetail> {
  return (await http.put<ApiResponse<DepartmentDetail>>(`/admin/iam/departments/${id}`, payload))
    .data.data
}
export async function changeDepartmentStatus(
  id: number,
  status: DepartmentStatus,
  version: number,
): Promise<DepartmentDetail> {
  return (
    await http.patch<ApiResponse<DepartmentDetail>>(`/admin/iam/departments/${id}/status`, {
      status,
      version,
    })
  ).data.data
}
export async function deleteDepartment(id: number, version: number): Promise<void> {
  await http.delete(`/admin/iam/departments/${id}`, { params: { version } })
}
