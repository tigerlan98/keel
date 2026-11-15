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

type ForbiddenHandler = (code: string, message: string) => void

let unauthorizedHandler: UnauthorizedHandler | null = null

let forbiddenHandler: ForbiddenHandler | null = null

/**
 * 401 时由应用入口注册的回调统一清理登录态并跳转。
 */
export function onUnauthorized(handler: UnauthorizedHandler): void {
  unauthorizedHandler = handler
}

/**
 * 403 只提示，不退出登录；由应用入口统一处理提示与改密跳转。
 */
export function onForbidden(handler: ForbiddenHandler): void {
  forbiddenHandler = handler
}

function readCookie(name: string): string | null {
  const prefix = `${name}=`
  const item = document.cookie
    .split(';')
    .map((cookie) => cookie.trim())
    .find((cookie) => cookie.startsWith(prefix))
  if (!item) return null
  try {
    return decodeURIComponent(item.slice(prefix.length)) || null
  } catch {
    return null
  }
}

interface CsrfRequestConfig extends InternalAxiosRequestConfig {
  csrfRetryAttempted?: boolean
  csrfRetryToken?: string
}

function needsCsrf(config: InternalAxiosRequestConfig): boolean {
  const method = config.method?.toUpperCase() ?? 'GET'
  const authorization = config.headers.get('Authorization')
  return (
    ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method) &&
    !(typeof authorization === 'string' && /^Bearer\s/i.test(authorization))
  )
}

let csrfRequest: Promise<string> | null = null

function extractFieldErrors(body?: ApiResponse<unknown>): FieldErrorItem[] {
  const data = body?.data as ValidationErrorData | undefined
  return data?.fieldErrors ?? []
}

const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15_000,
  withCredentials: true,
})

http.interceptors.request.use(async (config: CsrfRequestConfig) => {
  if (needsCsrf(config)) {
    const explicitToken = config.headers.get('X-XSRF-TOKEN')
    const csrfToken =
      config.csrfRetryToken ||
      readCookie('XSRF-TOKEN') ||
      (typeof explicitToken === 'string' ? explicitToken : '') ||
      (await fetchCsrfToken())
    config.headers.set('X-XSRF-TOKEN', csrfToken)
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown> | undefined
    if (body && typeof body.code === 'string' && body.code !== 'OK') {
      return Promise.reject(
        new ApiError(
          body.code,
          body.message || '请求失败',
          response.status,
          extractFieldErrors(body),
        ),
      )
    }
    return response
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status
    const body = error.response?.data
    const config = error.config as CsrfRequestConfig | undefined

    // CsrfFilter 拒绝的请求尚未执行业务，只对该明确错误恢复一次。
    if (
      status === 403 &&
      body?.code === 'AUTH_CSRF_INVALID' &&
      config &&
      needsCsrf(config) &&
      !config.csrfRetryAttempted
    ) {
      config.csrfRetryAttempted = true
      config.csrfRetryToken = await fetchCsrfToken()
      return http.request(config)
    }

    if (status === 401) {
      unauthorizedHandler?.()
    }

    const message = body?.message || (status ? `请求失败（${status}）` : '网络连接失败，请稍后重试')

    if (status === 403) {
      forbiddenHandler?.(body?.code || 'COMMON_FORBIDDEN', message)
    }
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

/**
 * 获取 CSRF token。Web 登录与写请求前调用，服务端会同时写入 XSRF-TOKEN Cookie。
 */
export function fetchCsrfToken(): Promise<string> {
  // 并发写入共用一次获取，避免首次生成的 Cookie 和请求头互相覆盖。
  if (!csrfRequest) {
    csrfRequest = http
      .get<ApiResponse<{ token: string }>>('/admin/auth/csrf')
      .then((response) => {
        const token = response.data.data.token
        if (!token) throw new ApiError('COMMON_REQUEST_FAILED', '获取 CSRF token 失败')
        return token
      })
      .finally(() => {
        csrfRequest = null
      })
  }
  return csrfRequest
}

export default http
