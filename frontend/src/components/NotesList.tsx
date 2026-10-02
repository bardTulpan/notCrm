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

/** Read-only description: one line per note in a neutral panel, with clickable links. */
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
    <div className="rounded-xl bg-bg border border-border px-4 py-3 flex flex-col gap-1.5 text-[14px] leading-snug text-ink-900">
      {notes.map((n, i) => (
        <div key={n.id ?? i} className="whitespace-pre-wrap min-w-0">
          {linkify(n.text)}
        </div>
      ))}
    </div>
  )
}
