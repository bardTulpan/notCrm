import { useEffect, useMemo, useState } from 'react'
import { auditApi } from '../../api/audit'
import { Loader } from '../../components/Loader'
import { ErrorState } from '../../components/ErrorState'
import { apiErrorMessage } from '../../hooks/useToast'
import { studentsApi } from '../../api/students'
import type { AuditPage, StudentDto, UserDto } from '../../types'

const PAGE_SIZE = 50

const ENTITY_FILTERS: { value: string; label: string }[] = [
  { value: '', label: 'Всё' },
  { value: 'student', label: 'Ученики' },
  { value: 'lead', label: 'Лиды' },
  { value: 'user', label: 'Пользователи' },
  { value: 'cohort', label: 'Когорты' },
  { value: 'pipelineStage', label: 'Этапы' },
]

function toLocalDate(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function formatDateTime(value: string): string {
  return new Date(value).toLocaleString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function AuditLog({ users }: { users: UserDto[] }) {
  const [entityType, setEntityType] = useState('')
  const [actorId, setActorId] = useState('')
  const [page, setPage] = useState(0)
  const [students, setStudents] = useState<StudentDto[]>([])
  const [studentText, setStudentText] = useState('')
  const [studentId, setStudentId] = useState('')
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')
  const [data, setData] = useState<AuditPage | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    studentsApi.list().then(setStudents).catch(() => undefined)
  }, [])

  // Option labels for the student picker; duplicate names get the curator appended so each label is unique.
  const studentOptions = useMemo(() => {
    const sorted = [...students].sort((a, b) => a.fullName.localeCompare(b.fullName, 'ru'))
    const counts = new Map<string, number>()
    for (const s of sorted) counts.set(s.fullName, (counts.get(s.fullName) ?? 0) + 1)
    const curatorName = (id: string) => users.find((u) => u.id === id)?.fullName ?? ''
    return sorted.map((s) => ({
      id: s.id,
      label: (counts.get(s.fullName) ?? 0) > 1 ? `${s.fullName} (${curatorName(s.curatorId)})` : s.fullName,
    }))
  }, [students, users])

  function onStudentTextChange(text: string) {
    setStudentText(text)
    const match = studentOptions.find((o) => o.label === text)
    setStudentId(match?.id ?? '')
    setPage(0)
  }

  function setPreset(days: number | null) {
    if (days === null) {
      setFromDate('')
      setToDate('')
    } else {
      const to = new Date()
      const from = new Date()
      from.setDate(from.getDate() - (days - 1))
      setFromDate(toLocalDate(from))
      setToDate(toLocalDate(to))
    }
    setPage(0)
  }

  useEffect(() => {
    let cancelled = false
    auditApi
      .list({
        entityType: entityType || undefined,
        actorId: actorId || undefined,
        entityId: studentId || undefined,
        // Local-day boundaries; "to" is sent exclusive, i.e. the start of the day after the chosen one.
        from: fromDate ? new Date(`${fromDate}T00:00:00`).toISOString() : undefined,
        to: toDate ? new Date(new Date(`${toDate}T00:00:00`).getTime() + 86_400_000).toISOString() : undefined,
        page,
        size: PAGE_SIZE,
      })
      .then((d) => {
        if (!cancelled) {
          setError(null)
          setData(d)
        }
      })
      .catch((e) => {
        if (!cancelled) setError(apiErrorMessage(e))
      })
    return () => {
      cancelled = true
    }
  }, [entityType, actorId, studentId, fromDate, toDate, page])

  const totalPages = data ? Math.max(1, Math.ceil(data.total / PAGE_SIZE)) : 1

  return (
    <div>
      <div className="flex items-center gap-2.5 flex-wrap mb-4">
        {ENTITY_FILTERS.map((f) => (
          <button
            key={f.value}
            className={`chip ${entityType === f.value ? 'active' : ''}`}
            onClick={() => {
              setEntityType(f.value)
              setPage(0)
            }}
          >
            {f.label}
          </button>
        ))}
        <select
          className="input ml-auto"
          value={actorId}
          onChange={(e) => {
            setActorId(e.target.value)
            setPage(0)
          }}
        >
          <option value="">Все пользователи</option>
          {users.map((u) => (
            <option key={u.id} value={u.id}>
              {u.fullName}
            </option>
          ))}
        </select>
      </div>

      <div className="flex items-center gap-2.5 flex-wrap mb-4">
        <input
          className="input w-64"
          list="audit-students"
          placeholder="Ученик (начните вводить имя)"
          value={studentText}
          onChange={(e) => onStudentTextChange(e.target.value)}
        />
        <datalist id="audit-students">
          {studentOptions.map((o) => (
            <option key={o.id} value={o.label} />
          ))}
        </datalist>
        <span className="text-xs text-ink-400 ml-2">Период:</span>
        <input
          className="input"
          type="date"
          value={fromDate}
          max={toDate || undefined}
          onChange={(e) => {
            setFromDate(e.target.value)
            setPage(0)
          }}
        />
        <span className="text-xs text-ink-400">—</span>
        <input
          className="input"
          type="date"
          value={toDate}
          min={fromDate || undefined}
          onChange={(e) => {
            setToDate(e.target.value)
            setPage(0)
          }}
        />
        <button className="chip" onClick={() => setPreset(1)}>Сегодня</button>
        <button className="chip" onClick={() => setPreset(7)}>7 дней</button>
        <button className="chip" onClick={() => setPreset(30)}>30 дней</button>
        {(fromDate || toDate || studentId || studentText) && (
          <button
            className="text-xs font-semibold text-ink-600 underline"
            onClick={() => {
              setPreset(null)
              setStudentText('')
              setStudentId('')
            }}
          >
            Сбросить
          </button>
        )}
      </div>

      {error && <ErrorState message={error} />}
      {!error && !data && <Loader />}
      {!error && data && (
        <>
          {data.items.length === 0 ? (
            <div className="text-sm text-ink-400">Записей пока нет</div>
          ) : (
            <div className="card divide-y divide-border">
              {data.items.map((e) => (
                <div key={e.id} className="flex items-baseline gap-3 px-4 py-2.5 text-[13px]">
                  <span className="font-mono text-xs text-ink-400 flex-shrink-0 w-[110px]">{formatDateTime(e.createdAt)}</span>
                  <span>
                    <span className="font-semibold">{e.actorName}</span> {e.description}
                  </span>
                </div>
              ))}
            </div>
          )}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-3 mt-4 text-xs text-ink-600">
              <button className="btn-ghost" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                ← Новее
              </button>
              <span>
                {page + 1} / {totalPages}
              </span>
              <button className="btn-ghost" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>
                Старее →
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
