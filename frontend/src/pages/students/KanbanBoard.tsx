import { Fragment, useCallback, useEffect, useRef, useState } from 'react'
import { StudentCard } from './StudentCard'
import { cardZone, compareCards } from './cardZone'
import type { StageDto, StudentDto } from '../../types'

const DRAG_THRESHOLD_PX = 6

interface DropTarget {
  stageId: string
  beforeStudentId: string | null
}

function sameTarget(a: DropTarget | null, b: DropTarget | null) {
  if (a === b) return true
  if (!a || !b) return false
  return a.stageId === b.stageId && a.beforeStudentId === b.beforeStudentId
}

export function KanbanBoard({
  stages,
  students,
  curatorName,
  onOpenStudent,
  onReorder,
}: {
  stages: StageDto[]
  students: StudentDto[]
  curatorName: (curatorId: string) => { name: string; color: string | null }
  onOpenStudent: (id: string) => void
  onReorder: (studentId: string, stageId: string, beforeStudentId: string | null) => void
}) {
  const [dragStudentId, setDragStudentId] = useState<string | null>(null)
  const [dropTarget, setDropTarget] = useState<DropTarget | null>(null)
  const dropTargetRef = useRef<DropTarget | null>(null)
  const wasDragRef = useRef(false)

  const cardRefs = useRef(new Map<string, HTMLDivElement>())
  const columnRefs = useRef(new Map<string, HTMLDivElement>())
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

  const studentsByStage = useCallback(
    (stageId: string) => students.filter((s) => s.currentStageId === stageId).sort(compareCards),
    [students],
  )

  const computeDropTarget = useCallback(
    (clientX: number, clientY: number, draggedStudent: StudentDto): DropTarget | null => {
      let bestStageId: string | null = null
      let bestDist = Infinity
      for (const [stageId, el] of columnRefs.current) {
        const rect = el.getBoundingClientRect()
        const dist = clientX < rect.left ? rect.left - clientX : clientX > rect.right ? clientX - rect.right : 0
        if (dist < bestDist) {
          bestDist = dist
          bestStageId = stageId
        }
      }
      if (!bestStageId) return null

      const zone = cardZone(draggedStudent)
      const zoneItems = studentsByStage(bestStageId).filter((s) => s.id !== draggedStudent.id && cardZone(s) === zone)

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
    [studentsByStage],
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

  const draggedStudent = dragStudentId ? students.find((s) => s.id === dragStudentId) : null

  return (
    <div className="flex gap-3.5 overflow-x-auto pb-2">
      {stages.map((stage, idx) => {
        const items = studentsByStage(stage.id)
        const showPlaceholderInThisColumn = dragStudentId && dropTarget?.stageId === stage.id

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
              ref={(el) => {
                if (el) columnRefs.current.set(stage.id, el)
                else columnRefs.current.delete(stage.id)
              }}
              className={`min-h-[60px] max-h-[62vh] overflow-y-auto flex flex-col gap-2 rounded-lg p-0.5 ${
                showPlaceholderInThisColumn ? 'bg-accent-soft/40' : ''
              }`}
            >
              {items.map((s) => (
                <Fragment key={s.id}>
                  {showPlaceholderInThisColumn && dropTarget?.beforeStudentId === s.id && (
                    <div className="rounded-lg border-2 border-dashed border-accent h-[52px]" />
                  )}
                  <StudentCard
                    key={s.id}
                    student={s}
                    normDays={stage.normDays}
                    curatorName={curatorName(s.curatorId).name}
                    curatorColor={curatorName(s.curatorId).color}
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
              {showPlaceholderInThisColumn && dropTarget?.beforeStudentId === null && (
                <div className="rounded-lg border-2 border-dashed border-accent h-[52px]" />
              )}
            </div>
          </div>
        )
      })}

      {draggedStudent && (
        <div
          ref={ghostRef}
          className="fixed top-0 left-0 pointer-events-none z-[200] card p-2.5 shadow-xl bg-white"
          style={{ width: pointerStateRef.current?.width ?? 200 }}
        >
          <span className="font-display font-semibold text-[13px]">{draggedStudent.fullName}</span>
        </div>
      )}
    </div>
  )
}
