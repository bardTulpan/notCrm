import { Avatar } from '../../components/Avatar'
import type { CuratorWorkload } from '../../types'

const SEGMENTS: { key: 'green' | 'yellow' | 'red' | 'paused'; label: string; color: string }[] = [
  { key: 'green', label: 'В норме', color: 'var(--color-success)' },
  { key: 'yellow', label: 'Подходят к норме', color: 'var(--color-amber)' },
  { key: 'red', label: 'Превысили норму', color: 'var(--color-warn)' },
  { key: 'paused', label: 'На паузе', color: 'var(--color-pause)' },
]

const TH = 'text-left text-[11px] uppercase tracking-wide text-ink-400 font-semibold px-3.5 py-2.5 border-b border-border'
const TD = 'px-3.5 py-2.5 border-b border-border text-[13px]'

export function CuratorWorkloadTable({ rows }: { rows: CuratorWorkload[] }) {
  if (rows.length === 0) return <div className="text-xs text-ink-400">Кураторов пока нет.</div>
  return (
    <div>
      <div className="flex items-center gap-4 mb-3 flex-wrap text-xs text-ink-600">
        {SEGMENTS.map((s) => (
          <span key={s.key} className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-sm inline-block" style={{ background: s.color }} />
            {s.label}
          </span>
        ))}
      </div>
      <table className="w-full max-w-[980px] border-collapse card overflow-hidden">
        <thead>
          <tr>
            <th className={TH}>Куратор</th>
            <th className={TH}>Учеников</th>
            <th className={`${TH} w-[260px]`}>Состояние учеников</th>
            <th className={TH}>Новых за 30 дн.</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.curatorId} className={r.blocked ? 'opacity-60' : ''}>
              <td className={TD}>
                <div className="flex items-center gap-2">
                  <Avatar name={r.name} color={r.avatarColor} />
                  {r.name}
                  {r.blocked && <span className="text-[11px] text-ink-400">(заблокирован)</span>}
                </div>
              </td>
              <td className={`${TD} font-mono text-xs`}>
                {r.students} <span className="text-ink-400">· {r.sharePct}%</span>
              </td>
              <td className={TD}>
                {r.students === 0 ? (
                  <span className="text-ink-400 text-xs">—</span>
                ) : (
                  <div className="flex h-2.5 rounded-full overflow-hidden bg-border" title={SEGMENTS.map((s) => `${s.label}: ${r[s.key]}`).join(' · ')}>
                    {SEGMENTS.map((s) => (
                      <div key={s.key} style={{ width: `${(r[s.key] / r.students) * 100}%`, background: s.color }} />
                    ))}
                  </div>
                )}
                <div className="font-mono text-[11px] text-ink-600 mt-1">
                  {r.green} / {r.yellow} / <span className={r.red > 0 ? 'text-warn font-semibold' : ''}>{r.red}</span> / {r.paused}
                </div>
              </td>
              <td className={`${TD} font-mono text-xs`}>{r.newLast30Days}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
