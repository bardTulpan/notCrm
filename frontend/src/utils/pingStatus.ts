import { daysSince, formatDateShort } from './dates'

export type PingTier = 'overdue' | 'today' | 'upcoming'

export function pingStatus(value: string | null): { tier: PingTier; label: string } {
  if (!value) return { tier: 'upcoming', label: '—' }
  const diff = -daysSince(value)
  if (diff < 0) return { tier: 'overdue', label: `просрочен · ${formatDateShort(value)}` }
  if (diff === 0) return { tier: 'today', label: 'сегодня' }
  return { tier: 'upcoming', label: formatDateShort(value) }
}
