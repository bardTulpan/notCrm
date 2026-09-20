import { daysSince } from '../../utils/dates'
import type { CohortStats, StageDto } from '../../types'

export function CohortFunnelTable({
  cohorts,
  stages,
  onOpenCohort,
}: {
  cohorts: CohortStats[]
  stages: StageDto[]
  onOpenCohort: (id: string) => void
}) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse card overflow-hidden">
        <thead>
          <tr>
            <th className="text-left text-[11px] uppercase tracking-wide text-ink-400 font-semibold px-3.5 py-2.5 border-b border-border whitespace-nowrap">
              Когорта
            </th>
            <th className="text-left text-[11px] uppercase tracking-wide text-ink-400 font-semibold px-3.5 py-2.5 border-b border-border">Всего</th>
            {stages.map((s, i) => (
              <th key={s.id} title={s.name} className="text-center text-[11px] text-ink-400 font-semibold px-2 py-2.5 border-b border-border">
                {i + 1}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {cohorts.map((co) => (
            <tr key={co.cohortId} className="cursor-pointer hover:bg-bg" onClick={() => onOpenCohort(co.cohortId)}>
              <td className="px-3.5 py-2.5 border-b border-border whitespace-nowrap">
                <div className="font-semibold text-[13px]">{co.name}</div>
                <div className="text-[10.5px] text-ink-400 mt-0.5">{co.startDate ? daysSince(co.startDate) : 0} дн. в пути</div>
              </td>
              <td className="px-3.5 py-2.5 border-b border-border font-mono text-xs">{co.total}</td>
              {stages.map((stage, idx) => {
                const reached = co.reachedCounts[idx] ?? 0
                const pct = co.total ? Math.round((reached / co.total) * 100) : 0
                const isPlanned = stage.position === co.plannedStagePosition
                return (
                  <td
                    key={stage.id}
                    className="px-2 py-2.5 border-b border-border text-center font-mono text-xs"
                    style={{
                      background: `rgba(47,94,255,${((pct / 100) * 0.35).toFixed(2)})`,
                      boxShadow: isPlanned ? 'inset 0 0 0 2px var(--color-accent)' : undefined,
                      fontWeight: isPlanned ? 700 : 400,
                    }}
                  >
                    {pct}%
                  </td>
                )
              })}
            </tr>
          ))}
        </tbody>
      </table>
      <div className="text-xs text-ink-400 mt-3 max-w-[720px]">
        Под названием когорты — сколько дней она в пути. Процент в колонках — доля когорты, достигшая этапа. Ячейка с синей
        рамкой — плановый этап сейчас. Клик по строке — список учеников когорты и точный план.
      </div>
    </div>
  )
}
