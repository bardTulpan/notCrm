/** Days on stage as a bar against the stage norm; the tick marks 70%, where TimeBadge turns amber. */
export function NormMeter({ daysOnStage, normDays }: { daysOnStage: number; normDays: number }) {
  const d = daysOnStage
  const tone = d > normDays ? 'crit' : d >= normDays * 0.7 ? 'warn' : 'ok'
  const fill = { ok: 'bg-ink-400', warn: 'bg-amber', crit: 'bg-warn' }[tone]
  const label = { ok: 'text-ink-600', warn: 'text-amber', crit: 'text-warn font-semibold' }[tone]
  const pct = Math.min(d / normDays, 1) * 100

  return (
    <div className="flex items-center gap-2" title={`${d} из ${normDays} дн. нормы этапа`}>
      <div className="relative flex-1 h-1.5 rounded-full bg-ink-900/[0.07] overflow-hidden">
        <div className={`h-full rounded-full ${fill}`} style={{ width: `${pct}%` }} />
        <div className="absolute top-0 bottom-0 left-[70%] w-px bg-ink-900/30" />
      </div>
      <span className={`font-mono text-[11px] whitespace-nowrap tabular-nums ${label}`}>
        {d} / {normDays} дн.
      </span>
    </div>
  )
}
