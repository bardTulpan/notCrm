import { useState } from 'react'
import { Modal } from '../../components/Modal'
import { Avatar } from '../../components/Avatar'
import { useAuth } from '../../auth/useAuth'
import { useCurators } from '../../hooks/useCurators'
import { useCohorts } from '../../hooks/useCohorts'
import { useToast, apiErrorMessage } from '../../hooks/useToast'
import { studentsApi } from '../../api/students'
import { formatDate, toDateInputValue } from '../../utils/dates'
import { StudentComments } from './StudentComments'
import { StudentHistory } from './StudentHistory'
import type { StageDto, StudentDto } from '../../types'

function plannedCompletionLabel(student: StudentDto, stages: StageDto[]): string {
  if (student.isPaused) return 'на паузе'
  const idx = stages.findIndex((s) => s.id === student.currentStageId)
  if (idx === -1) return '—'
  let remaining = 0
  const norm = stages[idx].normDays
  if (norm != null) remaining += Math.max(norm - student.daysOnStage, 0)
  for (let i = idx + 1; i < stages.length; i++) {
    if (stages[i].normDays != null) remaining += stages[i].normDays!
  }
  const d = new Date()
  d.setDate(d.getDate() + remaining)
  return formatDate(d.toISOString())
}

export function StudentDetailsModal({
  student,
  stages,
  onClose,
  onChanged,
}: {
  student: StudentDto
  stages: StageDto[]
  onClose: () => void
  onChanged: () => void
}) {
  const { user } = useAuth()
  const { curators, displayName } = useCurators()
  const { cohorts } = useCohorts()
  const { push } = useToast()
  const isAdmin = user?.role === 'ADMIN'
  const [busy, setBusy] = useState(false)
  const [startedAt, setStartedAt] = useState(toDateInputValue(student.startedAt))

  const [editing, setEditing] = useState(false)
  const [draftName, setDraftName] = useState(student.fullName)
  const [draftPostpay, setDraftPostpay] = useState(student.postpayPercent?.toString() ?? '')
  const [draftNotes, setDraftNotes] = useState<string[]>(
    student.notes.length ? student.notes.map((n) => n.text) : [''],
  )

  const cur = displayName(student.curatorId)

  async function togglePause() {
    setBusy(true)
    try {
      if (student.isPaused) await studentsApi.resume(student.id)
      else await studentsApi.pause(student.id)
      onChanged()
    } catch (err) {
      push('error', apiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  async function reassign(newCuratorId: string) {
    setBusy(true)
    try {
      await studentsApi.assignCurator(student.id, newCuratorId)
      push('success', 'Куратор изменён')
      onChanged()
    } catch (err) {
      push('error', apiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  async function reassignCohort(newCohortId: string) {
    setBusy(true)
    try {
      await studentsApi.update(student.id, { cohortId: newCohortId })
      push('success', 'Когорта изменена')
      onChanged()
    } catch (err) {
      push('error', apiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  async function saveStartedAt() {
    if (!startedAt) return
    const iso = new Date(startedAt).toISOString()
    if (iso === new Date(student.startedAt).toISOString()) return
    setBusy(true)
    try {
      await studentsApi.update(student.id, { startedAt: iso })
      push('success', 'Дата начала обновлена')
      onChanged()
    } catch (err) {
      push('error', apiErrorMessage(err))
      setStartedAt(toDateInputValue(student.startedAt))
    } finally {
      setBusy(false)
    }
  }

  function startEditing() {
    setDraftName(student.fullName)
    setDraftPostpay(student.postpayPercent?.toString() ?? '')
    setDraftNotes(student.notes.length ? student.notes.map((n) => n.text) : [''])
    setEditing(true)
  }

  function updateNote(idx: number, value: string) {
    setDraftNotes((prev) => prev.map((n, i) => (i === idx ? value : n)))
  }
  function removeNote(idx: number) {
    setDraftNotes((prev) => prev.filter((_, i) => i !== idx))
  }

  async function saveEdits() {
    if (!draftName.trim()) return
    setBusy(true)
    try {
      await studentsApi.update(student.id, {
        fullName: draftName.trim(),
        postpayPercent: draftPostpay ? Number(draftPostpay) : undefined,
        notes: draftNotes.filter((n) => n.trim()).map((text, position) => ({ text: text.trim(), position })),
      })
      push('success', 'Данные ученика обновлены')
      setEditing(false)
      onChanged()
    } catch (err) {
      push('error', apiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal
      title={
        editing ? (
          <input
            className="input font-display font-bold text-lg w-full"
            value={draftName}
            onChange={(e) => setDraftName(e.target.value)}
          />
        ) : (
          student.fullName
        )
      }
      subtitle={
        editing ? (
          <label className="text-xs font-semibold text-ink-600 flex items-center gap-2">
            постоплата, %
            <input
              className="input w-20"
              type="number"
              min={0}
              max={100}
              value={draftPostpay}
              onChange={(e) => setDraftPostpay(e.target.value)}
            />
          </label>
        ) : (
          <>
            <span className="font-mono text-xs bg-success-soft text-success px-2 py-0.5 rounded-md font-semibold">
              постоплата · {student.postpayPercent ?? '—'}%
            </span>
            <span className="font-mono text-xs bg-accent-soft text-accent px-2 py-0.5 rounded-md font-semibold">
              план завершения · {plannedCompletionLabel(student, stages)}
            </span>
          </>
        )
      }
      onClose={onClose}
    >
      <div className="text-xs font-semibold text-ink-600 uppercase tracking-wide mt-4 mb-2">Куратор</div>
      <div className="flex items-center gap-2 mb-3">
        <Avatar name={cur.name} color={cur.color} />
        <select
          className="input"
          value={student.curatorId}
          disabled={!isAdmin || busy}
          onChange={(e) => reassign(e.target.value)}
        >
          {!curators.find((c) => c.id === student.curatorId) && <option value={student.curatorId}>{cur.name}</option>}
          {curators.map((c) => (
            <option key={c.id} value={c.id}>
              {c.fullName}
            </option>
          ))}
        </select>
      </div>

      <div className="flex gap-3 mb-3">
        <label className="text-xs font-semibold text-ink-600 flex-1">
          Когорта
          <select
            className="input w-full mt-1"
            value={student.cohortId ?? ''}
            disabled={!isAdmin || busy}
            onChange={(e) => reassignCohort(e.target.value)}
          >
            {!student.cohortId && <option value="">— без когорты —</option>}
            {cohorts.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </label>
        <label className="text-xs font-semibold text-ink-600 flex-1">
          Дата начала
          <input
            className="input w-full mt-1"
            type="date"
            value={startedAt}
            disabled={busy}
            onChange={(e) => setStartedAt(e.target.value)}
            onBlur={saveStartedAt}
          />
        </label>
      </div>

      <button
        className={`btn-ghost w-full ${student.isPaused ? '!bg-pause-soft !text-pause !border-pause' : ''}`}
        disabled={busy}
        onClick={togglePause}
      >
        {student.isPaused ? '▶ Снять с паузы' : '⏸ Поставить на паузу'}
      </button>

      <div className="flex items-center justify-between mt-4 mb-2">
        <div className="text-xs font-semibold text-ink-600 uppercase tracking-wide">Описание</div>
        {!editing && (
          <button className="text-xs font-semibold text-accent underline" onClick={startEditing}>
            Редактировать
          </button>
        )}
      </div>

      {editing ? (
        <div className="flex flex-col gap-1.5">
          {draftNotes.map((n, idx) => (
            <div key={idx} className="flex gap-1.5">
              <input className="input flex-1" value={n} onChange={(e) => updateNote(idx, e.target.value)} />
              <button type="button" className="btn-ghost px-2" onClick={() => removeNote(idx)}>
                ✕
              </button>
            </div>
          ))}
          <button type="button" className="btn-ghost self-start" onClick={() => setDraftNotes((prev) => [...prev, ''])}>
            + строка описания
          </button>
          <div className="flex justify-end gap-2 mt-2">
            <button className="btn-ghost" onClick={() => setEditing(false)} disabled={busy}>
              Отмена
            </button>
            <button className="btn-primary" onClick={saveEdits} disabled={busy || !draftName.trim()}>
              Сохранить
            </button>
          </div>
        </div>
      ) : student.notes.length > 0 ? (
        <div className="text-[13px] text-ink-600 leading-relaxed">
          {student.notes.map((n) => (
            <div key={n.id}>
              <span className="text-ink-400 mr-1.5">·</span>
              {n.text}
            </div>
          ))}
        </div>
      ) : (
        <div className="text-xs text-ink-400">Описания пока нет.</div>
      )}

      <div className="text-xs font-semibold text-ink-600 uppercase tracking-wide mt-4 mb-2">Комментарии</div>
      <StudentComments studentId={student.id} authorName={(id) => displayName(id).name} />

      <div className="text-xs font-semibold text-ink-600 uppercase tracking-wide mt-4 mb-2">История</div>
      <StudentHistory student={student} stages={stages} curatorName={(id) => displayName(id).name} />
    </Modal>
  )
}
