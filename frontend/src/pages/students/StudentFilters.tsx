import { useAuth } from '../../auth/useAuth'
import { useCurators } from '../../hooks/useCurators'

export function StudentFilters({
  query,
  onQueryChange,
  selectedCuratorIds,
  onToggleCurator,
  onlyStuck,
  onToggleStuck,
}: {
  query: string
  onQueryChange: (v: string) => void
  selectedCuratorIds: Set<string>
  onToggleCurator: (id: string | 'all') => void
  onlyStuck: boolean
  onToggleStuck: () => void
}) {
  const { user } = useAuth()
  const { curators } = useCurators()
  const allActive = selectedCuratorIds.size === 0

  return (
    <div>
      <div className="mb-4">
        <input className="input w-60" placeholder="Поиск ученика по имени" value={query} onChange={(e) => onQueryChange(e.target.value)} />
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
        <label className="ml-auto flex items-center gap-2 text-xs font-semibold text-ink-600 cursor-pointer select-none">
          <span className={`w-8 h-[18px] rounded-full relative transition-colors ${onlyStuck ? 'bg-warn' : 'bg-border'}`} onClick={onToggleStuck}>
            <span
              className={`w-3.5 h-3.5 rounded-full bg-white absolute top-0.5 transition-all ${onlyStuck ? 'left-4' : 'left-0.5'}`}
            />
          </span>
          Только превысившие норму этапа
        </label>
      </div>
    </div>
  )
}
