import { pingStatus } from '../../utils/pingStatus'
import { pingBadgeClass } from '../../components/TimeBadge'
import type { LeadDto } from '../../types'

const iconBtn = '!px-2 !py-1 !text-xs !leading-none'

export function LeadCard({
  lead,
  onConvert,
  onSnooze,
  onDiscard,
  onRestore,
  onEdit,
}: {
  lead: LeadDto
  onConvert: () => void
  onSnooze: () => void
  onDiscard: () => void
  onRestore: () => void
  onEdit: () => void
}) {
  const archived = lead.status === 'ARCHIVED'
  const p = pingStatus(lead.nextPingAt)
  const firstNote = lead.notes[0]?.text
  const extraNotes = lead.notes.length - 1

  return (
    <div className={`card px-3 py-2 flex items-center gap-2.5 ${archived ? 'opacity-70' : ''}`}>
      <div className="flex-1 min-w-0 cursor-pointer" onClick={onEdit}>
        <div className="flex items-baseline gap-2 flex-wrap">
          <span className="font-display font-semibold text-[14px] whitespace-nowrap">{lead.name}</span>
          {lead.telegramUsername && <span className="font-mono text-xs text-accent whitespace-nowrap">{lead.telegramUsername}</span>}
          {lead.priceDescription && (
            <span className="font-mono text-xs bg-bg px-1.5 py-0.5 rounded-md whitespace-nowrap">{lead.priceDescription}</span>
          )}
          {!archived && (
            <span className={`font-mono text-xs px-1.5 py-0.5 rounded-md font-medium whitespace-nowrap ${pingBadgeClass(p.tier)}`}>
              {p.label}
            </span>
          )}
        </div>
        {firstNote && (
          <div className="text-xs text-ink-600 truncate mt-0.5">
            <span className="text-ink-400 mr-1">·</span>
            {firstNote}
            {extraNotes > 0 && <span className="text-ink-400"> +{extraNotes}</span>}
          </div>
        )}
      </div>
      <div className="flex items-center gap-1 flex-shrink-0">
        {archived ? (
          <button className={`btn-primary ${iconBtn}`} onClick={onRestore} title="Восстановить">
            ↩
          </button>
        ) : (
          <>
            <button className={`btn-success ${iconBtn}`} onClick={onConvert} title="Оплатил">
              ✓
            </button>
            <button className={`btn-ghost ${iconBtn}`} onClick={onSnooze} title="Отложить пинг на 2 дня">
              +2д
            </button>
            <button className={`btn-danger ${iconBtn}`} onClick={onDiscard} title="Выкинуть">
              ✕
            </button>
          </>
        )}
      </div>
    </div>
  )
}
