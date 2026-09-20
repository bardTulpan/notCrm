export function TimeBadge({
  daysOnStage,
  normDays,
  isPaused,
}: {
  daysOnStage: number
  normDays: number | null
  isPaused: boolean
}) {
  if (isPaused) {
    return <span className="time-badge bg-pause-soft text-pause px-1.5 py-0.5 rounded-md font-mono text-[11px]">⏸ на паузе</span>
  }
  const d = daysOnStage
  let cls = 'bg-bg text-ink-600'
  if (normDays != null) {
    if (d > normDays) cls = 'bg-warn text-white font-semibold'
    else if (d >= normDays * 0.7) cls = 'bg-amber-soft text-amber'
  }
  return <span className={`px-1.5 py-0.5 rounded-md font-mono text-[11px] inline-block ${cls}`}>{d} дн. на этапе</span>
}

export function pingBadgeClass(tier: 'overdue' | 'today' | 'upcoming'): string {
  if (tier === 'overdue') return 'bg-warn-soft text-warn'
  if (tier === 'today') return 'bg-amber-soft text-amber'
  return 'bg-bg text-ink-600'
}
