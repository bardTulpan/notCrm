import { useContext } from 'react'
import { ToastContext } from '../components/Toast'
import type { ApiError } from '../types'

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) {
    throw new Error('useToast must be used within ToastProvider')
  }
  return ctx
}

export function apiErrorMessage(error: unknown): string {
  const e = error as ApiError
  if (e?.errors) {
    return Object.values(e.errors).join(', ')
  }
  return e?.message ?? 'Что-то пошло не так'
}
