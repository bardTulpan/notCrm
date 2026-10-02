type Cell = string | number | boolean | null | undefined

function escapeCell(value: Cell): string {
  let text = value === null || value === undefined ? '' : String(value)
  // Spreadsheet formula injection: a text cell that starts with = + - @ would be evaluated by Excel/Sheets.
  // Numbers are safe, and plain Telegram handles ("@name") are kept as they are.
  if (typeof value === 'string' && /^[=+\-@\t\r]/.test(text) && !/^@[A-Za-z0-9_]+$/.test(text)) text = `'${text}`
  return /[";\n\r]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
}

/**
 * Builds a CSV that opens correctly in Excel with Russian locale: ";" separator, CRLF, UTF-8 BOM.
 */
export function toCsv(header: string[], rows: Cell[][]): string {
  return '﻿' + [header, ...rows].map((r) => r.map(escapeCell).join(';')).join('\r\n')
}

export function downloadCsv(filename: string, header: string[], rows: Cell[][]) {
  const blob = new Blob([toCsv(header, rows)], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

export function csvDateStamp(): string {
  return new Date().toISOString().slice(0, 10)
}
