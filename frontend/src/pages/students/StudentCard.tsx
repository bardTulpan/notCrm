import { forwardRef } from 'react'
import { Avatar } from '../../components/Avatar'
import { TimeBadge } from '../../components/TimeBadge'
import { NormMeter } from '../../components/NormMeter'
import type { StudentDto } from '../../types'

export const StudentCard = forwardRef<
  HTMLDivElement,
  {
    student: StudentDto
    normDays: number | null
    curatorName: string
    curatorColor: string | null
    showNormMeter?: boolean
    onClick: () => void
    onPointerDown: (e: React.PointerEvent) => void
    isDragSource?: boolean
  }
>(function StudentCard({ student, normDays, curatorName, curatorColor, showNormMeter, onClick, onPointerDown, isDragSource }, ref) {
  const stuck = student.health === 'red'
  const meterNorm = showNormMeter && !student.isPaused && normDays != null && normDays > 0 ? normDays : null
  return (
    <div
      ref={ref}
      className={`card p-2.5 cursor-grab select-none touch-none ${stuck ? 'border-warn bg-warn-soft' : ''} ${
        student.isPaused ? 'border-dashed opacity-40 grayscale bg-pause-soft' : ''
      } ${isDragSource ? 'opacity-30' : ''}`}
      onPointerDown={onPointerDown}
      onClick={onClick}
    >
      <div className="flex items-center justify-between gap-1.5 mb-1.5">
        <span className="font-display font-semibold text-[13px]">{student.fullName}</span>
        <Avatar name={curatorName} color={curatorColor} />
      </div>
      {meterNorm != null ? (
        <NormMeter daysOnStage={student.daysOnStage} normDays={meterNorm} />
      ) : (
        <TimeBadge daysOnStage={student.daysOnStage} normDays={normDays} isPaused={student.isPaused} />
      )}
    </div>
  )
})
