import { useEffect, useState } from 'react'
import { usersApi } from '../api/users'
import { useAuth } from '../auth/useAuth'

export interface CuratorOption {
  id: string
  fullName: string
  avatarColor: string | null
}

/**
 * Names for everyone (via the names-only /directory/users, open to every signed-in user — a curator needs the
 * admin's name on a comment or the previous curator's name in a student's history) plus the list of active
 * curators for pickers and filters.
 */
export function useCurators() {
  const { user } = useAuth()
  const [people, setPeople] = useState<{ id: string; fullName: string; avatarColor: string | null; role: string; blocked: boolean }[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    usersApi
      .directory()
      .then((list) => {
        if (!cancelled) setPeople(list)
      })
      .catch(() => undefined)
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [user])

  // Admin's filter chips keep showing blocked curators too (they may still own students); pickers elsewhere want active ones.
  const curators: CuratorOption[] = people
    .filter((p) => p.role === 'CURATOR' && (user?.role === 'ADMIN' || !p.blocked))
    .map(({ id, fullName, avatarColor }) => ({ id, fullName, avatarColor }))

  function displayName(personId: string | null): { name: string; color: string | null } {
    if (!personId) return { name: '—', color: null }
    if (user?.id === personId) return { name: user.fullName, color: user.avatarColor }
    const found = people.find((p) => p.id === personId)
    return found ? { name: found.fullName, color: found.avatarColor } : { name: loading ? '…' : 'Неизвестный', color: null }
  }

  return { curators, loading, displayName }
}
