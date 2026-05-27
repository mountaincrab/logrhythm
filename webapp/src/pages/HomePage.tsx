import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AppShell from '../components/AppShell'
import TimelineEntryRow from '../components/TimelineEntryRow'
import AddPoopSheet from '../components/sheets/AddPoopSheet'
import AddFoodSheet from '../components/sheets/AddFoodSheet'
import AddNoteSheet from '../components/sheets/AddNoteSheet'
import { useEntriesContext } from '../contexts/EntriesContext'
import { TimelineEntry } from '../types'
import { dayKey, formatDayLabel, formatDayShort } from '../lib/dates'

type SheetKind = 'poop' | 'food' | 'note' | null

const LOG_BUTTONS = [
  ['poop', '💩', 'Poop'],
  ['food', '🍴', 'Food'],
  ['note', '📝', 'Note'],
] as const

export default function HomePage() {
  const { timeline, loading, addPoop, addFood, addNote } = useEntriesContext()
  const [sheet, setSheet] = useState<SheetKind>(null)
  const navigate = useNavigate()

  const groups = useMemo(() => {
    const map = new Map<number, TimelineEntry[]>()
    for (const item of timeline) {
      const key = dayKey(item.occurredAt)
      const arr = map.get(key) ?? []
      arr.push(item)
      map.set(key, arr)
    }
    return [...map.entries()].sort((a, b) => b[0] - a[0])
  }, [timeline])

  const subtitle = useMemo(() => {
    const todayKey = dayKey(Date.now())
    const todaysPoops = timeline.filter((t) => t.kind === 'poop' && dayKey(t.occurredAt) === todayKey)
    if (todaysPoops.length === 0) return `${formatDayShort(Date.now())} · no poops today`
    const worst = Math.max(...todaysPoops.map((t) => (t.kind === 'poop' ? t.entry.blood : 1)))
    return `${formatDayShort(Date.now())} · ${todaysPoops.length} poop${todaysPoops.length > 1 ? 's' : ''} · rating ${worst}`
  }, [timeline])

  const openEntry = (item: TimelineEntry) => navigate(`/entry/${item.kind}/${item.entry.id}`)

  const logButtons = (
    <div className="flex gap-2">
      {LOG_BUTTONS.map(([kind, emoji, label]) => (
        <button
          key={kind}
          onClick={() => setSheet(kind)}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-surface-raised border border-DEFAULT text-fg text-sm font-semibold hover:bg-surface-high transition-colors"
        >
          <span className="text-base leading-none">{emoji}</span>
          {label}
        </button>
      ))}
    </div>
  )

  return (
    <AppShell title="Home" subtitle={subtitle} headerRight={logButtons}>
      {loading ? (
        <p className="text-fg-faint text-sm py-8">Loading…</p>
      ) : groups.length === 0 ? (
        <div className="py-24 text-center">
          <div className="text-5xl mb-4">🩺</div>
          <p className="text-fg font-semibold">Nothing logged yet</p>
          <p className="text-fg-muted text-sm mt-1">Use the buttons in the top right to log your first entry.</p>
        </div>
      ) : (
        <div className="space-y-7">
          {groups.map(([key, items]) => (
            <div key={key}>
              <div className="flex items-baseline justify-between mb-3">
                <span className="ds-eyebrow">{formatDayLabel(key)}</span>
                <span className="text-[11px] text-fg-faint font-semibold">
                  {items.length} entr{items.length === 1 ? 'y' : 'ies'}
                </span>
              </div>
              <div className="relative pl-[22px]">
                <span className="absolute left-[7px] top-1.5 bottom-1.5 w-px bg-[var(--border)]" />
                {items.map((item) => (
                  <TimelineEntryRow key={`${item.kind}-${item.entry.id}`} item={item} onClick={() => openEntry(item)} />
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {sheet === 'poop' && <AddPoopSheet onClose={() => setSheet(null)} onSave={addPoop} />}
      {sheet === 'food' && <AddFoodSheet onClose={() => setSheet(null)} onSave={addFood} />}
      {sheet === 'note' && <AddNoteSheet onClose={() => setSheet(null)} onSave={addNote} />}
    </AppShell>
  )
}
