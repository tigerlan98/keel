import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'

import type { ApiResponse, FieldErrorItem, ValidationErrorData } from './types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

/**
 * 归一化后的接口错误，页面只需读取 code / message / fieldErrors。
 */
export class ApiError extends Error {
  readonly code: string
  readonly status?: number
  readonly fieldErrors: FieldErrorItem[]

  constructor(code: string, message: string, status?: number, fieldErrors: FieldErrorItem[] = []) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

type UnauthorizedHandler = () => void

let unauthorizedHandler: UnauthorizedHandler | null = null

/**
 * 401 时由应用入口注册的回调统一清理登录态并跳转。
 */
export function onUnauthorized(handler: UnauthorizedHandler): void {
  unauthorizedHandler = handler
}

function readCookie(name: string): string | null {
  const prefix = `${name}=`
  const item = document.cookie.split('; ').find((cookie) => cookie.startsWith(prefix))
  return item ? decodeURIComponent(item.slice(prefix.length)) : null
}

function extractFieldErrors(body?: ApiResponse<unknown>): FieldErrorItem[] {
  const data = body?.data as ValidationErrorData | undefined
  return data?.fieldErrors ?? []
}

const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15_000,
  withCredentials: true,
})

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const csrfToken = readCookie('XSRF-TOKEN')
  if (csrfToken) {
    config.headers.set('X-XSRF-TOKEN', csrfToken)
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown> | undefined
    if (body && typeof body.code === 'string' && body.code !== 'OK') {
      return Promise.reject(
        new ApiError(body.code, body.message || '请求失败', response.status, extractFieldErrors(body)),
      )
    }
    return response
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status
    const body = error.response?.data

    if (status === 401) {
      unauthorizedHandler?.()
    }

    const message = body?.message || (status ? `请求失败（${status}）` : '网络连接失败，请稍后重试')
    return Promise.reject(
      new ApiError(
        body?.code || 'COMMON_REQUEST_FAILED',
        message,
        status,
        extractFieldErrors(body),
      ),
    )
  },
)

export default http
