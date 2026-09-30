import type { CohortDto } from '../types'

/** Picks the cohort whose start month is closest to the given date-input value ("YYYY-MM-DD"). */
export function pickCohortIdForDate(dateInputValue: string, cohorts: CohortDto[]): string {
  if (!dateInputValue) return ''
  const target = new Date(dateInputValue)
  const targetMonth = target.getFullYear() * 12 + target.getMonth()

  let best: CohortDto | null = null
  let bestDiff = Infinity
  for (const c of cohorts) {
    if (c.archivedAt) continue
    const cohortDate = new Date(c.startDate)
    const diff = Math.abs(cohortDate.getFullYear() * 12 + cohortDate.getMonth() - targetMonth)
    if (diff < bestDiff) {
      bestDiff = diff
      best = c
    }
  }
  return best?.id ?? ''
}
