export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="card p-4 flex items-center justify-between gap-4 bg-warn-soft border-warn/30">
      <span className="text-sm text-warn">{message}</span>
      {onRetry && (
        <button className="btn-ghost" onClick={onRetry}>
          Повторить
        </button>
      )}
    </div>
  )
}
