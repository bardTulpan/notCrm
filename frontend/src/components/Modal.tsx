import type { ReactNode } from 'react'

export function Modal({
  title,
  subtitle,
  onClose,
  children,
  width = 460,
}: {
  title: ReactNode
  subtitle?: ReactNode
  onClose: () => void
  children: ReactNode
  width?: number
}) {
  return (
    <div
      className="fixed inset-0 bg-[rgba(18,21,28,0.45)] flex items-center justify-center z-50 p-5"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div
        className="card p-6 w-full max-h-[84vh] overflow-y-auto"
        style={{ maxWidth: width }}
      >
        <div className="flex items-start justify-between mb-1">
          <div className="font-display font-bold text-lg">{title}</div>
          <button
            className="border-none bg-bg w-7 h-7 rounded-full cursor-pointer text-ink-600 text-sm leading-none flex-shrink-0"
            onClick={onClose}
            aria-label="Закрыть"
          >
            ✕
          </button>
        </div>
        {subtitle && <div className="text-xs text-ink-600 mb-2.5 flex items-center gap-1.5 flex-wrap">{subtitle}</div>}
        {children}
      </div>
    </div>
  )
}
