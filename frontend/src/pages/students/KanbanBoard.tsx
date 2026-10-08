import { Fragment, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { StudentCard } from './StudentCard'
import { StudentAvatar } from '../../components/StudentAvatar'
import { cardZone, compareCards } from './cardZone'
import type { CohortDto, StageDto, StudentDto } from '../../types'

const DRAG_THRESHOLD_PX = 6
const ALL_LANE = 'all'
const NO_COHORT_LANE = 'no-cohort'

interface DropTarget {
  stageId: string
  beforeStudentId: string | null
}

interface Lane {
  key: string
  name: string
  count: number
  overdue: number
}

function sameTarget(a: DropTarget | null, b: DropTarget | null) {
  if (a === b) return true
  if (!a || !b) return false
  return a.stageId === b.stageId && a.beforeStudentId === b.beforeStudentId
}

export function KanbanBoard({
  stages,
  students,
  cohorts,
  groupByCohort,
  showNormMeter,
  curatorName,
  onOpenStudent,
  onReorder,
}: {
  stages: StageDto[]
  students: StudentDto[]
  cohorts: CohortDto[]
  groupByCohort: boolean
  showNormMeter: boolean
  curatorName: (curatorId: string) => { name: string; color: string | null }
  onOpenStudent: (id: string) => void
  onReorder: (studentId: string, stageId: string, beforeStudentId: string | null) => void
}) {
  const [dragStudentId, setDragStudentId] = useState<string | null>(null)
  const [dropTarget, setDropTarget] = useState<DropTarget | null>(null)
  const [collapsedLanes, setCollapsedLanes] = useState<Set<string>>(new Set())
  const dropTargetRef = useRef<DropTarget | null>(null)
  const wasDragRef = useRef(false)

  const cardRefs = useRef(new Map<string, HTMLDivElement>())
  const cellRefs = useRef(new Map<string, { stageId: string; lane: string; el: HTMLDivElement }>())
  const ghostRef = useRef<HTMLDivElement>(null)

  const pointerStateRef = useRef<{
    studentId: string
    startX: number
    startY: number
    offsetX: number
    offsetY: number
    width: number
    dragging: boolean
  } | null>(null)

  const knownCohortIds = useMemo(() => new Set(cohorts.map((c) => c.id)), [cohorts])

  // Cohort follows the start date, so in lane mode a card can only move within its own cohort lane.
  const laneOf = useCallback(
    (s: StudentDto) => {
      if (!groupByCohort) return ALL_LANE
      return s.cohortId && knownCohortIds.has(s.cohortId) ? s.cohortId : NO_COHORT_LANE
    },
    [groupByCohort, knownCohortIds],
  )

  const lanes = useMemo<Lane[]>(() => {
    if (!groupByCohort) return []
    const stats = new Map<string, { count: number; overdue: number }>()
    for (const s of students) {
      const key = laneOf(s)
      const st = stats.get(key) ?? { count: 0, overdue: 0 }
      st.count++
      if (s.health === 'red' && !s.isPaused) st.overdue++
      stats.set(key, st)
    }
    const result: Lane[] = [...cohorts]
      .sort((a, b) => a.startDate.localeCompare(b.startDate))
      .filter((c) => stats.has(c.id))
      .map((c) => ({ key: c.id, name: c.archivedAt ? `${c.name} (архив)` : c.name, ...stats.get(c.id)! }))
    const none = stats.get(NO_COHORT_LANE)
    if (none) result.push({ key: NO_COHORT_LANE, name: 'Без когорты', ...none })
    return result
  }, [groupByCohort, students, cohorts, laneOf])

  const studentsByStage = useCallback(
    (stageId: string) => students.filter((s) => s.currentStageId === stageId).sort(compareCards),
    [students],
  )

  const computeDropTarget = useCallback(
    (clientX: number, clientY: number, draggedStudent: StudentDto): DropTarget | null => {
      const lane = laneOf(draggedStudent)
      let bestStageId: string | null = null
      let bestDist = Infinity
      for (const cell of cellRefs.current.values()) {
        if (cell.lane !== lane) continue
        const rect = cell.el.getBoundingClientRect()
        const dist = clientX < rect.left ? rect.left - clientX : clientX > rect.right ? clientX - rect.right : 0
        if (dist < bestDist) {
          bestDist = dist
          bestStageId = cell.stageId
        }
      }
      if (!bestStageId) return null

      const zone = cardZone(draggedStudent)
      const zoneItems = studentsByStage(bestStageId).filter(
        (s) => s.id !== draggedStudent.id && cardZone(s) === zone && laneOf(s) === lane,
      )

      let beforeId: string | null = null
      for (const item of zoneItems) {
        const el = cardRefs.current.get(item.id)
        if (!el) continue
        const rect = el.getBoundingClientRect()
        const midY = rect.top + rect.height / 2
        if (clientY < midY) {
          beforeId = item.id
          break
        }
      }
      return { stageId: bestStageId, beforeStudentId: beforeId }
    },
    [studentsByStage, laneOf],
  )

  const endDrag = useCallback((commit: boolean) => {
    const target = dropTargetRef.current
    const state = pointerStateRef.current
    if (commit && state?.dragging && target) {
      onReorder(state.studentId, target.stageId, target.beforeStudentId)
    }
    pointerStateRef.current = null
    dropTargetRef.current = null
    setDragStudentId(null)
    setDropTarget(null)
    document.body.style.cursor = ''
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [onReorder])

  useEffect(() => {
    function onMove(e: PointerEvent) {
      const state = pointerStateRef.current
      if (!state) return
      const dx = e.clientX - state.startX
      const dy = e.clientY - state.startY

      if (!state.dragging) {
        if (Math.hypot(dx, dy) < DRAG_THRESHOLD_PX) return
        state.dragging = true
        wasDragRef.current = true
        setDragStudentId(state.studentId)
        document.body.style.cursor = 'grabbing'
      }

      if (ghostRef.current) {
        ghostRef.current.style.transform = `translate(${e.clientX - state.offsetX}px, ${e.clientY - state.offsetY}px) rotate(-2deg)`
      }

      const draggedStudent = students.find((s) => s.id === state.studentId)
      if (draggedStudent) {
        const next = computeDropTarget(e.clientX, e.clientY, draggedStudent)
        if (!sameTarget(next, dropTargetRef.current)) {
          dropTargetRef.current = next
          setDropTarget(next)
        }
      }
    }

    function onUp() {
      endDrag(true)
    }

    function onKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape' && pointerStateRef.current?.dragging) {
        endDrag(false)
      }
    }

    window.addEventListener('pointermove', onMove)
    window.addEventListener('pointerup', onUp)
    window.addEventListener('keydown', onKeyDown)
    return () => {
      window.removeEventListener('pointermove', onMove)
      window.removeEventListener('pointerup', onUp)
      window.removeEventListener('keydown', onKeyDown)
    }
  }, [students, computeDropTarget, endDrag])

  function handlePointerDown(student: StudentDto, e: React.PointerEvent) {
    if (e.button !== 0) return
    const el = cardRefs.current.get(student.id)
    const rect = el?.getBoundingClientRect()
    wasDragRef.current = false
    pointerStateRef.current = {
      studentId: student.id,
      startX: e.clientX,
      startY: e.clientY,
      offsetX: e.clientX - (rect?.left ?? e.clientX),
      offsetY: e.clientY - (rect?.top ?? e.clientY),
      width: rect?.width ?? 200,
      dragging: false,
    }
  }

  function toggleLane(key: string) {
    setCollapsedLanes((prev) => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })
  }

  const draggedStudent = dragStudentId ? students.find((s) => s.id === dragStudentId) : null
  const draggedLane = draggedStudent ? laneOf(draggedStudent) : null

  function renderStageHeader(stage: StageDto, idx: number, count: number) {
    return (
      <>
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
            {count}
          </span>
        </div>
        <div className="font-mono text-[10.5px] text-ink-400 mb-2.5">
          {stage.normDays != null ? `норма · ${stage.normDays} дн.` : 'финальный шаг'}
        </div>
      </>
    )
  }

  function renderCell(stage: StageDto, items: StudentDto[], lane: string, className: string) {
    const showPlaceholder = dragStudentId != null && dropTarget?.stageId === stage.id && draggedLane === lane
    const cellKey = `${lane}|${stage.id}`
    return (
      <div
        ref={(el) => {
          if (el) cellRefs.current.set(cellKey, { stageId: stage.id, lane, el })
          else cellRefs.current.delete(cellKey)
        }}
        className={`flex flex-col gap-2 rounded-lg p-0.5 ${className} ${showPlaceholder ? 'bg-accent-soft/40' : ''}`}
      >
        {items.map((s) => (
          <Fragment key={s.id}>
            {showPlaceholder && dropTarget?.beforeStudentId === s.id && (
              <div className="rounded-lg border-2 border-dashed border-accent h-[52px]" />
            )}
            <StudentCard
              student={s}
              normDays={stage.normDays}
              curatorName={curatorName(s.curatorId).name}
              curatorColor={curatorName(s.curatorId).color}
              showNormMeter={showNormMeter}
              onClick={() => {
                if (!wasDragRef.current) onOpenStudent(s.id)
              }}
              onPointerDown={(e) => handlePointerDown(s, e)}
              isDragSource={s.id === dragStudentId}
              ref={(el) => {
                if (el) cardRefs.current.set(s.id, el)
                else cardRefs.current.delete(s.id)
              }}
            />
          </Fragment>
        ))}
        {showPlaceholder && dropTarget?.beforeStudentId === null && (
          <div className="rounded-lg border-2 border-dashed border-accent h-[52px]" />
        )}
      </div>
    )
  }

  const ghost = draggedStudent && (
    <div
      ref={ghostRef}
      className="fixed top-0 left-0 pointer-events-none z-[200] card p-2.5 shadow-xl bg-white"
      style={{ width: pointerStateRef.current?.width ?? 200 }}
    >
      <div className="flex items-center gap-2.5">
        <StudentAvatar student={draggedStudent} size={30} />
        <span className="font-display font-semibold text-[13px]">{draggedStudent.fullName}</span>
      </div>
    </div>
  )

  if (!groupByCohort) {
    return (
      <div className="flex gap-3.5 overflow-x-auto pb-2">
        {stages.map((stage, idx) => {
          const items = studentsByStage(stage.id)
          return (
            <div key={stage.id} className="flex-none w-[220px]">
              {renderStageHeader(stage, idx, items.length)}
              {renderCell(stage, items, ALL_LANE, 'min-h-[60px] max-h-[62vh] overflow-y-auto')}
            </div>
          )
        })}
        {ghost}
      </div>
    )
  }

  return (
    <div className="overflow-x-auto pb-2">
      <div className="grid gap-x-3.5 w-max" style={{ gridTemplateColumns: `repeat(${stages.length}, 220px)` }}>
        {stages.map((stage, idx) => (
          <div key={stage.id}>{renderStageHeader(stage, idx, studentsByStage(stage.id).length)}</div>
        ))}
        {lanes.map((lane, i) => {
          const collapsed = collapsedLanes.has(lane.key)
          return (
            <Fragment key={lane.key}>
              <div className={`col-span-full ${i > 0 ? 'border-t border-border mt-2 pt-2' : ''}`}>
                <button
                  type="button"
                  className="sticky left-0 flex items-center gap-2 py-1 pr-2 bg-transparent border-0 font-display font-semibold text-[13px] text-ink-900 cursor-pointer"
                  onClick={() => toggleLane(lane.key)}
                  aria-expanded={!collapsed}
                >
                  <span className="text-[10px] text-ink-400 w-2.5">{collapsed ? '▸' : '▾'}</span>
                  {lane.name}
                  <span className="font-mono text-[11px] font-normal text-ink-400">
                    {lane.count} уч.
                    {lane.overdue > 0 && <span className="text-warn font-semibold"> · {lane.overdue} просроч.</span>}
                  </span>
                </button>
              </div>
              {!collapsed &&
                stages.map((stage) => (
                  <div key={stage.id} className="pb-1.5 flex flex-col">
                    {renderCell(
                      stage,
                      studentsByStage(stage.id).filter((s) => laneOf(s) === lane.key),
                      lane.key,
                      'flex-1 min-h-[44px]',
                    )}
                  </div>
                ))}
            </Fragment>
          )
        })}
      </div>
      {ghost}
    </div>
  )
}
