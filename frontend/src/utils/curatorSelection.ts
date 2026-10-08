/** Chip toggling for curator filters: 'all' clears the selection, which means everyone. */
export function toggleCuratorSelection(prev: Set<string>, id: string | 'all'): Set<string> {
  if (id === 'all') return new Set()
  const next = new Set(prev)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  return next
}
