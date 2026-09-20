import { createContext, useCallback, useState, type ReactNode } from 'react'

export interface ToastItem {
  id: number
  kind: 'success' | 'error'
  text: string
}

interface ToastContextValue {
  push: (kind: ToastItem['kind'], text: string) => void
}

// eslint-disable-next-line react-refresh/only-export-components
export const ToastContext = createContext<ToastContextValue | null>(null)

let seq = 0

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])

  const push = useCallback((kind: ToastItem['kind'], text: string) => {
    const id = ++seq
    setItems((prev) => [...prev, { id, kind, text }])
    setTimeout(() => {
      setItems((prev) => prev.filter((t) => t.id !== id))
    }, 4000)
  }, [])

  return (
    <ToastContext.Provider value={{ push }}>
      {children}
      <div className="fixed bottom-4 right-4 flex flex-col gap-2 z-[100]">
        {items.map((t) => (
          <div
            key={t.id}
            className={`card px-4 py-3 text-sm shadow-lg min-w-[240px] ${
              t.kind === 'error' ? 'bg-warn-soft border-warn text-warn' : 'bg-success-soft border-success text-success'
            }`}
          >
            {t.text}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
