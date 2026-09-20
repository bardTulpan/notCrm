import { StudentCard } from './StudentCard'
import type { StageDto, StudentDto } from '../../types'

function cardRank(s: StudentDto): number {
  return s.isPaused ? 2 : s.health === 'red' ? 1 : 0
}

export function KanbanBoard({
  stages,
  students,
  curatorName,
  onOpenStudent,
  onMoveStage,
}: {
  stages: StageDto[]
  students: StudentDto[]
  curatorName: (curatorId: string) => { name: string; color: string | null }
  onOpenStudent: (id: string) => void
  onMoveStage: (studentId: string, stageId: string) => void
}) {
  return (
    <div className="flex gap-3.5 overflow-x-auto pb-2">
      {stages.map((stage, idx) => {
        const items = students
          .filter((s) => s.currentStageId === stage.id)
          .sort((a, b) => {
            const ra = cardRank(a)
            const rb = cardRank(b)
            if (ra !== rb) return ra - rb
            if (ra === 1) {
              return b.daysOnStage - a.daysOnStage
            }
            return a.fullName.localeCompare(b.fullName)
          })

        return (
          <div key={stage.id} className="flex-none w-[220px]">
            <div className="h-1 rounded-full bg-border mb-2.5 flex gap-0.5">
              {stages.map((_, i) => (
                <div key={i} className={`flex-1 rounded-full ${i <= idx ? 'bg-accent' : 'bg-border'}`} />
              ))}
            </div>
            <div className="flex items-center justify-between mb-0.5">
              <span className="font-display font-semibold text-[12.5px] leading-tight">
                {idx + 1}. {stage.name}
              </span>
              <span className="font-mono text-[11px] text-ink-400 bg-surface border border-border px-1.5 rounded-full flex-shrink-0 ml-1.5">
                {items.length}
              </span>
            </div>
            <div className="font-mono text-[10.5px] text-ink-400 mb-2.5">
              {stage.normDays != null ? `норма · ${stage.normDays} дн.` : 'финальный шаг'}
            </div>
            <div
              className="min-h-[60px] max-h-[62vh] overflow-y-auto flex flex-col gap-2 rounded-lg p-0.5"
              onDragOver={(e) => e.preventDefault()}
              onDrop={(e) => {
                e.preventDefault()
                const id = e.dataTransfer.getData('text/plain')
                if (id) onMoveStage(id, stage.id)
              }}
            >
              {items.map((s) => {
                const cur = curatorName(s.curatorId)
                return (
                  <StudentCard
                    key={s.id}
                    student={s}
                    normDays={stage.normDays}
                    curatorName={cur.name}
                    curatorColor={cur.color}
                    onClick={() => onOpenStudent(s.id)}
                    onDragStart={(e) => e.dataTransfer.setData('text/plain', s.id)}
                  />
                )
              })}
            </div>
          </div>
        )
      })}
    </div>
  )
}
