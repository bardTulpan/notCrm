import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from './useAuth'
import type { AuthUserDto } from '../types'

export function PermissionRoute({
  permission,
  children,
}: {
  permission: keyof Pick<AuthUserDto, 'seeLeads' | 'seeStats'>
  children: ReactNode
}) {
  const { user } = useAuth()

  if (user && user.role !== 'ADMIN' && !user[permission]) {
    return <Navigate to="/students" replace />
  }
  return <>{children}</>
}
