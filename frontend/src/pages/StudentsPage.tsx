import { useCallback, useEffect, useState } from 'react'
import { studentsApi } from '../api/students'
import { useStages } from '../hooks/useStages'
import { useCohorts } from '../hooks/useCohorts'
import { csvDateStamp, downloadCsv } from '../utils/csv'
import { formatDate } from '../utils/dates'
import { useCurators } from '../hooks/useCurators'
import { Loader } from '../components/Loader'
import { ErrorState } from '../components/ErrorState'
import { EmptyState } from '../components/EmptyState'
import { useToast, apiErrorMessage } from '../hooks/useToast'
import { StudentFilters } from './students/StudentFilters'
import { KanbanBoard } from './students/KanbanBoard'
import { StudentDetailsModal } from './students/StudentDetailsModal'
import { AddStudentModal } from './students/AddStudentModal'
import type { StudentDto } from '../types'

/** Mirrors the backend's reorder() placement so the board updates instantly, before the server confirms. */
function applyLocalReorder(
  students: StudentDto[],
  studentId: string,
  stageId: string,
  beforeStudentId: string | null,
): StudentDto[] {
  const moving = students.find((s) => s.id === studentId)
  if (!moving) return students
  const stageChanged = moving.currentStageId !== stageId
  const updatedMoving: StudentDto = stageChanged
    ? { ...moving, currentStageId: stageId, stageEnteredAt: new Date().toISOString(), daysOnStage: 0, health: 'green' }
    : moving

  const siblings = students
    .filter((s) => s.currentStageId === stageId && s.id !== studentId)
    .sort((a, b) => a.stagePosition - b.stagePosition)
  const insertAt = beforeStudentId ? siblings.findIndex((s) => s.id === beforeStudentId) : -1
  siblings.splice(insertAt === -1 ? siblings.length : insertAt, 0, updatedMoving)

  const renumbered = new Map(siblings.map((s, i) => [s.id, { ...s, stagePosition: i }]))
  return students.map((s) => renumbered.get(s.id) ?? s)
}

export function StudentsPage() {
  const { stages, loading: stagesLoading, error: stagesError } = useStages()
  const { displayName } = useCurators()
  const { cohorts } = useCohorts()
  const { push } = useToast()

  const [students, setStudents] = useState<StudentDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const [onlyStuck, setOnlyStuck] = useState(false)
  const [selectedCuratorIds, setSelectedCuratorIds] = useState<Set<string>>(new Set())
  const [openStudentId, setOpenStudentId] = useState<string | null>(null)
  const [adding, setAdding] = useState(false)

  const refetch = useCallback(() => {
    setError(null)
    return studentsApi
      .list({ onlyOverdue: onlyStuck, search: query || undefined })
      .then(setStudents)
      .catch((e) => setError(apiErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [onlyStuck, query])

  useEffect(() => {
    refetch()
  }, [refetch])

  function toggleCurator(id: string | 'all') {
    if (id === 'all') {
      setSelectedCuratorIds(new Set())
      return
    }
    setSelectedCuratorIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  function reorderStudent(studentId: string, stageId: string, beforeStudentId: string | null) {
    const student = students.find((s) => s.id === studentId)
    if (!student) return
    const stageChanged = student.currentStageId !== stageId
    const originalStageId = student.currentStageId

    const prevStudents = students
    setStudents((current) => applyLocalReorder(current, studentId, stageId, beforeStudentId))

    studentsApi
      .reorder(studentId, stageId, beforeStudentId)
      .then(() => {
        if (stageChanged) {
          const targetStage = stages.find((s) => s.id === stageId)
          push('success', `${student.fullName} → ${targetStage?.name ?? 'другой этап'}`, {
            actionLabel: 'Отменить',
            durationMs: 7000,
            onAction: () => {
              studentsApi.reorder(studentId, originalStageId, null).catch(() => undefined).finally(refetch)
            },
          })
        }
        refetch()
      })
      .catch((err) => {
        setStudents(prevStudents)
        push('error', apiErrorMessage(err))
      })
  }

  function exportCsv() {
    const stageName = (id: string) => stages.find((s) => s.id === id)?.name ?? ''
    const cohortName = (id: string | null) => cohorts.find((c) => c.id === id)?.name ?? ''
    const healthLabel: Record<StudentDto['health'], string> = { green: 'по плану', yellow: 'внимание', red: 'застрял', paused: 'на паузе' }
    downloadCsv(
      `ученики-${csvDateStamp()}.csv`,
      ['Имя', 'Telegram', 'Этап', 'Куратор', 'Когорта', 'Дата старта', 'На этапе с', 'Дней на этапе', 'Статус', 'Постоплата, %'],
      visible.map((s) => [
        s.fullName,
        s.telegramUsername,
        stageName(s.currentStageId),
        displayName(s.curatorId).name,
        cohortName(s.cohortId),
        formatDate(s.startedAt),
        formatDate(s.stageEnteredAt),
        s.daysOnStage,
        healthLabel[s.health],
        s.postpayPercent,
      ]),
    )
  }

  const visible = selectedCuratorIds.size === 0 ? students : students.filter((s) => selectedCuratorIds.has(s.curatorId))
  const openStudent = students.find((s) => s.id === openStudentId) ?? null

  if (stagesLoading || loading) return <Loader />
  if (stagesError) return <ErrorState message={stagesError} />
  if (error) return <ErrorState message={error} onRetry={refetch} />

  return (
    <div>
      <StudentFilters
        query={query}
        onQueryChange={setQuery}
        selectedCuratorIds={selectedCuratorIds}
        onToggleCurator={toggleCurator}
        onlyStuck={onlyStuck}
        onToggleStuck={() => setOnlyStuck((v) => !v)}
        onAddStudent={() => setAdding(true)}
        onExport={exportCsv}
      />
      {visible.length === 0 ? (
        <EmptyState />
      ) : (
        <KanbanBoard
          stages={stages}
          students={visible}
          curatorName={displayName}
          onOpenStudent={setOpenStudentId}
          onReorder={reorderStudent}
        />
      )}
      {adding && <AddStudentModal stages={stages} onClose={() => setAdding(false)} onCreated={refetch} />}
      {openStudent && (
        <StudentDetailsModal
          student={openStudent}
          stages={stages}
          onClose={() => setOpenStudentId(null)}
          onChanged={refetch}
        />
      )}
    </div>
  )
}
