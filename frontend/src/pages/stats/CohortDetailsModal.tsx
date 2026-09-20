import { useEffect, useState } from 'react'
import { Modal } from '../../components/Modal'
import { Avatar } from '../../components/Avatar'
import { HealthBadge } from '../../components/HealthBadge'
import { statisticsApi } from '../../api/statistics'
import { studentsApi } from '../../api/students'
import { useAuth } from '../../auth/useAuth'
import { useCurators } from '../../hooks/useCurators'
import { useCohorts } from '../../hooks/useCohorts'
import { useToast, apiErrorMessage } from '../../hooks/useToast'
import { daysSince } from '../../utils/dates'
import { pluralPeople } from '../../utils/plural'
import { StudentDetailsModal } from '../students/StudentDetailsModal'
import type { CohortStats, StageDto, StudentDto } from '../../types'

export function CohortDetailsModal({
  cohortId,
  stages,
  onClose,
}: {
  cohortId: string
  stages: StageDto[]
  onClose: () => void
}) {
  const { user } = useAuth()
  const { displayName } = useCurators()
  const { cohorts } = useCohorts()
  const { push } = useToast()
  const [detail, setDetail] = useState<CohortStats | null>(null)
  const [students, setStudents] = useState<StudentDto[]>([])
  const [openStudentId, setOpenStudentId] = useState<string | null>(null)

  function refetch() {
    statisticsApi.cohortDetail(cohortId).then(setDetail)
    studentsApi.list({ cohortId }).then(setStudents)
  }

  useEffect(() => {
    refetch()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cohortId])

  if (!detail) return null

  const plannedStage = stages.find((s) => s.position === detail.plannedStagePosition)
  const elapsed = detail.startDate ? daysSince(detail.startDate) : 0
  let cumBefore = 0
  for (const s of stages) {
    if (s.position === detail.plannedStagePosition) break
    cumBefore += s.normDays ?? 0
  }
  const dayInStage = elapsed - cumBefore + 1
  const planLabel = plannedStage
    ? `${(plannedStage.position ?? 0) + 1}. ${plannedStage.name}${plannedStage.normDays != null ? ` (день ${dayInStage} из ${plannedStage.normDays})` : ''}`
    : '—'
  const onTrackPct = detail.total ? Math.round((detail.onTrackCount / detail.total) * 100) : 0
  const behindPct = detail.total ? 100 - onTrackPct : 0

  async function reassignCohort(studentId: string, newCohortId: string) {
    try {
      await studentsApi.update(studentId, { cohortId: newCohortId })
      refetch()
    } catch (err) {
      push('error', apiErrorMessage(err))
    }
  }

  return (
    <Modal title={`Когорта: ${detail.name ?? '—'}`} subtitle={`${detail.total} ${pluralPeople(detail.total)} · ${elapsed} дн. в пути`} onClose={onClose}>
      <div className="bg-bg border border-border rounded-lg px-3.5 mb-4">
        <div className="flex items-center justify-between py-2.5 text-[13px]">
          <span className="text-ink-600">Плановый этап</span>
          <span className="font-semibold">{planLabel}</span>
        </div>
        <div className="flex items-center justify-between py-2.5 text-[13px] border-t border-border">
          <span className="text-ink-600">На плановом этапе (или дальше)</span>
          <span className="font-semibold text-success">
            {onTrackPct}%<span className="font-normal text-ink-400 text-xs ml-1">({detail.onTrackCount} {pluralPeople(detail.onTrackCount)})</span>
          </span>
        </div>
        <div className="flex items-center justify-between py-2.5 text-[13px] border-t border-border">
          <span className="text-ink-600">Отстают</span>
          <span className={`font-semibold ${detail.behindCount > 0 ? 'text-warn' : 'text-ink-600'}`}>
            {behindPct}%<span className="font-normal text-ink-400 text-xs ml-1">({detail.behindCount} {pluralPeople(detail.behindCount)})</span>
          </span>
        </div>
      </div>

      {students.length === 0 && <div className="text-xs text-ink-400">В этой когорте пока никого нет.</div>}
      {students.map((s) => {
        const cur = displayName(s.curatorId)
        const stageName = stages.find((st) => st.id === s.currentStageId)?.name ?? '—'
        return (
          <div
            key={s.id}
            className="flex items-center justify-between border border-border rounded-lg px-3.5 py-2.5 mb-2 cursor-pointer hover:border-accent"
            onClick={() => setOpenStudentId(s.id)}
          >
            <div className="flex items-center gap-2 text-[13px] font-semibold">
              <HealthBadge health={s.health} showLabel={false} />
              <Avatar name={cur.name} color={cur.color} />
              <span>{s.fullName}</span>
              <span className="font-normal text-ink-600 text-xs">· {stageName}</span>
            </div>
            {user?.role === 'ADMIN' && (
              <select
                className="input text-[11px]"
                value={s.cohortId ?? ''}
                onClick={(e) => e.stopPropagation()}
                onChange={(e) => reassignCohort(s.id, e.target.value)}
              >
                {cohorts.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            )}
          </div>
        )
      })}

      {openStudentId && (
        <StudentDetailsModal
          student={students.find((s) => s.id === openStudentId)!}
          stages={stages}
          onClose={() => setOpenStudentId(null)}
          onChanged={refetch}
        />
      )}
    </Modal>
  )
}
