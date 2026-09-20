import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from './useAuth'
import type { Role } from '../types'

export function RoleRoute({ role, children }: { role: Role; children: ReactNode }) {
  const { user } = useAuth()

  if (user?.role !== role) {
    return <Navigate to="/leads" replace />
  }
  return <>{children}</>
}
