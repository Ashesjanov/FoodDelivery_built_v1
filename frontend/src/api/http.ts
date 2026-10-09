import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios'
import type { ApiResponse } from '@/types/domain'

const TOKEN_STORAGE_KEY = 'food-delivery-token'
let redirectingToLogin = false

export class ApiError extends Error {
  readonly status: number
  readonly code: number
  readonly details?: unknown

  constructor(message: string, status = 0, code = -1, details?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.details = details
  }
}

export function setToken(token: string | null): void {
  if (typeof window === 'undefined') return
  if (token) {
    window.localStorage.setItem(TOKEN_STORAGE_KEY, token)
  } else {
    window.localStorage.removeItem(TOKEN_STORAGE_KEY)
  }
}

export function getToken(): string | null {
  if (typeof window === 'undefined') return null
  return window.localStorage.getItem(TOKEN_STORAGE_KEY)
}

function redirectToLogin(): void {
  if (typeof window === 'undefined' || redirectingToLogin) return
  const pathname = window.location.pathname
  const mobile = pathname === '/m' || pathname.startsWith('/m/')
  if (['/login', '/register', '/m/login', '/m/register'].includes(pathname)) return
  redirectingToLogin = true
  const redirect = encodeURIComponent(`${pathname}${window.location.search}`)
  window.location.assign(`${mobile ? '/m/login' : '/login'}?redirect=${redirect}`)
}

export const api: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: Number(import.meta.env.VITE_API_TIMEOUT_MS ?? 5_000),
  headers: { Accept: 'application/json' },
})

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (response) => {
    const envelope = response.data as ApiResponse<unknown> | undefined
    if (envelope && typeof envelope.code === 'number' && envelope.code !== 0) {
      return Promise.reject(
        new ApiError(envelope.message || '请求失败', response.status, envelope.code, envelope.data),
      )
    }
    return response
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status ?? 0
    const envelope = error.response?.data
    const message = envelope?.message || error.message || '网络请求失败'
    const code = envelope?.code ?? status
    const hadAuthorization = Boolean(error.config?.headers?.Authorization)

    if (status === 401) {
      setToken(null)
      if (hadAuthorization) redirectToLogin()
    }

    return Promise.reject(new ApiError(message, status, code, envelope?.data ?? error.response?.data))
  },
)

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await api.request<ApiResponse<T>>(config)
  return response.data.data
}
