import { telegramLabel, telegramUrl } from '../utils/telegram'

/** Clickable Telegram handle (opens t.me in a new tab); falls back to plain text if the value isn't a valid handle. */
export function TelegramLink({ value, className = '' }: { value: string | null | undefined; className?: string }) {
  if (!value) return null
  const url = telegramUrl(value)
  const base = `font-mono text-xs text-accent whitespace-nowrap ${className}`
  if (!url) return <span className={base}>{value}</span>
  return (
    <a
      className={`${base} hover:underline`}
      href={url}
      target="_blank"
      rel="noopener noreferrer"
      onClick={(e) => e.stopPropagation()}
    >
      {telegramLabel(value)}
    </a>
  )
}
