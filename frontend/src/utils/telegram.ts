/** Turns "@name", "name", "t.me/name" or "https://t.me/name" into a canonical https://t.me/name link ("" if unusable). */
export function telegramUrl(raw: string | null | undefined): string {
  if (!raw) return ''
  const handle = raw
    .trim()
    .replace(/^https?:\/\//i, '')
    .replace(/^(www\.)?(t|telegram)\.me\//i, '')
    .replace(/^@/, '')
    .split(/[/?#\s]/)[0]
  return /^[A-Za-z0-9_]{3,64}$/.test(handle) ? `https://t.me/${handle}` : ''
}

/** Display form: always with a leading "@" for plain handles. */
export function telegramLabel(raw: string): string {
  const url = telegramUrl(raw)
  return url ? `@${url.slice('https://t.me/'.length)}` : raw.trim()
}
