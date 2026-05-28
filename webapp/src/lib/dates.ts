// Date/time helpers. All timestamps are epoch milliseconds (matching the
// `occurredAt` / `createdAt` Long fields written by the Android app).

const startOfDay = (ms: number): number => {
  const d = new Date(ms)
  d.setHours(0, 0, 0, 0)
  return d.getTime()
}

export const dayKey = (ms: number): number => startOfDay(ms)

export function formatTime(ms: number): string {
  return new Date(ms).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })
}

/** "Fri 30 Jan" */
export function formatDayShort(ms: number): string {
  return new Date(ms).toLocaleDateString([], { weekday: 'short', day: 'numeric', month: 'short' })
}

/** "Thu 29 Jan 2026" */
export function formatDayFull(ms: number): string {
  return new Date(ms).toLocaleDateString([], { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })
}

/** Relative day label used for timeline group headers. */
export function formatDayLabel(ms: number): string {
  const today = startOfDay(Date.now())
  const day = startOfDay(ms)
  const diffDays = Math.round((today - day) / 86_400_000)
  if (diffDays === 0) return `Today · ${formatDayShort(ms)}`
  if (diffDays === 1) return `Yesterday · ${formatDayShort(ms)}`
  return formatDayShort(ms)
}

/** Value for <input type="datetime-local"> (local time, no timezone suffix). */
export function toLocalInputValue(ms: number): string {
  const d = new Date(ms)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function fromLocalInputValue(value: string): number {
  return new Date(value).getTime()
}
