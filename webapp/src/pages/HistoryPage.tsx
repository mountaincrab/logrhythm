import { useMemo, useState } from 'react'
import { CalendarDays, TrendingUp, ChevronLeft, ChevronRight } from 'lucide-react'
import AppShell from '../components/AppShell'
import { useEntriesContext } from '../contexts/EntriesContext'
import { useMedicationsContext } from '../contexts/MedicationsContext'
import { PoopEntry, FoodEntry, Medication, MedicationEntry } from '../types'
import { ratingColor, RATING_COLORS } from '../lib/ratings'
import {
  doseUnits, formatDose, formatMedicationValue, formEmoji, medicationSeriesColor, parseAmount,
} from '../lib/medications'
import { dayKey } from '../lib/dates'

type Tab = 'calendar' | 'trends'

interface DayStat { count: number; worst: number }

function buildPoopByDay(poops: PoopEntry[]): Map<number, DayStat> {
  const map = new Map<number, DayStat>()
  for (const p of poops) {
    const key = dayKey(p.occurredAt)
    const cur = map.get(key) ?? { count: 0, worst: 0 }
    cur.count += 1
    cur.worst = Math.max(cur.worst, p.blood)
    map.set(key, cur)
  }
  return map
}

export default function HistoryPage() {
  const { poops, foods, loading } = useEntriesContext()
  const [tab, setTab] = useState<Tab>('calendar')

  const poopByDay = useMemo(() => buildPoopByDay(poops), [poops])

  return (
    <AppShell
      title="History"
      subtitle={loading ? undefined : tab === 'calendar' ? 'Worst rating per day' : 'Trends over time'}
      showProfileSwitcher
    >
      <div className="flex bg-surface border border-DEFAULT rounded-xl p-[3px] mb-5 max-w-xs">
        {(['calendar', 'trends'] as const).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={
              'flex-1 py-2 rounded-[9px] text-[13px] font-bold inline-flex items-center justify-center gap-1.5 transition-colors ' +
              (tab === t ? 'bg-accent text-accent-fg' : 'text-fg-muted')
            }
          >
            {t === 'calendar' ? <CalendarDays size={15} /> : <TrendingUp size={15} />}
            {t === 'calendar' ? 'Calendar' : 'Trends'}
          </button>
        ))}
      </div>

      {tab === 'calendar' ? <CalendarView poopByDay={poopByDay} /> : <TrendsView poops={poops} foods={foods} />}
    </AppShell>
  )
}

const WEEKDAYS = ['M', 'T', 'W', 'T', 'F', 'S', 'S']
const MONTH_NAMES = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']

function CalendarView({ poopByDay }: { poopByDay: Map<number, DayStat> }) {
  const now = new Date()
  const [anchor, setAnchor] = useState(() => new Date(now.getFullYear(), now.getMonth(), 1))
  const todayKey = dayKey(Date.now())

  const year = anchor.getFullYear()
  const month = anchor.getMonth()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const mondayIndex = (new Date(year, month, 1).getDay() + 6) % 7

  const cells: ({ day: number; key: number } | null)[] = []
  for (let i = 0; i < mondayIndex; i++) cells.push(null)
  for (let d = 1; d <= daysInMonth; d++) cells.push({ day: d, key: dayKey(new Date(year, month, d).getTime()) })

  const loggedDays = cells.filter((c) => c && poopByDay.has(c.key)).length

  return (
    <>
      <div className="pb-3 flex items-center gap-2">
        <span className="text-lg font-extrabold tracking-tightish flex-1">{MONTH_NAMES[month]} {year}</span>
        <span className="text-[11px] text-fg-faint font-semibold mr-1">{loggedDays} day{loggedDays === 1 ? '' : 's'} logged</span>
        <div className="flex gap-1.5">
          <button onClick={() => setAnchor(new Date(year, month - 1, 1))} className="w-9 h-9 rounded-xl bg-surface-raised border border-DEFAULT flex items-center justify-center text-fg-muted">
            <ChevronLeft size={18} />
          </button>
          <button onClick={() => setAnchor(new Date(year, month + 1, 1))} className="w-9 h-9 rounded-xl bg-surface-raised border border-DEFAULT flex items-center justify-center text-fg-muted">
            <ChevronRight size={18} />
          </button>
        </div>
      </div>

      <div className="mb-3 bg-surface-raised border border-DEFAULT rounded-2xl p-3.5">
        <div className="grid grid-cols-7 gap-1.5 mb-1.5">
          {WEEKDAYS.map((d, i) => (
            <div key={i} className="text-center text-[10px] font-extrabold text-fg-faint tracking-wide">{d}</div>
          ))}
        </div>
        <div className="grid grid-cols-7 gap-1.5">
          {cells.map((c, i) => {
            if (!c) return <div key={i} className="aspect-square" />
            const stat = poopByDay.get(c.key)
            const isToday = c.key === todayKey
            const isFuture = c.key > todayKey
            const rc = stat ? ratingColor(stat.worst) : null
            return (
              <div
                key={i}
                className="aspect-square rounded-xl flex flex-col items-center justify-center gap-px"
                style={{
                  background: rc ? rc.bg : isFuture ? 'transparent' : 'var(--surface)',
                  color: rc ? rc.fg : isFuture ? 'var(--fg-disabled)' : 'var(--fg-muted)',
                  boxShadow: isToday ? 'inset 0 0 0 2px var(--accent)' : undefined,
                }}
              >
                <span className="text-[13px] font-bold leading-none">{c.day}</span>
                {stat && <span className="text-[9px] font-semibold opacity-70 leading-none">{stat.count}×</span>}
              </div>
            )
          })}
        </div>
      </div>

      <div className="px-3.5 py-3 bg-surface-raised border border-DEFAULT rounded-xl flex items-center gap-2 text-[11px] text-fg-muted">
        <span className="mr-auto">Worst rating per day</span>
        {[1, 2, 3, 4, 5].map((n) => (
          <span key={n} className="flex items-center gap-1">
            <span className="w-3.5 h-3.5 rounded" style={{ background: RATING_COLORS[n].bg }} />
            <span className="font-bold text-fg">{n}</span>
          </span>
        ))}
      </div>
    </>
  )
}

const RANGES: { days: number; label: string }[] = [
  { days: 7, label: '7d' },
  { days: 30, label: '30d' },
  { days: 90, label: '90d' },
]

function TrendsView({ poops, foods }: { poops: PoopEntry[]; foods: FoodEntry[] }) {
  const [days, setDays] = useState(30)

  const series = useMemo(() => {
    const poopByDay = buildPoopByDay(poops)
    const today = dayKey(Date.now())
    const out: { key: number; rating: number | null; count: number }[] = []
    for (let i = days - 1; i >= 0; i--) {
      const key = today - i * 86_400_000
      const stat = poopByDay.get(key)
      out.push({ key, rating: stat ? stat.worst : null, count: stat ? stat.count : 0 })
    }
    return out
  }, [poops, days])

  const rated = series.filter((s) => s.rating != null) as { rating: number }[]
  const avgRating = rated.length ? rated.reduce((a, b) => a + b.rating, 0) / rated.length : 0
  const totalPoops = series.reduce((a, b) => a + b.count, 0)
  const avgFreq = totalPoops / days
  const maxCount = Math.max(1, ...series.map((s) => s.count))

  const suspects = useMemo(() => computeSuspects(poops, foods, days), [poops, foods, days])

  // Blood-rating sparkline geometry
  const w = 320, h = 110, pad = 6
  const points = series.map((s, i) => {
    if (s.rating == null) return null
    const x = pad + (i / Math.max(1, series.length - 1)) * (w - pad * 2)
    const y = pad + (1 - (s.rating - 1) / 4) * (h - pad * 2)
    return { x, y, rating: s.rating }
  })
  const segments: { x: number; y: number; rating: number }[][] = []
  let cur: { x: number; y: number; rating: number }[] = []
  for (const p of points) {
    if (!p) { if (cur.length) segments.push(cur); cur = [] } else cur.push(p)
  }
  if (cur.length) segments.push(cur)

  return (
    <>
      <div className="flex bg-surface border border-DEFAULT rounded-xl p-[3px] mb-4 max-w-xs">
        {RANGES.map((r) => (
          <button
            key={r.days}
            onClick={() => setDays(r.days)}
            className={'flex-1 py-2 rounded-[9px] text-xs font-bold transition-colors ' + (days === r.days ? 'bg-surface-high text-fg' : 'text-fg-muted')}
          >
            {r.label}
          </button>
        ))}
      </div>

      <div className="grid lg:grid-cols-2 gap-3 mb-3">
      {/* Blood rating */}
      <div className="bg-surface-raised border border-DEFAULT rounded-2xl p-4">
        <div className="ds-eyebrow mb-1">Blood rating</div>
        <div className="flex items-baseline gap-2 mb-2">
          <span className="text-[32px] font-black tracking-tight leading-none">{rated.length ? avgRating.toFixed(1) : '—'}</span>
          <span className="text-xs text-fg-muted">avg over {rated.length} day{rated.length === 1 ? '' : 's'}</span>
        </div>
        {rated.length === 0 ? (
          <p className="text-sm text-fg-muted py-6 text-center">No poops logged in this range.</p>
        ) : (
          <svg viewBox={`0 0 ${w} ${h + 16}`} className="w-full h-32 block">
            <defs>
              <linearGradient id="rfill" x1="0" x2="0" y1="0" y2="1">
                <stop offset="0%" stopColor="var(--accent)" stopOpacity="0.30" />
                <stop offset="100%" stopColor="var(--accent)" stopOpacity="0" />
              </linearGradient>
            </defs>
            {[1, 2, 3, 4, 5].map((n) => {
              const y = pad + (1 - (n - 1) / 4) * (h - pad * 2)
              return (
                <g key={n}>
                  <line x1={pad} x2={w - pad} y1={y} y2={y} stroke="var(--border-subtle)" strokeWidth="1" />
                  <text x={w - pad + 2} y={y + 3} fontSize="9" fill="var(--fg-faint)" className="font-mono">{n}</text>
                </g>
              )
            })}
            {segments.map((seg, si) => {
              const line = seg.map((p, i) => `${i ? 'L' : 'M'}${p.x} ${p.y}`).join(' ')
              const area = `${line} L${seg[seg.length - 1].x} ${h - pad} L${seg[0].x} ${h - pad} Z`
              return (
                <g key={si}>
                  <path d={area} fill="url(#rfill)" />
                  <path d={line} fill="none" stroke="var(--accent)" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
                </g>
              )
            })}
            {points.map((p, i) => p && (
              <circle key={i} cx={p.x} cy={p.y} r="2.6" fill={ratingColor(p.rating).bg} stroke="var(--bg)" strokeWidth="1.5" />
            ))}
            <text x={pad} y={h + 12} fill="var(--fg-faint)" fontSize="9" className="font-mono">{days}d ago</text>
            <text x={w - pad} y={h + 12} textAnchor="end" fill="var(--fg-faint)" fontSize="9" className="font-mono">today</text>
          </svg>
        )}
      </div>

      {/* Frequency */}
      <div className="bg-surface-raised border border-DEFAULT rounded-2xl p-4">
        <div className="ds-eyebrow mb-1">Poops per day</div>
        <div className="flex items-baseline gap-2 mb-2">
          <span className="text-[32px] font-black tracking-tight leading-none">{avgFreq.toFixed(1)}</span>
          <span className="text-xs text-fg-muted">avg / day · {totalPoops} total</span>
        </div>
        <div className="flex gap-[2px] items-end h-[70px]">
          {series.map((s, i) => (
            <div
              key={i}
              className="flex-1 rounded-[2px]"
              style={{ height: `${Math.max(2, (s.count / maxCount) * 100)}%`, background: s.count >= 3 ? 'var(--warning)' : 'var(--accent)' }}
            />
          ))}
        </div>
        <div className="mt-1.5 flex justify-between text-[9px] text-fg-faint font-mono">
          <span>{days}d ago</span><span>today</span>
        </div>
      </div>
      </div>

      <MedicationTotals days={days} />

      {/* Food suspects */}
      <div className="bg-surface-raised border border-DEFAULT rounded-2xl p-4">
        <div className="ds-eyebrow mb-1">Food suspects</div>
        <p className="text-xs text-fg-muted mb-2.5 leading-snug">
          Foods eaten in the 24h before a rating ≥ 3, ranked by how often they showed up.
        </p>
        {suspects.length === 0 ? (
          <p className="text-sm text-fg-muted py-3">Not enough data yet — no high-rating days with food logged beforehand.</p>
        ) : (
          suspects.map((f) => (
            <div key={f.food} className="flex items-center gap-3 py-2.5 border-t border-subtle first:border-t-0">
              <div className="flex-1 min-w-0">
                <div className="text-[13px] font-bold capitalize truncate">{f.food}</div>
                <div className="text-[11px] text-fg-muted">{f.bad} of {f.total} times before a bad day</div>
              </div>
              <div className="text-[13px] font-extrabold text-danger-text tabular-nums">{f.bad}×</div>
            </div>
          ))
        )}
      </div>
    </>
  )
}

interface MedicationSeries {
  id: string
  name: string
  emoji: string
  /** The medication's strength as defined, e.g. "1g". */
  strength: string
  /** "g" / "mg" when the strength is numeric, "×" when it isn't — see buildMedicationSeries. */
  unit: string
  color: string
  /** One total per day of the range, oldest first. */
  daily: number[]
  total: number
  avgPerDay: number
  peak: number
}

/** Local midnights across the range, oldest first — the medication rows' shared x-axis. */
function rangeDayKeys(days: number): number[] {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  d.setDate(d.getDate() - (days - 1))
  const keys: number[] = []
  for (let i = 0; i < days; i++) {
    keys.push(d.getTime())
    d.setDate(d.getDate() + 1)
  }
  return keys
}

/**
 * Daily totals per medication over the range.
 *
 * Doses carry only a quantity; the strength lives on the catalog row, so a dose is worth
 * `quantity × strength` and every medication is totalled in its own unit — grams of one drug
 * and milligrams of another are never added together. Rows keep the catalog's order, the same
 * order the Meds page uses, so a medication's colour never moves when the range changes or
 * when another medication drops out of it. Mirror of buildMedicationSeries in
 * HistoryViewModel.kt.
 */
function buildMedicationSeries(
  entries: MedicationEntry[],
  catalog: Medication[],
  days: number,
): MedicationSeries[] {
  if (catalog.length === 0) return []
  const dayKeys = rangeDayKeys(days)
  const indexByDay = new Map(dayKeys.map((k, i) => [k, i]))

  const byMedication = new Map<string, MedicationEntry[]>()
  for (const e of entries) {
    if (!indexByDay.has(dayKey(e.occurredAt))) continue
    const list = byMedication.get(e.medicationId)
    if (list) list.push(e)
    else byMedication.set(e.medicationId, [e])
  }

  const out: MedicationSeries[] = []
  catalog.forEach((med, index) => {
    const doses = byMedication.get(med.id)
    if (!doses) return
    const unitAmount = parseAmount(med.doseAmount)
    const daily = new Array<number>(dayKeys.length).fill(0)
    for (const dose of doses) {
      const i = indexByDay.get(dayKey(dose.occurredAt))
      if (i === undefined) continue
      daily[i] += doseUnits(dose.quantity, unitAmount)
    }
    const total = daily.reduce((a, b) => a + b, 0)
    if (total <= 0) return
    out.push({
      id: med.id,
      name: med.name,
      emoji: formEmoji(med.form),
      strength: formatDose(med.doseAmount, med.doseUnit),
      // A non-numeric strength has nothing to multiply by, so the row counts units taken.
      unit: unitAmount !== null ? med.doseUnit.trim() : '×',
      color: medicationSeriesColor(index),
      daily,
      total,
      avgPerDay: total / dayKeys.length,
      peak: Math.max(...daily),
    })
  })
  return out
}

/**
 * Total taken per medication over the range.
 *
 * Deliberately one row per medication rather than one stacked chart: grams of one drug and
 * milligrams of another can't share an axis, and a total across them would be a number that
 * means nothing. Each row is scaled to its own peak and labelled with its own unit, so the
 * comparison is a medication against its own history — which is the one that matters.
 */
function MedicationTotals({ days }: { days: number }) {
  const { medicationEntries } = useEntriesContext()
  const { medicationsById } = useMedicationsContext()

  // The archived-inclusive catalog, in its own order: a dose of a medication archived
  // mid-range still has to resolve its name and strength.
  const catalog = useMemo(() => [...medicationsById.values()], [medicationsById])
  const dayKeys = useMemo(() => rangeDayKeys(days), [days])
  const series = useMemo(
    () => buildMedicationSeries(medicationEntries, catalog, days),
    [medicationEntries, catalog, days],
  )

  const value = (s: MedicationSeries, v: number, decimals = 2) =>
    formatMedicationValue(v, decimals) + s.unit

  return (
    <div className="bg-surface-raised border border-DEFAULT rounded-2xl p-4 mb-3">
      <div className="ds-eyebrow mb-1">Medication · total taken</div>
      {series.length === 0 ? (
        <p className="text-sm text-fg-muted py-3">
          {catalog.length === 0
            ? 'No medications yet — add them on the Meds page and doses will total up here.'
            : 'No doses logged in this range.'}
        </p>
      ) : (
        <>
          {series.map((s) => (
            <div key={s.id} className="py-3 border-t border-subtle first:border-t-0 first:pt-1.5">
              <div className="flex items-baseline gap-1.5 pb-2">
                <span className="text-xs leading-none">{s.emoji}</span>
                <span className="text-[13px] font-bold tracking-tightish">{s.name}</span>
                <span className="text-[11px] text-fg-faint flex-1 truncate">{s.strength}</span>
                <span className="text-[13px] font-bold tabular-nums" style={{ color: s.color }}>
                  {value(s, s.total)}
                </span>
              </div>
              <div className="flex gap-[3px] items-end h-[34px]">
                {s.daily.map((v, i) => (
                  <div
                    key={i}
                    title={`${value(s, v)} on ${new Date(dayKeys[i]).toLocaleDateString()}`}
                    className="flex-1 rounded-[2px]"
                    style={{
                      height: v > 0 ? `${Math.max(6, (v / s.peak) * 100)}%` : '2px',
                      background: v > 0 ? s.color : 'var(--border)',
                    }}
                  />
                ))}
              </div>
              <div className="flex justify-between text-[9px] text-fg-faint font-mono pt-1.5">
                <span>{value(s, s.avgPerDay, 1)} / day avg</span>
                <span>peak {value(s, s.peak)}</span>
              </div>
            </div>
          ))}
          {/* Every row shares the range's time axis, so it's labelled once at the foot. */}
          <div className="flex justify-between text-[9px] text-fg-faint font-mono pt-1 border-t border-subtle mt-1">
            <span>{days}d ago</span><span>today</span>
          </div>
        </>
      )}
    </div>
  )
}

interface Suspect { food: string; bad: number; total: number }

function computeSuspects(poops: PoopEntry[], foods: FoodEntry[], days: number): Suspect[] {
  const cutoff = dayKey(Date.now()) - (days - 1) * 86_400_000
  const badPoops = poops.filter((p) => p.blood >= 3 && p.occurredAt >= cutoff)
  const rangeFoods = foods.filter((f) => f.occurredAt >= cutoff - 86_400_000)

  const tokenize = (items: string) =>
    items.split(',').map((s) => s.trim().toLowerCase()).filter((s) => s.length > 1)

  const total = new Map<string, number>()
  for (const f of rangeFoods) for (const t of new Set(tokenize(f.items))) total.set(t, (total.get(t) ?? 0) + 1)

  const bad = new Map<string, number>()
  for (const p of badPoops) {
    const windowStart = p.occurredAt - 86_400_000
    const eaten = new Set<string>()
    for (const f of rangeFoods) {
      if (f.occurredAt >= windowStart && f.occurredAt <= p.occurredAt) for (const t of tokenize(f.items)) eaten.add(t)
    }
    for (const t of eaten) bad.set(t, (bad.get(t) ?? 0) + 1)
  }

  return [...bad.entries()]
    .map(([food, b]) => ({ food, bad: b, total: total.get(food) ?? b }))
    .sort((a, b) => b.bad - a.bad)
    .slice(0, 6)
}
