import { useEffect, useState } from 'react'
import { auditApi } from '../../api/audit'
import { Loader } from '../../components/Loader'
import { ErrorState } from '../../components/ErrorState'
import { apiErrorMessage } from '../../hooks/useToast'
import type { AuditPage, UserDto } from '../../types'

const PAGE_SIZE = 50

const ENTITY_FILTERS: { value: string; label: string }[] = [
  { value: '', label: 'Всё' },
  { value: 'student', label: 'Ученики' },
  { value: 'lead', label: 'Лиды' },
  { value: 'user', label: 'Пользователи' },
  { value: 'cohort', label: 'Когорты' },
  { value: 'pipelineStage', label: 'Этапы' },
]

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
  const [data, setData] = useState<AuditPage | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    auditApi
      .list({ entityType: entityType || undefined, actorId: actorId || undefined, page, size: PAGE_SIZE })
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
  }, [entityType, actorId, page])

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
