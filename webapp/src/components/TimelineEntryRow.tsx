import { TimelineEntry } from '../types'
import { ratingColor } from '../lib/ratings'
import { bristol } from '../lib/bristol'
import { mealTagLabel } from '../lib/mealTags'
import { formatTime } from '../lib/dates'

function RatingPill({ n }: { n: number }) {
  const c = ratingColor(n)
  return (
    <span
      className="inline-flex items-center gap-1.5 font-bold text-xs px-2 py-0.5 rounded-full"
      style={{ background: c.soft, color: c.bg }}
    >
      <span
        className="w-[18px] h-[18px] rounded-full inline-flex items-center justify-center text-[11px] font-extrabold"
        style={{ background: c.bg, color: c.fg }}
      >
        {n}
      </span>
      {c.label}
    </span>
  )
}

function describePoop(types: number[]): string {
  if (types.length === 0) return 'No type recorded'
  const sorted = [...types].sort((a, b) => a - b)
  const nums = sorted.join(', ')
  const plain = sorted.map((t) => bristol(t)?.plain).filter(Boolean).join(' / ')
  return `Bristol ${nums} · ${plain}`
}

export default function TimelineEntryRow({ item, onClick }: { item: TimelineEntry; onClick: () => void }) {
  let dotColor = 'var(--surface-high)'
  let kindLabel = ''
  let kindColor = 'var(--fg-faint)'
  let body: React.ReactNode = null
  let meta: React.ReactNode = null

  if (item.kind === 'poop') {
    const c = ratingColor(item.entry.blood)
    dotColor = c.bg
    kindLabel = 'Poop'
    kindColor = c.bg
    body = (
      <>
        {describePoop(item.entry.bristolTypes)}
        {item.entry.notes && <span className="text-fg-muted"> · {item.entry.notes}</span>}
      </>
    )
    meta = <RatingPill n={item.entry.blood} />
  } else if (item.kind === 'food') {
    kindLabel = 'Food'
    const tag = mealTagLabel(item.entry.mealTag)
    body = (
      <span>
        {item.entry.items}
        {tag && <span className="text-fg-muted"> · {tag}</span>}
      </span>
    )
  } else {
    dotColor = 'var(--accent)'
    kindLabel = 'Note'
    kindColor = 'var(--accent-text)'
    body = <span>{item.entry.content}</span>
    const flags = [item.entry.caffeine && 'Caffeine', item.entry.alcohol && 'Alcohol'].filter(Boolean) as string[]
    if (flags.length > 0) {
      meta = (
        <div className="flex gap-1.5">
          {flags.map((f) => (
            <span key={f} className="px-2 py-0.5 rounded-full bg-surface-high text-fg-muted text-[11px] font-semibold">
              {f}
            </span>
          ))}
        </div>
      )
    }
  }

  return (
    <div className="relative mb-2">
      <span
        className="absolute -left-[19px] top-4 w-[11px] h-[11px] rounded-full"
        style={{ background: dotColor, border: '2px solid var(--bg)' }}
      />
      <button
        onClick={onClick}
        className="w-full text-left bg-surface-raised border border-DEFAULT rounded-2xl px-3.5 py-3 hover:bg-surface-high/40 transition-colors"
      >
        <div className="flex items-center gap-2 mb-1">
          <span className="text-[13px] font-bold font-mono tabular-nums">{formatTime(item.occurredAt)}</span>
          <span className="text-[11px] font-bold uppercase tracking-wide" style={{ color: kindColor }}>
            · {kindLabel}
          </span>
        </div>
        <div className="text-sm leading-snug text-fg break-words">{body}</div>
        {meta && <div className="mt-1.5 flex items-center gap-2.5">{meta}</div>}
      </button>
    </div>
  )
}
