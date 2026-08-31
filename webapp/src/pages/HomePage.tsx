import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AppShell from '../components/AppShell'
import TimelineEntryRow from '../components/TimelineEntryRow'
import AddPoopSheet from '../components/sheets/AddPoopSheet'
import AddFoodSheet from '../components/sheets/AddFoodSheet'
import AddNoteSheet from '../components/sheets/AddNoteSheet'
import AddMedicineSheet from '../components/sheets/AddMedicineSheet'
import { useEntriesContext } from '../contexts/EntriesContext'
import { useAuth } from '../contexts/AuthContext'
import { useProfileContext } from '../contexts/ProfileContext'
import { usePagedTimeline } from '../hooks/usePagedTimeline'
import { drawnIconSize, MedicineIcon } from '../components/MedicationIcons'
import { EntryKind, TimelineEntry } from '../types'
import { dayKey, formatDayLabel, formatDayShort } from '../lib/dates'

type SheetKind = 'poop' | 'food' | 'note' | 'medicine' | null

// Medicine covers one-off doses; anything on a schedule records itself and is corrected
// from the Meds page. It's also the one icon here that isn't an emoji — there is no emoji
// for four of the five medication forms, so that whole set is drawn (see MedicationIcons).
const LOG_BUTTONS: { kind: Exclude<SheetKind, null>; label: string; icon: (size: number) => JSX.Element }[] = [
  { kind: 'poop', label: 'Poop', icon: (s) => <span style={{ fontSize: s, lineHeight: 1 }}>💩</span> },
  { kind: 'food', label: 'Food', icon: (s) => <span style={{ fontSize: s, lineHeight: 1 }}>🍴</span> },
  { kind: 'note', label: 'Note', icon: (s) => <span style={{ fontSize: s, lineHeight: 1 }}>📝</span> },
  // Sized through drawnIconSize so the bottle carries the same weight as the three
  // emoji beside it — same nominal size, it paints ~22% less ink than they do.
  { kind: 'medicine', label: 'Medicine', icon: (s) => <MedicineIcon size={drawnIconSize(s)} /> },
]

const ALL_ENTRY_KINDS: EntryKind[] = ['poop', 'food', 'note', 'medicine']
const HOME_FILTER_STORAGE_KEY = 'logrhythm:disabledHomeEntryKinds'

function storedEnabledEntryKinds(): Set<EntryKind> {
  try {
    const stored = localStorage.getItem(HOME_FILTER_STORAGE_KEY)
    if (stored === null) return new Set(ALL_ENTRY_KINDS)

    const parsed: unknown = JSON.parse(stored)
    if (!Array.isArray(parsed)) return new Set(ALL_ENTRY_KINDS)

    const disabled = new Set(parsed.filter((value): value is EntryKind =>
      typeof value === 'string' && ALL_ENTRY_KINDS.includes(value as EntryKind),
    ))
    return new Set(ALL_ENTRY_KINDS.filter((kind) => !disabled.has(kind)))
  } catch {
    return new Set(ALL_ENTRY_KINDS)
  }
}

function storeEnabledEntryKinds(enabled: Set<EntryKind>) {
  try {
    const disabled = ALL_ENTRY_KINDS.filter((kind) => !enabled.has(kind))
    localStorage.setItem(HOME_FILTER_STORAGE_KEY, JSON.stringify(disabled))
  } catch {
    // Storage can be unavailable in restricted browser contexts; keep the filter in memory.
  }
}

/**
 * A fixed slot for a log button's mark, sized for the tallest of them: the drawn bottle
 * needs more box than the emoji do, and without this the Medicine button stood taller than
 * its three neighbours and its label sat a few pixels lower.
 */
function IconSlot({ em, children }: { em: number; children: React.ReactNode }) {
  const side = drawnIconSize(em)
  return (
    <span className="inline-flex items-center justify-center shrink-0" style={{ width: side, height: side }}>
      {children}
    </span>
  )
}

export default function HomePage() {
  // CRUD stays on the shared context; the timeline feed is paged separately so
  // Home no longer loads all history (History still reads the full context).
  const { addPoop, addFood, addNote, addMedicine } = useEntriesContext()
  const { user } = useAuth()
  const { activeProfileId } = useProfileContext()
  const { timeline, loading, hasMore, loadingMore, loadMore } = usePagedTimeline(user!.uid, activeProfileId)
  const [sheet, setSheet] = useState<SheetKind>(null)
  const [enabledKinds, setEnabledKinds] = useState<Set<EntryKind>>(storedEnabledEntryKinds)
  const navigate = useNavigate()

  useEffect(() => {
    storeEnabledEntryKinds(enabledKinds)
  }, [enabledKinds])

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
  }, [hasMore, loadMore, timeline.length, enabledKinds])

  const filtersActive = enabledKinds.size !== ALL_ENTRY_KINDS.length

  const toggleKind = (kind: EntryKind) => {
    setEnabledKinds((current) => {
      const next = new Set(current)
      if (next.has(kind)) next.delete(kind)
      else next.add(kind)
      return next
    })
  }

  const clearFilters = () => setEnabledKinds(new Set(ALL_ENTRY_KINDS))

  const groups = useMemo(() => {
    const map = new Map<number, { items: TimelineEntry[]; total: number }>()
    for (const item of timeline) {
      const key = dayKey(item.occurredAt)
      const group = map.get(key) ?? { items: [], total: 0 }
      group.total += 1
      if (enabledKinds.has(item.kind)) group.items.push(item)
      map.set(key, group)
    }
    return [...map.entries()]
      .filter(([, group]) => group.items.length > 0)
      .sort((a, b) => b[0] - a[0])
  }, [timeline, enabledKinds])

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
      {LOG_BUTTONS.map(({ kind, label, icon }) => (
        <button
          key={kind}
          onClick={() => setSheet(kind)}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-surface-raised border border-DEFAULT text-fg text-sm font-semibold hover:bg-surface-high transition-colors"
        >
          <IconSlot em={16}>{icon(16)}</IconSlot>
          {label}
        </button>
      ))}
    </div>
  )

  // Phone: full-width bar of vertical emoji+label cards above the tab bar.
  const mobileLogBar = (
    <div className="border-t border-DEFAULT bg-surface px-3 py-2.5">
      <div className="mx-auto max-w-4xl grid grid-cols-4 gap-2">
        {LOG_BUTTONS.map(({ kind, label, icon }) => (
          <button
            key={kind}
            onClick={() => setSheet(kind)}
            className="flex flex-col items-center gap-1 py-2.5 rounded-2xl bg-surface-raised border border-DEFAULT text-fg-muted hover:bg-surface-high transition-colors"
          >
            <IconSlot em={22}>{icon(22)}</IconSlot>
            <span className="text-[11px] font-bold">{label}</span>
          </button>
        ))}
      </div>
    </div>
  )

  const filterBar = (
    <div
      className="mx-auto w-full max-w-4xl px-4 sm:px-6 lg:px-10 py-2 overflow-x-auto no-scrollbar"
      role="group"
      aria-label="Filter entries by type"
    >
      <div className="flex items-center gap-2 w-max">
        {LOG_BUTTONS.map(({ kind, label, icon }) => {
          const enabled = enabledKinds.has(kind)
          return (
            <button
              key={kind}
              type="button"
              aria-pressed={enabled}
              onClick={() => toggleKind(kind)}
              className={`inline-flex items-center gap-1.5 px-3 py-2 rounded-full border text-xs font-bold transition-colors ${
                enabled
                  ? 'bg-accent-soft border-accent text-accent-text'
                  : 'bg-surface-raised border-DEFAULT text-fg-faint'
              }`}
            >
              <span className={`inline-flex items-center justify-center w-5 h-5 ${enabled ? '' : 'opacity-35'}`}>
                {icon(16)}
              </span>
              {label}
            </button>
          )
        })}
        {filtersActive && (
          <button
            type="button"
            onClick={clearFilters}
            className="inline-flex items-center gap-1 px-3 py-2 rounded-full border border-dashed border-strong text-fg-muted text-xs font-bold hover:text-fg transition-colors"
          >
            <span aria-hidden="true">×</span>
            Clear
          </button>
        )}
      </div>
    </div>
  )

  return (
    <AppShell
      title="Home"
      subtitle={subtitle}
      headerRight={desktopLogButtons}
      subheader={filterBar}
      showProfileSwitcher
      bottomBar={mobileLogBar}
    >
      {loading ? (
        <p className="text-fg-faint text-sm py-8">Loading…</p>
      ) : timeline.length === 0 ? (
        <div className="py-24 text-center">
          <div className="text-5xl mb-4">🩺</div>
          <p className="text-fg font-semibold">Nothing logged yet</p>
          <p className="text-fg-muted text-sm mt-1">Tap a log button to add your first entry.</p>
        </div>
      ) : groups.length === 0 ? (
        <div className="py-24 text-center">
          <p className="text-fg font-semibold">No matching entries</p>
          <p className="text-fg-muted text-sm mt-1">Turn an entry type back on or clear the filter.</p>
          <button type="button" onClick={clearFilters} className="mt-3 text-sm font-bold text-accent-text">
            Clear filter
          </button>
          {hasMore && enabledKinds.size > 0 && <div ref={sentinel} className="h-px" />}
          {loadingMore && <p className="text-fg-faint text-sm text-center py-4">Loading…</p>}
        </div>
      ) : (
        <div className="space-y-7">
          {groups.map(([key, group]) => (
            <div key={key}>
              <div className="flex items-baseline justify-between mb-3">
                <span className="ds-eyebrow">{formatDayLabel(key)}</span>
                <span className="text-[11px] text-fg-faint font-semibold">
                  {filtersActive
                    ? `${group.items.length} of ${group.total}`
                    : `${group.total} entr${group.total === 1 ? 'y' : 'ies'}`}
                </span>
              </div>
              <div className="relative pl-[22px]">
                <span className="absolute left-[7px] top-1.5 bottom-1.5 w-px bg-[var(--border)]" />
                {group.items.map((item) => (
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
      {sheet === 'medicine' && <AddMedicineSheet onClose={() => setSheet(null)} onSave={addMedicine} />}
    </AppShell>
  )
}
