import type { StatsOverview } from '../../types'

export function OverviewStats({ stats }: { stats: StatsOverview }) {
  const boxes: { label: string; value: number; className?: string }[] = [
    { label: 'Всего учеников', value: stats.total },
    { label: 'Идут по плану', value: stats.green, className: 'text-success' },
    { label: 'Требуют внимания', value: stats.yellow, className: 'text-amber' },
    { label: 'Застряли', value: stats.red, className: 'text-warn' },
    { label: 'На паузе', value: stats.paused, className: 'text-pause' },
  ]
  const icons = ['', '🟢 ', '🟡 ', '🔴 ', '⚪ ']

  return (
    <div className="flex gap-3 flex-wrap mb-5">
      {boxes.map((b, i) => (
        <div key={b.label} className="card px-5 py-4 min-w-[140px]">
          <div className={`font-display font-bold text-2xl ${b.className ?? ''}`}>
            {icons[i]}
            {b.value}
          </div>
          <div className="text-xs text-ink-600 mt-0.5">{b.label}</div>
        </div>
      ))}
    </div>
  )
}
