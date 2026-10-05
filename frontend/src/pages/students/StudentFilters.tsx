import type { ReactNode } from 'react'
import { useAuth } from '../../auth/useAuth'
import { useCurators } from '../../hooks/useCurators'

function Toggle({
  on,
  onToggle,
  tone = 'accent',
  children,
}: {
  on: boolean
  onToggle: () => void
  tone?: 'accent' | 'warn'
  children: ReactNode
}) {
  const onBg = tone === 'warn' ? 'bg-warn' : 'bg-accent'
  return (
    <button
      type="button"
      role="switch"
      aria-checked={on}
      onClick={onToggle}
      className="flex items-center gap-2 text-xs font-semibold text-ink-600 cursor-pointer select-none bg-transparent border-0 p-0"
    >
      <span className={`w-8 h-[18px] rounded-full relative transition-colors ${on ? onBg : 'bg-border'}`}>
        <span className={`w-3.5 h-3.5 rounded-full bg-white absolute top-0.5 transition-all ${on ? 'left-4' : 'left-0.5'}`} />
      </span>
      {children}
    </button>
  )
}

export function StudentFilters({
  query,
  onQueryChange,
  selectedCuratorIds,
  onToggleCurator,
  onlyStuck,
  onToggleStuck,
  groupByCohort,
  onToggleGroupByCohort,
  showNormMeter,
  onToggleNormMeter,
  onAddStudent,
}: {
  query: string
  onQueryChange: (v: string) => void
  selectedCuratorIds: Set<string>
  onToggleCurator: (id: string | 'all') => void
  onlyStuck: boolean
  onToggleStuck: () => void
  groupByCohort: boolean
  onToggleGroupByCohort: () => void
  showNormMeter: boolean
  onToggleNormMeter: () => void
  onAddStudent: () => void
}) {
  const { user } = useAuth()
  const { curators } = useCurators()
  const allActive = selectedCuratorIds.size === 0

  return (
    <div>
      <div className="mb-4 flex items-center gap-3">
        <input className="input w-64" placeholder="Поиск по имени или @нику" value={query} onChange={(e) => onQueryChange(e.target.value)} />
        <button className="btn-primary ml-auto" onClick={onAddStudent}>
          + Ученик
        </button>
      </div>
      <div className="flex items-center gap-2.5 mb-4 flex-wrap">
        {user?.role === 'ADMIN' && (
          <>
            <button className={`chip ${allActive ? '!bg-ink-900 !border-ink-900 !text-white' : ''}`} onClick={() => onToggleCurator('all')}>
              Все кураторы
            </button>
            {curators.map((c) => (
              <button
                key={c.id}
                className={`chip ${selectedCuratorIds.has(c.id) ? 'active' : ''}`}
                onClick={() => onToggleCurator(c.id)}
              >
                <span className="w-1.5 h-1.5 rounded-full inline-block" style={{ background: c.avatarColor ?? '#93989F' }} />
                {c.fullName}
              </button>
            ))}
          </>
        )}
        <div className="ml-auto flex items-center gap-5 flex-wrap">
          <Toggle on={groupByCohort} onToggle={onToggleGroupByCohort}>
            По когортам
          </Toggle>
          <Toggle on={showNormMeter} onToggle={onToggleNormMeter}>
            Шкала нормы
          </Toggle>
          <Toggle on={onlyStuck} onToggle={onToggleStuck} tone="warn">
            Только превысившие норму этапа
          </Toggle>
        </div>
      </div>
    </div>
  )
}
