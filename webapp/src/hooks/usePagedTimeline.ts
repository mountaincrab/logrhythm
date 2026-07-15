import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  collection, query, where, orderBy, limit, onSnapshot,
} from 'firebase/firestore'
import { db } from '../firebase'
import { PoopEntry, FoodEntry, NoteEntry, TimelineEntry } from '../types'
import { mapPoop, mapFood, mapNote } from './useEntries'

const PAGE_SIZE = 50

interface Bucket<T> {
  items: T[]
  /** Whether the query returned a full page — i.e. more docs may exist beyond it. */
  full: boolean
}

const EMPTY: Bucket<never> = { items: [], full: false }

/**
 * Paged, live Home timeline built by merging the three entry collections.
 *
 * Firestore has no cross-collection query, so each collection is fetched
 * independently (ordered newest-first) and merged client-side. Rather than
 * juggling three `startAfter` cursors, we grow a single shared `limit` and let
 * each collection's `onSnapshot` keep its slice live — so the whole loaded
 * range stays reactive (new entries, edits and deletes all re-emit), and
 * `loadMore` just widens the window by one page.
 *
 * Relies on server-side `profileId` / `isDeleted` filtering, which is only
 * correct once every doc carries both fields (see the Firestore backfill).
 * Filtering server-side keeps a page of N fetched docs a page of N usable
 * rows, so there are no post-filter short pages to compensate for.
 */
export function usePagedTimeline(userId: string, profileId: string) {
  const [count, setCount] = useState(PAGE_SIZE)
  const [poop, setPoop] = useState<Bucket<PoopEntry>>(EMPTY)
  const [food, setFood] = useState<Bucket<FoodEntry>>(EMPTY)
  const [note, setNote] = useState<Bucket<NoteEntry>>(EMPTY)
  const [ready, setReady] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  // Prevents a burst of scroll events from queuing multiple page bumps before
  // the next snapshot arrives; cleared once the wider page has been delivered.
  const busy = useRef(false)

  // Reset to the first page whenever the account or active profile changes.
  useEffect(() => {
    setReady(false)
    setLoadingMore(false)
    busy.current = false
    setCount(PAGE_SIZE)
  }, [userId, profileId])

  useEffect(() => {
    const delivered = { poop: false, food: false, note: false }
    const markDelivered = (key: keyof typeof delivered) => {
      delivered[key] = true
      if (delivered.poop && delivered.food && delivered.note) {
        setReady(true)
        setLoadingMore(false)
        busy.current = false
      }
    }
    const subscribe = <T,>(
      name: string,
      mapper: (id: string, d: Record<string, unknown>) => T,
      set: (b: Bucket<T>) => void,
      key: keyof typeof delivered,
    ) => {
      const q = query(
        collection(db, 'users', userId, name),
        where('profileId', '==', profileId),
        where('isDeleted', '==', false),
        orderBy('occurredAt', 'desc'),
        limit(count),
      )
      return onSnapshot(q, (snap) => {
        set({ items: snap.docs.map((d) => mapper(d.id, d.data())), full: snap.size === count })
        markDelivered(key)
      })
    }
    const unsubs = [
      subscribe('poop_entries', mapPoop, setPoop, 'poop'),
      subscribe('food_entries', mapFood, setFood, 'food'),
      subscribe('note_entries', mapNote, setNote, 'note'),
    ]
    return () => unsubs.forEach((u) => u())
  }, [userId, profileId, count])

  const merged = useMemo<TimelineEntry[]>(() => {
    return [
      ...poop.items.map((entry) => ({ kind: 'poop' as const, occurredAt: entry.occurredAt, entry })),
      ...food.items.map((entry) => ({ kind: 'food' as const, occurredAt: entry.occurredAt, entry })),
      ...note.items.map((entry) => ({ kind: 'note' as const, occurredAt: entry.occurredAt, entry })),
    ].sort((a, b) => b.occurredAt - a.occurredAt)
  }, [poop.items, food.items, note.items])

  // Only the newest `count` merged rows are shown; the rest are the merge
  // over-fetch (up to 3×count fetched to yield one page).
  const timeline = useMemo(() => merged.slice(0, count), [merged, count])

  // More to show if we're hiding over-fetched rows, or any collection hit its
  // page limit (docs may exist beyond what we fetched).
  const hasMore = merged.length > count || poop.full || food.full || note.full

  const loadMore = useCallback(() => {
    if (busy.current) return
    busy.current = true
    setLoadingMore(true)
    setCount((c) => c + PAGE_SIZE)
  }, [])

  return { timeline, loading: !ready, hasMore, loadingMore, loadMore }
}
