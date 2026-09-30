import { useRef, useState, type KeyboardEvent } from 'react'

const DASH_LINE = /^(\s*)-\s(.*)$/

/**
 * Plain textarea state that auto-continues "- " prefixed lines on Enter
 * (Notion/markdown-style), and drops the prefix when the list item is empty.
 * Lines are otherwise stored verbatim — no forced bullet formatting.
 */
export function useAutoListTextarea(initial: string) {
  const [value, setValue] = useState(initial)
  const ref = useRef<HTMLTextAreaElement>(null)

  function onKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key !== 'Enter') return
    const el = e.currentTarget
    const cursor = el.selectionStart
    const lineStart = value.lastIndexOf('\n', cursor - 1) + 1
    const match = value.slice(lineStart, cursor).match(DASH_LINE)
    if (!match) return
    e.preventDefault()
    const [, indent, rest] = match

    if (rest.trim() === '') {
      const next = value.slice(0, lineStart) + value.slice(cursor)
      setValue(next)
      requestAnimationFrame(() => el.setSelectionRange(lineStart, lineStart))
      return
    }

    const insertion = `\n${indent}- `
    const next = value.slice(0, cursor) + insertion + value.slice(cursor)
    setValue(next)
    const newCursor = cursor + insertion.length
    requestAnimationFrame(() => el.setSelectionRange(newCursor, newCursor))
  }

  return { value, setValue, onKeyDown, ref }
}
