import axios, { AxiosError } from 'axios'

import type { ApiResponse, FieldErrorItem, ValidationErrorData } from './types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

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

function extractFieldErrors(body?: ApiResponse<unknown>): FieldErrorItem[] {
  const data = body?.data as ValidationErrorData | undefined
  return data?.fieldErrors ?? []
}

const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15_000,
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
