import { Avatar } from '../../components/Avatar'
import { TimeBadge } from '../../components/TimeBadge'
import type { StudentDto } from '../../types'

export function StudentCard({
  student,
  normDays,
  curatorName,
  curatorColor,
  onClick,
  onDragStart,
}: {
  student: StudentDto
  normDays: number | null
  curatorName: string
  curatorColor: string | null
  onClick: () => void
  onDragStart: (e: React.DragEvent) => void
}) {
  const stuck = student.health === 'red'
  return (
    <div
      className={`card p-2.5 cursor-pointer ${stuck ? 'border-warn bg-warn-soft' : ''} ${
        student.isPaused ? 'border-dashed opacity-40 grayscale bg-pause-soft' : ''
      }`}
      draggable
      onDragStart={onDragStart}
      onClick={onClick}
    >
      <div className="flex items-center justify-between gap-1.5 mb-1.5">
        <span className="font-display font-semibold text-[13px]">{student.fullName}</span>
        <Avatar name={curatorName} color={curatorColor} />
      </div>
      <TimeBadge daysOnStage={student.daysOnStage} normDays={normDays} isPaused={student.isPaused} />
    </div>
  )
}
