import type { StatsOverview } from '../../types'

export function OverviewStats({ stats }: { stats: StatsOverview }) {
  // Paused usually means the student dropped out, so they stay outside the "studying" total.
  const studying = stats.total - stats.paused
  const boxes: { label: string; value: number; icon: string; className?: string }[] = [
    { label: 'Учатся', value: studying, icon: '' },
    { label: 'Идут по плану', value: stats.green, icon: '🟢 ', className: 'text-success' },
    { label: 'Требуют внимания', value: stats.yellow, icon: '🟡 ', className: 'text-amber' },
    { label: 'Застряли', value: stats.red, icon: '🔴 ', className: 'text-warn' },
  ]

  return (
    <div className="flex gap-3 flex-wrap items-stretch mb-5">
      {boxes.map((b) => (
        <div key={b.label} className="card px-5 py-4 min-w-[140px]">
          <div className={`font-display font-bold text-2xl ${b.className ?? ''}`}>
            {b.icon}
            {b.value}
          </div>
          <div className="text-xs text-ink-600 mt-0.5">{b.label}</div>
        </div>
      ))}
      <div className="w-px bg-border mx-1.5 self-stretch" aria-hidden="true" />
      <div className="card px-5 py-4 min-w-[140px] bg-pause-soft border-dashed">
        <div className="font-display font-bold text-2xl text-pause">⚪ {stats.paused}</div>
        <div className="text-xs text-ink-600 mt-0.5">На паузе</div>
        <div className="text-[11px] text-ink-400 mt-0.5">всего с паузой: {stats.total}</div>
      </div>
    </div>
  )
}
