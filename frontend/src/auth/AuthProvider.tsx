import { createContext, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { authApi } from '../api/auth'
import { registerAuthHooks, setAccessToken } from '../api/client'
import type { AuthUserDto } from '../types'

interface AuthContextValue {
  user: AuthUserDto | null
  status: 'loading' | 'authenticated' | 'anonymous'
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

// eslint-disable-next-line react-refresh/only-export-components
export const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUserDto | null>(null)
  const [status, setStatus] = useState<'loading' | 'authenticated' | 'anonymous'>('loading')

  const clearAuth = useCallback(() => {
    setAccessToken(null)
    setUser(null)
    setStatus('anonymous')
  }, [])

  useEffect(() => {
    registerAuthHooks({
      refresh: async () => {
        try {
          const res = await authApi.refresh()
          setAccessToken(res.accessToken)
          setUser(res.user)
          setStatus('authenticated')
          return res.accessToken
        } catch {
          return null
        }
      },
      onAuthLost: clearAuth,
    })
  }, [clearAuth])

  useEffect(() => {
    // Try to restore the session from the refresh cookie on first load.
    authApi
      .refresh()
      .then((res) => {
        setAccessToken(res.accessToken)
        setUser(res.user)
        setStatus('authenticated')
      })
      .catch(() => setStatus('anonymous'))
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    const res = await authApi.login(username, password)
    setAccessToken(res.accessToken)
    setUser(res.user)
    setStatus('authenticated')
  }, [])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      clearAuth()
    }
  }, [clearAuth])

  const value = useMemo(() => ({ user, status, login, logout }), [user, status, login, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
