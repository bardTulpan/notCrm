import { useState } from 'react'
import { cohortsApi } from '../../api/cohorts'
import { useToast, apiErrorMessage } from '../../hooks/useToast'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { formatDate } from '../../utils/dates'
import type { CohortDto } from '../../types'

export function CohortSettings({ cohorts, onChanged }: { cohorts: CohortDto[]; onChanged: () => void }) {
  const { push } = useToast()
  const [name, setName] = useState('')
  const [startDate, setStartDate] = useState('')
  const [archiveTarget, setArchiveTarget] = useState<CohortDto | null>(null)

  async function create() {
    if (!name.trim() || !startDate) return
    try {
      await cohortsApi.create({ name: name.trim(), startDate })
      setName('')
      setStartDate('')
      push('success', 'Когорта создана')
      onChanged()
    } catch (err) {
      push('error', apiErrorMessage(err))
    }
  }

  async function archive(cohort: CohortDto) {
    try {
      await cohortsApi.archive(cohort.id)
      push('success', 'Когорта архивирована')
      onChanged()
    } catch (err) {
      push('error', apiErrorMessage(err))
    }
  }

  return (
    <div>
      <div className="font-display font-semibold text-sm mb-2.5">Когорты</div>
      <div className="card p-4 max-w-[480px] mb-4 flex flex-col gap-2">
        <input className="input" placeholder="Название когорты" value={name} onChange={(e) => setName(e.target.value)} />
        <div className="flex gap-2">
          <input className="input flex-1" type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
          <button className="btn-primary flex-shrink-0" onClick={create} disabled={!name.trim() || !startDate}>
            Создать
          </button>
        </div>
      </div>
      {cohorts.map((c) => (
        <div key={c.id} className="card flex items-center justify-between px-3.5 py-2.5 mb-2 max-w-[560px]">
          <div>
            <div className="text-[13px] font-medium">{c.name}</div>
            <div className="text-xs text-ink-400">старт {formatDate(c.startDate)}</div>
          </div>
          <button className="btn-danger" onClick={() => setArchiveTarget(c)}>
            Архивировать
          </button>
        </div>
      ))}
      {archiveTarget && (
        <ConfirmDialog
          title="Архивировать когорту?"
          message={`Когорта «${archiveTarget.name}» больше не будет предлагаться при конвертации лидов.`}
          confirmLabel="Архивировать"
          danger
          onConfirm={() => archive(archiveTarget)}
          onClose={() => setArchiveTarget(null)}
        />
      )}
    </div>
  )
}
