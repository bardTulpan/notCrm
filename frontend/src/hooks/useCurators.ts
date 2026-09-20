import { useEffect, useState } from 'react'
import { usersApi } from '../api/users'
import { useAuth } from '../auth/useAuth'
import type { UserDto } from '../types'

/**
 * ADMIN can list all curators via /users. CURATOR cannot (403) and doesn't need to —
 * every lead/student they can see already belongs to them.
 */
export function useCurators() {
  const { user } = useAuth()
  const [curators, setCurators] = useState<UserDto[]>([])
  const [loading, setLoading] = useState(user?.role === 'ADMIN')

  useEffect(() => {
    if (user?.role !== 'ADMIN') {
      setLoading(false)
      return
    }
    let cancelled = false
    usersApi
      .list()
      .then((list) => {
        if (!cancelled) setCurators(list.filter((u) => u.role === 'CURATOR'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [user])

  function displayName(curatorId: string | null): { name: string; color: string | null } {
    if (!curatorId) return { name: '—', color: null }
    if (user?.id === curatorId) return { name: user.fullName, color: user.avatarColor }
    const found = curators.find((c) => c.id === curatorId)
    return found ? { name: found.fullName, color: found.avatarColor } : { name: '…', color: null }
  }

  return { curators, loading, displayName }
}
