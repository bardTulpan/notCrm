import type { StudentDto } from '../../types'

export type CardZone = 'red' | 'normal' | 'paused'

export function cardZone(s: StudentDto): CardZone {
  if (s.isPaused) return 'paused'
  if (s.health === 'red') return 'red'
  return 'normal'
}

export const ZONE_ORDER: Record<CardZone, number> = { red: 0, normal: 1, paused: 2 }

export function compareCards(a: StudentDto, b: StudentDto): number {
  const za = ZONE_ORDER[cardZone(a)]
  const zb = ZONE_ORDER[cardZone(b)]
  if (za !== zb) return za - zb
  if (a.stagePosition !== b.stagePosition) return a.stagePosition - b.stagePosition
  return a.fullName.localeCompare(b.fullName)
}
