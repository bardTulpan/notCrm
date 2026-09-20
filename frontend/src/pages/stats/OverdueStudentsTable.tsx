import type { OverdueStudent } from '../../types'

export function OverdueStudentsTable({ students }: { students: OverdueStudent[] }) {
  if (students.length === 0) {
    return (
      <table className="w-full max-w-[820px] border-collapse card overflow-hidden">
        <tbody>
          <tr>
            <td className="px-3.5 py-2.5 text-ink-400 text-[13px]">Никто не превысил норму.</td>
          </tr>
        </tbody>
      </table>
    )
  }
  return (
    <table className="w-full max-w-[820px] border-collapse card overflow-hidden">
      <thead>
        <tr>
          <th className="text-left text-[11px] uppercase tracking-wide text-ink-400 font-semibold px-3.5 py-2.5 border-b border-border">Ученик</th>
          <th className="text-left text-[11px] uppercase tracking-wide text-ink-400 font-semibold px-3.5 py-2.5 border-b border-border">Этап</th>
          <th className="text-left text-[11px] uppercase tracking-wide text-ink-400 font-semibold px-3.5 py-2.5 border-b border-border">Норма</th>
          <th className="text-left text-[11px] uppercase tracking-wide text-ink-400 font-semibold px-3.5 py-2.5 border-b border-border">Куратор</th>
          <th className="text-left text-[11px] uppercase tracking-wide text-ink-400 font-semibold px-3.5 py-2.5 border-b border-border">Фактически</th>
        </tr>
      </thead>
      <tbody>
        {students.map((s) => (
          <tr key={s.studentId}>
            <td className="px-3.5 py-2.5 border-b border-border text-[13px]">{s.name}</td>
            <td className="px-3.5 py-2.5 border-b border-border text-[13px]">{s.stageName}</td>
            <td className="px-3.5 py-2.5 border-b border-border font-mono text-xs">{s.normDays} дн.</td>
            <td className="px-3.5 py-2.5 border-b border-border text-[13px]">{s.curatorName}</td>
            <td className="px-3.5 py-2.5 border-b border-border font-mono text-xs text-warn font-semibold">{s.daysOnStage} дн.</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
