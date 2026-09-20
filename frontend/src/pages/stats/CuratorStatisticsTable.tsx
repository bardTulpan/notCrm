import { Avatar } from '../../components/Avatar'
import type { CuratorStats } from '../../types'

export function CuratorStatisticsTable({ curators }: { curators: CuratorStats[] }) {
  return (
    <table className="w-full max-w-[820px] border-collapse card overflow-hidden">
      <thead>
        <tr>
          <th className="text-left text-[11px] uppercase tracking-wide text-ink-400 font-semibold px-3.5 py-2.5 border-b border-border">Куратор</th>
          <th className="text-left text-[11px] uppercase tracking-wide text-ink-400 font-semibold px-3.5 py-2.5 border-b border-border">Учеников</th>
          <th className="text-left text-[11px] uppercase tracking-wide text-ink-400 font-semibold px-3.5 py-2.5 border-b border-border">Превысили норму</th>
          <th className="text-left text-[11px] uppercase tracking-wide text-ink-400 font-semibold px-3.5 py-2.5 border-b border-border">Средний темп (% от нормы)</th>
        </tr>
      </thead>
      <tbody>
        {curators.map((r) => (
          <tr key={r.curatorId}>
            <td className="px-3.5 py-2.5 border-b border-border text-[13px]">
              <div className="flex items-center gap-2">
                <Avatar name={r.name} color={r.avatarColor} />
                {r.name}
              </div>
            </td>
            <td className="px-3.5 py-2.5 border-b border-border font-mono text-xs">{r.count}</td>
            <td className={`px-3.5 py-2.5 border-b border-border font-mono text-xs ${r.stuck > 0 ? 'text-warn font-semibold' : ''}`}>
              {r.stuck}
            </td>
            <td className={`px-3.5 py-2.5 border-b border-border font-mono text-xs ${r.avgPct > 100 ? 'text-warn font-semibold' : ''}`}>
              {r.avgPct}%
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
