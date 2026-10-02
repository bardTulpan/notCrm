import type { CohortDto } from '../types'

const MONTHS_RU = ['Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь', 'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь']

/** "YYYY-MM-DD" (date input value or cohort startDate) → "YYYY-MM". Plain string work, so no timezone drift. */
function monthKey(date: string): string {
  return date.slice(0, 7)
}

/** The existing cohort for the calendar month of the given date-input value, or '' if that month has none yet. */
export function pickCohortIdForDate(dateInputValue: string, cohorts: CohortDto[]): string {
  if (!dateInputValue) return ''
  const key = monthKey(dateInputValue)
  return cohorts.find((c) => !c.archivedAt && monthKey(c.startDate) === key)?.id ?? ''
}

/** Name the backend gives an auto-created cohort for this month, e.g. "Январь 2026". */
export function cohortNameForDate(dateInputValue: string): string {
  if (!dateInputValue) return ''
  const [year, month] = dateInputValue.split('-')
  return `${MONTHS_RU[Number(month) - 1] ?? ''} ${year}`.trim()
}
