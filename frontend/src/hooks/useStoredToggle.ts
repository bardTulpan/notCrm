import { useEffect, useState } from 'react'

/** A boolean view preference remembered per browser (not security-sensitive, unlike the auth token). */
export function useStoredToggle(key: string, initial = false) {
  const [value, setValue] = useState<boolean>(() => {
    try {
      const raw = localStorage.getItem(key)
      return raw == null ? initial : raw === '1'
    } catch {
      return initial
    }
  })

  useEffect(() => {
    try {
      localStorage.setItem(key, value ? '1' : '0')
    } catch {
      // storage unavailable (private mode, blocked site data) — keep the in-memory value
    }
  }, [key, value])

  const toggle = () => setValue((v) => !v)
  return [value, toggle] as const
}
