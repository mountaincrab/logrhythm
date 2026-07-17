import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AppShell from '../components/AppShell'
import TimelineEntryRow from '../components/TimelineEntryRow'
import AddPoopSheet from '../components/sheets/AddPoopSheet'
import AddFoodSheet from '../components/sheets/AddFoodSheet'
import AddNoteSheet from '../components/sheets/AddNoteSheet'
import { useEntriesContext } from '../contexts/EntriesContext'
import { useAuth } from '../contexts/AuthContext'
import { useProfileContext } from '../contexts/ProfileContext'
import { usePagedTimeline } from '../hooks/usePagedTimeline'
import { TimelineEntry } from '../types'
import { dayKey, formatDayLabel, formatDayShort } from '../lib/dates'

type SheetKind = 'poop' | 'food' | 'note' | null

const LOG_BUTTONS = [
  ['poop', '💩', 'Poop'],
  ['food', '🍴', 'Food'],
  ['note', '📝', 'Note'],
] as const

export default function HomePage() {
  // CRUD stays on the shared context; the timeline feed is paged separately so
  // Home no longer loads all history (History still reads the full context).
  const { addPoop, addFood, addNote } = useEntriesContext()
  const { user } = useAuth()
  const { activeProfileId } = useProfileContext()
  const { timeline, loading, hasMore, loadingMore, loadMore } = usePagedTimeline(user!.uid, activeProfileId)
  const [sheet, setSheet] = useState<SheetKind>(null)
  const navigate = useNavigate()

  // Infinite scroll: load the next page when the sentinel scrolls into view.
  const sentinel = useRef<HTMLDivElement | null>(null)
  useEffect(() => {
    const el = sentinel.current
    if (!el || !hasMore) return
    const io = new IntersectionObserver(
      (entries) => { if (entries[0]?.isIntersecting) loadMore() },
      { rootMargin: '400px' },
    )
    io.observe(el)
    return () => io.disconnect()
  }, [hasMore, loadMore])

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

  // Desktop: compact pills in the header. Hidden on phones, where the sidebar
  // is gone and logging lives in the bottom bar (mirrors the Android app).
  const desktopLogButtons = (
    <div className="hidden md:flex gap-2">
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

  // Phone: full-width bar of vertical emoji+label cards above the tab bar.
  const mobileLogBar = (
    <div className="border-t border-DEFAULT bg-surface px-3 py-2.5">
      <div className="mx-auto max-w-4xl grid grid-cols-3 gap-2">
        {LOG_BUTTONS.map(([kind, emoji, label]) => (
          <button
            key={kind}
            onClick={() => setSheet(kind)}
            className="flex flex-col items-center gap-1 py-2.5 rounded-2xl bg-surface-raised border border-DEFAULT text-fg-muted hover:bg-surface-high transition-colors"
          >
            <span className="text-[22px] leading-none">{emoji}</span>
            <span className="text-[11px] font-bold">{label}</span>
          </button>
        ))}
      </div>
    </div>
  )

  return (
    <AppShell title="Home" subtitle={subtitle} headerRight={desktopLogButtons} showProfileSwitcher bottomBar={mobileLogBar}>
      {loading ? (
        <p className="text-fg-faint text-sm py-8">Loading…</p>
      ) : groups.length === 0 ? (
        <div className="py-24 text-center">
          <div className="text-5xl mb-4">🩺</div>
          <p className="text-fg font-semibold">Nothing logged yet</p>
          <p className="text-fg-muted text-sm mt-1">Tap a log button to add your first entry.</p>
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
          {/* Infinite-scroll sentinel + paging spinner */}
          {hasMore && <div ref={sentinel} className="h-px" />}
          {loadingMore && <p className="text-fg-faint text-sm text-center py-4">Loading…</p>}
        </div>
      )}

      {sheet === 'poop' && <AddPoopSheet onClose={() => setSheet(null)} onSave={addPoop} />}
      {sheet === 'food' && <AddFoodSheet onClose={() => setSheet(null)} onSave={addFood} />}
      {sheet === 'note' && <AddNoteSheet onClose={() => setSheet(null)} onSave={addNote} />}
    </AppShell>
  )
}
