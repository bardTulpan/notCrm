import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ApiError } from '../types'

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

export const client = axios.create({ baseURL })

// Access token lives only in memory (never localStorage), per S3-02.
let accessToken: string | null = null
export function setAccessToken(token: string | null) {
  accessToken = token
}
export function getAccessToken() {
  return accessToken
}

type Resolver = (token: string | null) => void
let isRefreshing = false
let waiters: Resolver[] = []

function onRefreshed(token: string | null) {
  waiters.forEach((resolve) => resolve(token))
  waiters = []
}

/** Set by AuthProvider; used to attempt session restore / clear state on refresh failure. */
let refreshHandler: (() => Promise<string | null>) | null = null
let onAuthLost: (() => void) | null = null
export function registerAuthHooks(handlers: {
  refresh: () => Promise<string | null>
  onAuthLost: () => void
}) {
  refreshHandler = handlers.refresh
  onAuthLost = handlers.onAuthLost
}

client.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (accessToken) {
    config.headers.set('Authorization', `Bearer ${accessToken}`)
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined
    const url = original?.url ?? ''
    const isAuthRoute = url.includes('/auth/login') || url.includes('/auth/refresh')

    if (error.response?.status === 401 && original && !original._retry && !isAuthRoute && refreshHandler) {
      original._retry = true

      if (!isRefreshing) {
        isRefreshing = true
        try {
          const newToken = await refreshHandler()
          isRefreshing = false
          onRefreshed(newToken)
          if (!newToken) {
            onAuthLost?.()
            return Promise.reject(toApiError(error))
          }
        } catch {
          isRefreshing = false
          onRefreshed(null)
          onAuthLost?.()
          return Promise.reject(toApiError(error))
        }
      }

      const token = await new Promise<string | null>((resolve) => waiters.push(resolve))
      if (!token) {
        return Promise.reject(toApiError(error))
      }
      original.headers.set('Authorization', `Bearer ${token}`)
      return client(original)
    }

    return Promise.reject(toApiError(error))
  },
)

function toApiError(error: AxiosError): ApiError {
  const data = error.response?.data as Partial<ApiError> | undefined
  return {
    status: error.response?.status ?? 0,
    message: data?.message ?? error.message ?? 'Network error',
    errors: data?.errors,
  }
}
