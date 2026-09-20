import { useState } from 'react'
import { Modal } from '../../components/Modal'
import { useAuth } from '../../auth/useAuth'
import { useCurators } from '../../hooks/useCurators'
import { useToast, apiErrorMessage } from '../../hooks/useToast'
import { leadsApi } from '../../api/leads'
import { toDateTimeInputValue } from '../../utils/dates'
import type { LeadDto } from '../../types'

export function LeadFormModal({ lead, onClose, onSaved }: { lead: LeadDto | null; onClose: () => void; onSaved: () => void }) {
  const { user } = useAuth()
  const { curators } = useCurators()
  const { push } = useToast()
  const isEdit = !!lead

  const [name, setName] = useState(lead?.name ?? '')
  const [telegramUsername, setTelegramUsername] = useState(lead?.telegramUsername ?? '')
  const [priceDescription, setPriceDescription] = useState(lead?.priceDescription ?? '')
  const [postpayPercent, setPostpayPercent] = useState<string>(lead?.postpayPercent?.toString() ?? '70')
  const [nextPingAt, setNextPingAt] = useState(toDateTimeInputValue(lead?.nextPingAt ?? new Date().toISOString()))
  const [curatorId, setCuratorId] = useState(lead?.assignedCuratorId ?? '')
  const [notes, setNotes] = useState<string[]>(lead?.notes.map((n) => n.text) ?? [''])
  const [submitting, setSubmitting] = useState(false)

  function updateNote(idx: number, value: string) {
    setNotes((prev) => prev.map((n, i) => (i === idx ? value : n)))
  }
  function removeNote(idx: number) {
    setNotes((prev) => prev.filter((_, i) => i !== idx))
  }

  async function onSubmit() {
    if (!name.trim()) return
    setSubmitting(true)
    try {
      const payload = {
        name: name.trim(),
        telegramUsername: telegramUsername.trim() || undefined,
        priceDescription: priceDescription.trim() || undefined,
        postpayPercent: postpayPercent ? Number(postpayPercent) : undefined,
        nextPingAt: nextPingAt ? new Date(nextPingAt).toISOString() : undefined,
        curatorId: user?.role === 'ADMIN' && curatorId ? curatorId : undefined,
        notes: notes.filter((n) => n.trim()).map((text, position) => ({ text: text.trim(), position })),
      }
      if (isEdit) {
        await leadsApi.update(lead.id, payload)
      } else {
        await leadsApi.create(payload)
      }
      push('success', isEdit ? 'Лид обновлён' : 'Лид создан')
      onSaved()
      onClose()
    } catch (err) {
      push('error', apiErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal title={isEdit ? 'Редактировать лида' : 'Новый лид'} onClose={onClose}>
      <div className="flex flex-col gap-3">
        <label className="text-xs font-semibold text-ink-600">
          Имя
          <input className="input w-full mt-1" value={name} onChange={(e) => setName(e.target.value)} required />
        </label>
        <label className="text-xs font-semibold text-ink-600">
          Telegram username
          <input
            className="input w-full mt-1"
            value={telegramUsername ?? ''}
            onChange={(e) => setTelegramUsername(e.target.value)}
            placeholder="@username"
          />
        </label>
        <label className="text-xs font-semibold text-ink-600">
          Цена
          <input
            className="input w-full mt-1"
            value={priceDescription ?? ''}
            onChange={(e) => setPriceDescription(e.target.value)}
            placeholder="65к"
          />
        </label>
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
            Следующий пинг
            <input
              className="input w-full mt-1"
              type="datetime-local"
              value={nextPingAt}
              onChange={(e) => setNextPingAt(e.target.value)}
            />
          </label>
        </div>
        {user?.role === 'ADMIN' && (
          <label className="text-xs font-semibold text-ink-600">
            Куратор
            <select className="input w-full mt-1" value={curatorId} onChange={(e) => setCuratorId(e.target.value)}>
              <option value="">— назначить себе/позже —</option>
              {curators.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.fullName}
                </option>
              ))}
            </select>
          </label>
        )}
        <div>
          <div className="text-xs font-semibold text-ink-600 mb-1">Заметки</div>
          <div className="flex flex-col gap-1.5">
            {notes.map((n, idx) => (
              <div key={idx} className="flex gap-1.5">
                <input className="input flex-1" value={n} onChange={(e) => updateNote(idx, e.target.value)} />
                <button type="button" className="btn-ghost px-2" onClick={() => removeNote(idx)}>
                  ✕
                </button>
              </div>
            ))}
          </div>
          <button type="button" className="btn-ghost mt-1.5" onClick={() => setNotes((prev) => [...prev, ''])}>
            + заметка
          </button>
        </div>
        <div className="flex justify-end gap-2 mt-2">
          <button className="btn-ghost" onClick={onClose}>
            Отмена
          </button>
          <button className="btn-primary" onClick={onSubmit} disabled={submitting || !name.trim()}>
            {isEdit ? 'Сохранить' : 'Создать'}
          </button>
        </div>
      </div>
    </Modal>
  )
}
