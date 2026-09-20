import { useCallback, useEffect, useState } from 'react'
import { studentsApi } from '../api/students'
import { useStages } from '../hooks/useStages'
import { useCurators } from '../hooks/useCurators'
import { Loader } from '../components/Loader'
import { ErrorState } from '../components/ErrorState'
import { EmptyState } from '../components/EmptyState'
import { useToast, apiErrorMessage } from '../hooks/useToast'
import { StudentFilters } from './students/StudentFilters'
import { KanbanBoard } from './students/KanbanBoard'
import { StudentDetailsModal } from './students/StudentDetailsModal'
import type { StudentDto } from '../types'

export function StudentsPage() {
  const { stages, loading: stagesLoading, error: stagesError } = useStages()
  const { displayName } = useCurators()
  const { push } = useToast()

  const [students, setStudents] = useState<StudentDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState('')
  const [onlyStuck, setOnlyStuck] = useState(false)
  const [selectedCuratorIds, setSelectedCuratorIds] = useState<Set<string>>(new Set())
  const [openStudentId, setOpenStudentId] = useState<string | null>(null)

  const refetch = useCallback(() => {
    setLoading(true)
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

  async function moveStage(studentId: string, stageId: string) {
    const student = students.find((s) => s.id === studentId)
    if (!student || student.currentStageId === stageId) return
    try {
      await studentsApi.moveStage(studentId, stageId)
      refetch()
    } catch (err) {
      push('error', apiErrorMessage(err))
      refetch()
    }
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
      />
      {visible.length === 0 ? (
        <EmptyState />
      ) : (
        <KanbanBoard
          stages={stages}
          students={visible}
          curatorName={displayName}
          onOpenStudent={setOpenStudentId}
          onMoveStage={moveStage}
        />
      )}
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
