import { useAvatarUrl } from '../hooks/useAvatarUrl'
import type { StudentDto } from '../types'

// Soft tint + dark letters, so a student's initials never read as the solid curator circle next to them.
const PALETTE: [string, string][] = [
  ['#e3ebfb', '#2d4c96'],
  ['#fbe5da', '#9a4421'],
  ['#e0f1e5', '#2b6a42'],
  ['#efe3f8', '#663c96'],
  ['#f8eed3', '#80601a'],
  ['#daeff1', '#1d636d'],
  ['#f9e0e7', '#93304d'],
  ['#e8ebdc', '#4d5928'],
  ['#e5e1f4', '#413788'],
  ['#f3e4d6', '#7a4829'],
]

/** Stable per student, so the same person keeps the same colour on every screen. */
function colorsFor(id: string): [string, string] {
  let h = 2166136261
  for (let i = 0; i < id.length; i++) {
    h ^= id.charCodeAt(i)
    h = Math.imul(h, 16777619)
  }
  return PALETTE[(h >>> 0) % PALETTE.length]
}

function initials(name: string): string {
  const words = name.trim().split(/\s+/)
  return ((words[0]?.[0] ?? '') + (words[1]?.[0] ?? '')).toUpperCase()
}

/** The student's Telegram photo, or their initials when there is none (no handle, hidden, not fetched yet). */
export function StudentAvatar({
  student,
  size,
}: {
  student: Pick<StudentDto, 'id' | 'fullName' | 'avatarVersion'>
  size: number
}) {
  const url = useAvatarUrl(student.id, student.avatarVersion)
  if (url) {
    return (
      <img
        src={url}
        alt=""
        draggable={false}
        className="rounded-full object-cover flex-shrink-0 bg-bg"
        style={{ width: size, height: size }}
      />
    )
  }
  const [bg, fg] = colorsFor(student.id)
  return (
    <span
      className="rounded-full flex items-center justify-center flex-shrink-0 font-display font-semibold select-none"
      style={{
        width: size,
        height: size,
        background: bg,
        color: fg,
        fontSize: Math.round(size * 0.37),
        boxShadow: `inset 0 0 0 1px ${fg}24`,
      }}
      aria-hidden="true"
    >
      {initials(student.fullName)}
    </span>
  )
}
