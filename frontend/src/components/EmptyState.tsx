export function EmptyState({ text = 'Пусто.' }: { text?: string }) {
  return <div className="text-xs text-ink-400 py-6">{text}</div>
}
