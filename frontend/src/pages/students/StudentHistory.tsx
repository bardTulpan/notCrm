import { useEffect, useState } from 'react'
import { studentsApi } from '../../api/students'
import { formatDateShort } from '../../utils/dates'
import type { StageDto, StudentDto, StudentHistoryDto } from '../../types'

interface Event {
  at: number
  node: React.ReactNode
}

export function StudentHistory({
  student,
  stages,
  curatorName,
}: {
  student: StudentDto
  stages: StageDto[]
  curatorName: (id: string) => string
}) {
  const [history, setHistory] = useState<StudentHistoryDto | null>(null)

  useEffect(() => {
    studentsApi.history(student.id).then(setHistory)
  }, [student.id])

  if (!history) return <div className="text-xs text-ink-400">Загрузка истории…</div>

  const stageName = (id: string) => stages.find((s) => s.id === id)?.name ?? '—'

  const events: Event[] = []
  const allStageEvents = [
    ...history.stages,
    { stageId: student.currentStageId, enteredAt: student.stageEnteredAt, exitedAt: null, changedById: '' },
  ]
  for (const h of allStageEvents) {
    const isNow = h.exitedAt === null
    const dur = isNow ? student.daysOnStage : Math.round((new Date(h.exitedAt!).getTime() - new Date(h.enteredAt).getTime()) / 86_400_000)
    events.push({
      at: new Date(h.enteredAt).getTime(),
      node: (
        <div className="flex items-center gap-2.5 py-2 border-b border-border text-[13px]">
          <span className={`w-1.5 h-1.5 rounded-full flex-shrink-0 ${isNow ? 'bg-accent' : 'bg-border'}`} />
          <span className="flex-1">
            {stageName(h.stageId)}
            <br />
            <span className="text-ink-400 text-[11px]">
              {formatDateShort(h.enteredAt)} — {isNow ? 'сейчас' : formatDateShort(h.exitedAt)}
            </span>
          </span>
          <span className="font-mono text-xs text-ink-600">{dur} дн.</span>
        </div>
      ),
    })
  }
  for (const c of history.curators) {
    events.push({
      at: new Date(c.changedAt).getTime(),
      node: (
        <div className="flex items-center gap-2.5 py-2 border-b border-border text-[12.5px] text-ink-600">
          <span className="w-1.5 h-1.5 rounded-full flex-shrink-0 bg-ink-400" />
          <span className="flex-1">
            Куратор изменён: {c.fromCuratorId ? curatorName(c.fromCuratorId) : '—'} → {curatorName(c.toCuratorId)}
            <br />
            <span className="text-ink-400 text-[11px]">{formatDateShort(c.changedAt)}</span>
          </span>
        </div>
      ),
    })
  }
  events.sort((a, b) => a.at - b.at)

  return <div>{events.map((e, i) => <div key={i}>{e.node}</div>)}</div>
}
