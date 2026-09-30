import { createContext, useCallback, useState, type ReactNode } from 'react'

export interface ToastItem {
  id: number
  kind: 'success' | 'error'
  text: string
  actionLabel?: string
  onAction?: () => void
  durationMs: number
}

export interface ToastOptions {
  actionLabel?: string
  onAction?: () => void
  durationMs?: number
}

interface ToastContextValue {
  push: (kind: ToastItem['kind'], text: string, options?: ToastOptions) => void
}

// eslint-disable-next-line react-refresh/only-export-components
export const ToastContext = createContext<ToastContextValue | null>(null)

let seq = 0

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])

  const dismiss = useCallback((id: number) => {
    setItems((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const push = useCallback(
    (kind: ToastItem['kind'], text: string, options?: ToastOptions) => {
      const id = ++seq
      const durationMs = options?.durationMs ?? 4000
      setItems((prev) => [
        ...prev,
        { id, kind, text, actionLabel: options?.actionLabel, onAction: options?.onAction, durationMs },
      ])
      setTimeout(() => dismiss(id), durationMs)
    },
    [dismiss],
  )

  return (
    <ToastContext.Provider value={{ push }}>
      {children}
      <div className="fixed bottom-4 right-4 flex flex-col gap-2 z-[100]">
        {items.map((t) => (
          <div
            key={t.id}
            className={`card px-4 py-3 text-sm shadow-lg min-w-[240px] overflow-hidden relative ${
              t.kind === 'error' ? 'bg-warn-soft border-warn text-warn' : 'bg-success-soft border-success text-success'
            }`}
          >
            <div className="flex items-center justify-between gap-3">
              <span>{t.text}</span>
              {t.actionLabel && t.onAction && (
                <button
                  className="font-semibold underline whitespace-nowrap"
                  onClick={() => {
                    t.onAction?.()
                    dismiss(t.id)
                  }}
                >
                  {t.actionLabel}
                </button>
              )}
            </div>
            {t.actionLabel && (
              <div
                className="absolute bottom-0 left-0 h-0.5 bg-current opacity-40"
                style={{ animation: `toast-progress ${t.durationMs}ms linear forwards` }}
              />
            )}
          </div>
        ))}
      </div>
      <style>{`
        @keyframes toast-progress {
          from { width: 100%; }
          to { width: 0%; }
        }
      `}</style>
    </ToastContext.Provider>
  )
}
