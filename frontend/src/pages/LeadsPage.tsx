import { useCallback, useEffect, useState } from 'react'
import { leadsApi } from '../api/leads'
import { Loader } from '../components/Loader'
import { ErrorState } from '../components/ErrorState'
import { EmptyState } from '../components/EmptyState'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { useToast, apiErrorMessage } from '../hooks/useToast'
import { addDaysIso } from '../utils/dates'
import { LeadCard } from './leads/LeadCard'
import { LeadFormModal } from './leads/LeadFormModal'
import { ConvertLeadModal } from './leads/ConvertLeadModal'
import type { LeadDto } from '../types'

export function LeadsPage() {
  const [view, setView] = useState<'active' | 'archive'>('active')
  const [query, setQuery] = useState('')
  const [leads, setLeads] = useState<LeadDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const { push } = useToast()

  const [formLead, setFormLead] = useState<LeadDto | null | 'new'>(null)
  const [convertLead, setConvertLead] = useState<LeadDto | null>(null)
  const [discardTarget, setDiscardTarget] = useState<LeadDto | null>(null)
  const [archivedCount, setArchivedCount] = useState(0)

  const refetch = useCallback(() => {
    setLoading(true)
    setError(null)
    return leadsApi
      .list({ status: view === 'active' ? 'ACTIVE' : 'ARCHIVED', search: query || undefined })
      .then((data) => {
        const sorted =
          view === 'active'
            ? [...data].sort((a, b) => new Date(a.nextPingAt ?? 0).getTime() - new Date(b.nextPingAt ?? 0).getTime())
            : data
        setLeads(sorted)
      })
      .catch((e) => setError(apiErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [view, query])

  const refetchArchivedCount = useCallback(() => {
    leadsApi.list({ status: 'ARCHIVED' }).then((data) => setArchivedCount(data.length))
  }, [])

  useEffect(() => {
    refetch()
  }, [refetch])

  useEffect(() => {
    refetchArchivedCount()
  }, [refetchArchivedCount, leads])

  async function snooze(lead: LeadDto) {
    try {
      await leadsApi.postponePing(lead.id, addDaysIso(2))
      refetch()
    } catch (err) {
      push('error', apiErrorMessage(err))
    }
  }

  async function discard(lead: LeadDto) {
    try {
      await leadsApi.archive(lead.id)
      push('success', 'Лид перемещён в архив')
      refetch()
    } catch (err) {
      push('error', apiErrorMessage(err))
    }
  }

  async function restore(lead: LeadDto) {
    try {
      await leadsApi.restore(lead.id)
      push('success', 'Лид восстановлен')
      refetch()
    } catch (err) {
      push('error', apiErrorMessage(err))
    }
  }

  return (
    <div>
      <div className="flex items-center gap-2.5 mb-4">
        <input
          className="input w-60"
          placeholder="Поиск по имени или @username"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button className="text-xs font-semibold text-ink-600 underline hover:text-ink-900" onClick={() => setView(view === 'active' ? 'archive' : 'active')}>
          {view === 'active' ? `Архив (${archivedCount})` : '← К активным лидам'}
        </button>
        <button className="btn-primary ml-auto" onClick={() => setFormLead('new')}>
          + Лид
        </button>
      </div>

      {loading && <Loader />}
      {error && <ErrorState message={error} onRetry={refetch} />}
      {!loading && !error && leads.length === 0 && <EmptyState />}
      {!loading && !error && leads.length > 0 && (
        <div className="flex flex-col gap-2.5 max-w-[720px]">
          {leads.map((lead) => (
            <LeadCard
              key={lead.id}
              lead={lead}
              onEdit={() => setFormLead(lead)}
              onConvert={() => setConvertLead(lead)}
              onSnooze={() => snooze(lead)}
              onDiscard={() => setDiscardTarget(lead)}
              onRestore={() => restore(lead)}
            />
          ))}
        </div>
      )}

      {formLead && (
        <LeadFormModal
          lead={formLead === 'new' ? null : formLead}
          onClose={() => setFormLead(null)}
          onSaved={refetch}
        />
      )}
      {convertLead && <ConvertLeadModal lead={convertLead} onClose={() => setConvertLead(null)} onConverted={refetch} />}
      {discardTarget && (
        <ConfirmDialog
          title="Выкинуть лида?"
          message={`${discardTarget.name} будет перемещён в архив. Это можно отменить восстановлением.`}
          confirmLabel="Выкинуть"
          danger
          onConfirm={() => discard(discardTarget)}
          onClose={() => setDiscardTarget(null)}
        />
      )}
    </div>
  )
}
