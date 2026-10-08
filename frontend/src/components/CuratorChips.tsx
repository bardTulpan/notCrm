import type { CuratorOption } from '../hooks/useCurators'

/** «Все кураторы» + one chip per curator; an empty selection means everyone. */
export function CuratorChips({
  curators,
  selected,
  onToggle,
}: {
  curators: CuratorOption[]
  selected: Set<string>
  onToggle: (id: string | 'all') => void
}) {
  const allActive = selected.size === 0
  return (
    <>
      <button className={`chip ${allActive ? '!bg-ink-900 !border-ink-900 !text-white' : ''}`} onClick={() => onToggle('all')}>
        Все кураторы
      </button>
      {curators.map((c) => (
        <button key={c.id} className={`chip ${selected.has(c.id) ? 'active' : ''}`} onClick={() => onToggle(c.id)}>
          <span className="w-1.5 h-1.5 rounded-full inline-block" style={{ background: c.avatarColor ?? '#93989F' }} />
          {c.fullName}
        </button>
      ))}
    </>
  )
}
