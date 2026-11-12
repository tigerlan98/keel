import type { components } from './generated'
import http from './http'
import type { ApiResponse, ResourceDetail, ResourceNode, ResourceStatus, ResourceType } from './types'

export type UpdateResourcePayload = components['schemas']['UpdateResourceRequest']

export interface ResourceTreeQuery {
  status?: ResourceStatus
  type?: ResourceType
}

export async function fetchResourceTree(params?: ResourceTreeQuery): Promise<ResourceNode[]> {
  const response = await http.get<ApiResponse<ResourceNode[]>>('/admin/iam/resources/tree', {
    params,
  })
  return response.data.data
}

export async function fetchResource(id: number): Promise<ResourceDetail> {
  const response = await http.get<ApiResponse<ResourceDetail>>(`/admin/iam/resources/${id}`)
  return response.data.data
}

export async function updateResource(
  id: number,
  payload: UpdateResourcePayload,
): Promise<ResourceDetail> {
  const response = await http.put<ApiResponse<ResourceDetail>>(
    `/admin/iam/resources/${id}`,
    payload,
  )
  return response.data.data
}

export async function updateResourceStatus(
  id: number,
  status: ResourceStatus,
  version: number,
): Promise<ResourceDetail> {
  const response = await http.patch<ApiResponse<ResourceDetail>>(
    `/admin/iam/resources/${id}/status`,
    { status, version },
  )
  return response.data.data
}
