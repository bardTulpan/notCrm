import type { ReactNode } from 'react'

const URL_RE = /(https?:\/\/[^\s<]+[^\s<.,;:!?)"'»])/g

/** Splits a line into text and clickable http(s) links. */
function linkify(text: string): ReactNode[] {
  return text.split(URL_RE).map((part, i) =>
    i % 2 === 1 ? (
      <a key={i} href={part} target="_blank" rel="noopener noreferrer" className="text-accent hover:underline break-all">
        {part}
      </a>
    ) : (
      part
    ),
  )
}

/** Read-only description: one row per note, with a bullet and clickable links. */
export function NotesList({ notes, emptyText = 'Описания пока нет.', onAdd }: { notes: { id?: string; text: string }[]; emptyText?: string; onAdd?: () => void }) {
  if (notes.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-border px-4 py-3.5 text-[13px] text-ink-400 flex items-center justify-between gap-3">
        <span>{emptyText}</span>
        {onAdd && (
          <button className="font-semibold text-accent hover:underline whitespace-nowrap" onClick={onAdd}>
            + Добавить
          </button>
        )}
      </div>
    )
  }
  return (
    <ul className="rounded-xl bg-accent-soft/50 border border-accent/10 px-4 py-1 list-none m-0">
      {notes.map((n, i) => (
        <li
          key={n.id ?? i}
          className={`flex items-start gap-2.5 py-2.5 text-[14px] leading-snug text-ink-900 ${i > 0 ? 'border-t border-accent/10' : ''}`}
        >
          <span className="mt-[7px] w-1.5 h-1.5 rounded-full bg-accent flex-shrink-0" />
          <span className="whitespace-pre-wrap min-w-0">{linkify(n.text)}</span>
        </li>
      ))}
    </ul>
  )
}
