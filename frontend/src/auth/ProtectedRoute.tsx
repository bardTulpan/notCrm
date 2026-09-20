import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from './useAuth'
import { Loader } from '../components/Loader'

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { status } = useAuth()

  if (status === 'loading') {
    return <Loader full />
  }
  if (status === 'anonymous') {
    return <Navigate to="/login" replace />
  }
  return <>{children}</>
}
