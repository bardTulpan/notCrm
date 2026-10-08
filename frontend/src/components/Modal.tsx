import { useRef, type ReactNode } from 'react'

export function Modal({
  title,
  subtitle,
  leading,
  onClose,
  children,
  width = 460,
}: {
  title: ReactNode
  subtitle?: ReactNode
  /** Shown left of the title and subtitle (e.g. a photo). */
  leading?: ReactNode
  onClose: () => void
  children: ReactNode
  width?: number
}) {
  // Close on a backdrop click only when the press *started* on the backdrop too. Otherwise selecting text
  // inside the card and releasing the mouse outside it counts as a click on the backdrop and closes the modal.
  const pressStartedOnBackdrop = useRef(false)

  const closeButton = (
    <button
      className="border-none bg-bg w-7 h-7 rounded-full cursor-pointer text-ink-600 text-sm leading-none flex-shrink-0"
      onClick={onClose}
      aria-label="Закрыть"
    >
      ✕
    </button>
  )

  return (
    <div
      className="fixed inset-0 bg-[rgba(18,21,28,0.45)] flex items-center justify-center z-50 p-5"
      onMouseDown={(e) => {
        pressStartedOnBackdrop.current = e.target === e.currentTarget
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget && pressStartedOnBackdrop.current) onClose()
        pressStartedOnBackdrop.current = false
      }}
    >
      <div
        className="card p-6 w-full max-h-[84vh] overflow-y-auto"
        style={{ maxWidth: width }}
      >
        {leading ? (
          <div className="flex items-start gap-3.5 mb-2.5">
            {leading}
            <div className="flex-1 min-w-0">
              <div className="flex items-start justify-between gap-2 mb-1">
                <div className="font-display font-bold text-lg min-w-0">{title}</div>
                {closeButton}
              </div>
              {subtitle && <div className="text-xs text-ink-600 flex items-center gap-1.5 flex-wrap">{subtitle}</div>}
            </div>
          </div>
        ) : (
          <>
            <div className="flex items-start justify-between mb-1">
              <div className="font-display font-bold text-lg">{title}</div>
              {closeButton}
            </div>
            {subtitle && <div className="text-xs text-ink-600 mb-2.5 flex items-center gap-1.5 flex-wrap">{subtitle}</div>}
          </>
        )}
        {children}
      </div>
    </div>
  )
}
