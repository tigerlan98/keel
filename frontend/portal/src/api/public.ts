import http from './http'
import type { ApiResponse, PublicSiteConfig } from './types'

/**
 * 门户公开配置。匿名接口，不携带认证信息。
 */
export async function fetchPublicSiteConfig(): Promise<PublicSiteConfig> {
  const response = await http.get<ApiResponse<PublicSiteConfig>>('/public/system/config')
  return response.data.data
}
