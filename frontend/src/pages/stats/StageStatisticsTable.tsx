import type { StageStats } from '../../types'

const th = 'text-left text-[11px] uppercase tracking-wide text-ink-400 font-semibold px-3.5 py-2.5 border-b border-border'
const td = 'px-3.5 py-2.5 border-b border-border'

export function StageStatisticsTable({ stages }: { stages: StageStats[] }) {
  const totals = stages.reduce(
    (acc, st) => ({ active: acc.active + st.active, stuck: acc.stuck + st.stuck, paused: acc.paused + st.paused }),
    { active: 0, stuck: 0, paused: 0 },
  )

  return (
    <table className="w-full max-w-[820px] border-collapse card overflow-hidden">
      <thead>
        <tr>
          <th className={th}>Этап</th>
          <th className={th}>Норма</th>
          <th className={th}>Учатся</th>
          <th className={th}>Застряло (🔴)</th>
          <th className={`${th} border-l`}>На паузе</th>
        </tr>
      </thead>
      <tbody>
        {stages.map((st, idx) => (
          <tr key={st.stageId}>
            <td className={`${td} text-[13px]`}>
              {idx + 1}. {st.name}
            </td>
            <td className={`${td} font-mono text-xs`}>{st.normDays != null ? `${st.normDays} дн.` : '—'}</td>
            <td className={`${td} font-mono text-xs`}>{st.active}</td>
            <td className={`${td} font-mono text-xs ${st.stuck > 0 ? 'text-warn font-semibold' : ''}`}>{st.stuck}</td>
            <td className={`${td} border-l font-mono text-xs ${st.paused > 0 ? 'text-pause' : 'text-ink-400'}`}>{st.paused}</td>
          </tr>
        ))}
      </tbody>
      <tfoot>
        <tr className="bg-bg/60">
          <td className="px-3.5 py-2.5 text-[13px] font-semibold">Итого</td>
          <td className="px-3.5 py-2.5" />
          <td className="px-3.5 py-2.5 font-mono text-xs font-semibold">{totals.active}</td>
          <td className={`px-3.5 py-2.5 font-mono text-xs font-semibold ${totals.stuck > 0 ? 'text-warn' : ''}`}>{totals.stuck}</td>
          <td className="px-3.5 py-2.5 border-l border-border font-mono text-xs font-semibold text-pause">{totals.paused}</td>
        </tr>
      </tfoot>
    </table>
  )
}
