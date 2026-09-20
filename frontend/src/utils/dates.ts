export function formatDate(value: string | null | undefined): string {
  if (!value) return '—'
  return new Date(value).toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

export function formatDateShort(value: string | null | undefined): string {
  if (!value) return '—'
  return new Date(value).toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit' })
}

export function daysSince(value: string): number {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const t = new Date(value)
  t.setHours(0, 0, 0, 0)
  return Math.round((today.getTime() - t.getTime()) / 86_400_000)
}

/** Instant string for "N days from now", suitable for a `datetime-local` input default or an API payload. */
export function addDaysIso(n: number): string {
  const d = new Date()
  d.setDate(d.getDate() + n)
  return d.toISOString()
}

export function toDateInputValue(value: string | null | undefined): string {
  if (!value) return ''
  return new Date(value).toISOString().slice(0, 10)
}

export function toDateTimeInputValue(value: string | null | undefined): string {
  if (!value) return ''
  const d = new Date(value)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}
