import { useState } from 'react'
import { Modal } from '../../components/Modal'
import { useAuth } from '../../auth/useAuth'
import { useCurators } from '../../hooks/useCurators'
import { useCohorts } from '../../hooks/useCohorts'
import { useToast, apiErrorMessage } from '../../hooks/useToast'
import { leadsApi } from '../../api/leads'
import { toDateInputValue } from '../../utils/dates'
import { pickCohortIdForDate } from '../../utils/cohorts'
import type { LeadDto } from '../../types'

export function ConvertLeadModal({ lead, onClose, onConverted }: { lead: LeadDto; onClose: () => void; onConverted: () => void }) {
  const { user } = useAuth()
  const { curators, loading: curatorsLoading } = useCurators()
  const { cohorts } = useCohorts()
  const { push } = useToast()
  const isAdmin = user?.role === 'ADMIN'

  const [curatorId, setCuratorId] = useState('')
  const [postpayPercent, setPostpayPercent] = useState<string>(lead.postpayPercent?.toString() ?? '70')
  const [startedAt, setStartedAt] = useState(toDateInputValue(new Date().toISOString()))
  const [submitting, setSubmitting] = useState(false)

  // Cohort isn't a field the curator picks here — it's derived from the start date
  // (for cohort-funnel reporting) and can be corrected later from the student's card if needed.
  const cohortId = pickCohortIdForDate(startedAt, cohorts)

  async function onSubmit() {
    if (isAdmin && !curatorId) {
      push('error', 'Выберите куратора')
      return
    }
    setSubmitting(true)
    try {
      await leadsApi.convert(lead.id, {
        curatorId: isAdmin ? curatorId : undefined,
        cohortId: cohortId || undefined,
        postpayPercent: postpayPercent ? Number(postpayPercent) : undefined,
        startedAt: startedAt ? new Date(startedAt).toISOString() : undefined,
      })
      push('success', `${lead.name} переведён в ученики`)
      onConverted()
      onClose()
    } catch (err) {
      push('error', apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal title="Перевести в ученики" subtitle={`${lead.name} · ${lead.telegramUsername ?? ''}`} onClose={onClose}>
      <div className="flex flex-col gap-3">
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
          <label className="text-xs font-semibold text-ink-600 flex-1">
            Дата старта
            <input className="input w-full mt-1" type="date" value={startedAt} onChange={(e) => setStartedAt(e.target.value)} />
          </label>
        </div>
        <div className="flex justify-end gap-2 mt-2">
          <button className="btn-ghost" onClick={onClose}>
            Отмена
          </button>
          <button className="btn-success" onClick={onSubmit} disabled={submitting}>
            Оплатил →
          </button>
        </div>
      </div>
    </Modal>
  )
}
