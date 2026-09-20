import type { StageStats } from '../../types'

export function StageStatisticsTable({ stages }: { stages: StageStats[] }) {
  return (
    <table className="w-full max-w-[820px] border-collapse card overflow-hidden">
      <thead>
        <tr>
          <th className="text-left text-[11px] uppercase tracking-wide text-ink-400 font-semibold px-3.5 py-2.5 border-b border-border">Этап</th>
          <th className="text-left text-[11px] uppercase tracking-wide text-ink-400 font-semibold px-3.5 py-2.5 border-b border-border">Норма</th>
          <th className="text-left text-[11px] uppercase tracking-wide text-ink-400 font-semibold px-3.5 py-2.5 border-b border-border">Сейчас учеников</th>
          <th className="text-left text-[11px] uppercase tracking-wide text-ink-400 font-semibold px-3.5 py-2.5 border-b border-border">Застряло (🔴)</th>
        </tr>
      </thead>
      <tbody>
        {stages.map((st, idx) => (
          <tr key={st.stageId}>
            <td className="px-3.5 py-2.5 border-b border-border text-[13px] last:border-b-0">
              {idx + 1}. {st.name}
            </td>
            <td className="px-3.5 py-2.5 border-b border-border font-mono text-xs">{st.normDays != null ? `${st.normDays} дн.` : '—'}</td>
            <td className="px-3.5 py-2.5 border-b border-border font-mono text-xs">{st.onStage}</td>
            <td className={`px-3.5 py-2.5 border-b border-border font-mono text-xs ${st.stuck > 0 ? 'text-warn font-semibold' : ''}`}>
              {st.stuck}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
