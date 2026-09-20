import type { Health } from '../types'

const CONFIG: Record<Health, { icon: string; label: string; className: string }> = {
  green: { icon: '🟢', label: 'по плану', className: 'text-success' },
  yellow: { icon: '🟡', label: 'внимание', className: 'text-amber' },
  red: { icon: '🔴', label: 'застрял', className: 'text-warn font-semibold' },
  paused: { icon: '⚪', label: 'на паузе', className: 'text-pause' },
}

export function HealthBadge({ health, showLabel = true }: { health: Health; showLabel?: boolean }) {
  const c = CONFIG[health]
  return (
    <span className={`text-xs font-mono inline-flex items-center gap-1 ${c.className}`}>
      <span>{c.icon}</span>
      {showLabel && <span>{c.label}</span>}
    </span>
  )
}
