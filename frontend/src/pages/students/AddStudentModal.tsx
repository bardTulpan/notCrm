import { useState } from 'react'
import { Modal } from '../../components/Modal'
import { useAuth } from '../../auth/useAuth'
import { useCurators } from '../../hooks/useCurators'
import { useCohorts } from '../../hooks/useCohorts'
import { useToast, apiErrorMessage } from '../../hooks/useToast'
import { studentsApi } from '../../api/students'
import { toDateInputValue } from '../../utils/dates'
import { cohortNameForDate, pickCohortIdForDate } from '../../utils/cohorts'
import type { StageDto } from '../../types'

/**
 * Direct student creation (no lead). Deliberately has no price field — pricing lives on leads only.
 * Curators always create for themselves; admin picks the curator.
 */
export function AddStudentModal({
  stages,
  onClose,
  onCreated,
}: {
  stages: StageDto[]
  onClose: () => void
  onCreated: () => void
}) {
  const { user } = useAuth()
  const { curators, loading: curatorsLoading } = useCurators()
  const { cohorts } = useCohorts()
  const { push } = useToast()
  const isAdmin = user?.role === 'ADMIN'

  const activeStages = [...stages].filter((s) => s.isActive).sort((a, b) => a.position - b.position)
  const today = toDateInputValue(new Date().toISOString())

  const [fullName, setFullName] = useState('')
  const [stageId, setStageId] = useState(activeStages[0]?.id ?? '')
  const [curatorId, setCuratorId] = useState('')
  const [startedAt, setStartedAt] = useState(today)
  const [telegramUsername, setTelegramUsername] = useState('')
  const [postpayPercent, setPostpayPercent] = useState('70')
  const [submitting, setSubmitting] = useState(false)

  const cohortId = pickCohortIdForDate(startedAt, cohorts)

  async function onSubmit() {
    if (!fullName.trim()) return
    if (isAdmin && !curatorId) {
      push('error', 'Выберите куратора')
      return
    }
    setSubmitting(true)
    try {
      await studentsApi.create({
        fullName: fullName.trim(),
        telegramUsername: telegramUsername.trim() || undefined,
        currentStageId: stageId,
        curatorId: isAdmin ? curatorId : undefined,
        cohortId: cohortId || undefined,
        startedAt: startedAt ? new Date(startedAt).toISOString() : undefined,
        postpayPercent: postpayPercent ? Number(postpayPercent) : undefined,
      })
      push('success', `${fullName.trim()} добавлен`)
      onCreated()
      onClose()
    } catch (err) {
      push('error', apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal title="Новый ученик" onClose={onClose}>
      <div className="flex flex-col gap-3">
        <label className="text-xs font-semibold text-ink-600">
          Имя
          <input className="input w-full mt-1" value={fullName} onChange={(e) => setFullName(e.target.value)} autoFocus />
        </label>
        <label className="text-xs font-semibold text-ink-600">
          Telegram username
          <input
            className="input w-full mt-1"
            value={telegramUsername}
            onChange={(e) => setTelegramUsername(e.target.value)}
            placeholder="@username"
          />
        </label>
        <label className="text-xs font-semibold text-ink-600">
          Этап
          <select className="input w-full mt-1" value={stageId} onChange={(e) => setStageId(e.target.value)}>
            {activeStages.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        </label>
        {isAdmin && (
          <label className="text-xs font-semibold text-ink-600">
            Куратор
            <select className="input w-full mt-1" value={curatorId} onChange={(e) => setCuratorId(e.target.value)} disabled={curatorsLoading}>
              <option value="">— выберите —</option>
              {curators.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.fullName}
                </option>
              ))}
            </select>
          </label>
        )}
        <div className="flex gap-3">
          <label className="text-xs font-semibold text-ink-600 flex-1">
            Дата старта
            <input className="input w-full mt-1" type="date" value={startedAt} onChange={(e) => setStartedAt(e.target.value)} />
          </label>
          <label className="text-xs font-semibold text-ink-600 flex-1">
            Постоплата, %
            <input
              className="input w-full mt-1"
              type="number"
              min={0}
              max={100}
              value={postpayPercent}
              onChange={(e) => setPostpayPercent(e.target.value)}
            />
          </label>
        </div>
        {startedAt && (
          <div className="text-xs text-ink-600">
            Когорта: {cohortId ? cohorts.find((c) => c.id === cohortId)?.name : `«${cohortNameForDate(startedAt)}» (будет создана)`}
          </div>
        )}
        <div className="flex justify-end gap-2 mt-2">
          <button className="btn-ghost" onClick={onClose}>
            Отмена
          </button>
          <button className="btn-primary" onClick={onSubmit} disabled={submitting || !fullName.trim() || !stageId}>
            Добавить
          </button>
        </div>
      </div>
    </Modal>
  )
}
