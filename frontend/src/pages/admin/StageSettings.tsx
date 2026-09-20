import { useState } from 'react'
import { stagesApi } from '../../api/stages'
import { useToast, apiErrorMessage } from '../../hooks/useToast'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import type { StageDto } from '../../types'

export function StageSettings({ stages, onChanged }: { stages: StageDto[]; onChanged: () => void }) {
  const { push } = useToast()
  const [drafts, setDrafts] = useState<Record<string, string>>({})
  const [nameDrafts, setNameDrafts] = useState<Record<string, string>>({})
  const [archiveTarget, setArchiveTarget] = useState<StageDto | null>(null)

  async function saveNormDays(stage: StageDto) {
    const raw = drafts[stage.id]
    if (raw === undefined) return
    const value = Number(raw)
    if (!Number.isInteger(value) || value <= 0) return
    try {
      await stagesApi.update(stage.id, { normDays: value })
      onChanged()
    } catch (err) {
      push('error', apiErrorMessage(err))
    }
  }

  async function saveName(stage: StageDto) {
    const name = nameDrafts[stage.id]?.trim()
    if (!name || name === stage.name) return
    try {
      await stagesApi.update(stage.id, { name })
      onChanged()
    } catch (err) {
      push('error', apiErrorMessage(err))
    }
  }

  async function move(stage: StageDto, direction: -1 | 1) {
    const sorted = [...stages].sort((a, b) => a.position - b.position)
    const idx = sorted.findIndex((s) => s.id === stage.id)
    const swapWith = idx + direction
    if (swapWith < 0 || swapWith >= sorted.length) return
    const ids = sorted.map((s) => s.id)
    ;[ids[idx], ids[swapWith]] = [ids[swapWith], ids[idx]]
    try {
      await stagesApi.reorder(ids)
      onChanged()
    } catch (err) {
      push('error', apiErrorMessage(err))
    }
  }

  async function archive(stage: StageDto) {
    try {
      await stagesApi.archive(stage.id)
      push('success', 'Этап архивирован')
      onChanged()
    } catch (err) {
      push('error', apiErrorMessage(err))
    }
  }

  const sorted = [...stages].sort((a, b) => a.position - b.position)

  return (
    <div>
      <div className="font-display font-semibold text-sm mb-2.5">Длительность этапов</div>
      {sorted.map((stage, idx) => (
        <div key={stage.id} className="card flex items-center justify-between px-3.5 py-2.5 mb-2 max-w-[640px] gap-2">
          <div className="flex flex-col gap-1 flex-1 min-w-0">
            <input
              className="text-[13px] font-medium border-none bg-transparent focus:outline-none focus:bg-bg rounded px-1 -mx-1"
              defaultValue={stage.name}
              onChange={(e) => setNameDrafts((prev) => ({ ...prev, [stage.id]: e.target.value }))}
              onBlur={() => saveName(stage)}
            />
          </div>
          <div className="flex items-center gap-1 flex-shrink-0">
            <button className="btn-ghost px-2" disabled={idx === 0} onClick={() => move(stage, -1)}>
              ↑
            </button>
            <button className="btn-ghost px-2" disabled={idx === sorted.length - 1} onClick={() => move(stage, 1)}>
              ↓
            </button>
          </div>
          <div className="flex items-center gap-1.5 flex-shrink-0">
            {stage.normDays != null ? (
              <>
                <input
                  type="number"
                  min={1}
                  className="input w-16 text-center font-mono"
                  defaultValue={stage.normDays}
                  onChange={(e) => setDrafts((prev) => ({ ...prev, [stage.id]: e.target.value }))}
                  onBlur={() => saveNormDays(stage)}
                />
                <span className="text-xs text-ink-400">дн.</span>
              </>
            ) : (
              <span className="text-xs text-ink-400">без нормы</span>
            )}
          </div>
          <button className="btn-danger flex-shrink-0" onClick={() => setArchiveTarget(stage)}>
            Архивировать
          </button>
        </div>
      ))}
      {archiveTarget && (
        <ConfirmDialog
          title="Архивировать этап?"
          message={`Этап «${archiveTarget.name}» станет недоступен для назначения новым ученикам.`}
          confirmLabel="Архивировать"
          danger
          onConfirm={() => archive(archiveTarget)}
          onClose={() => setArchiveTarget(null)}
        />
      )}
    </div>
  )
}
