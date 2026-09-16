import { formatFoodNumber, MergedFoodRow } from '../lib/food'
import { formatTime } from '../lib/dates'

/**
 * A day's loggings of one food as a single row inside the grouped food box: the food once,
 * the total quantity and component amounts, then every time it was logged.
 *
 * Each time is its own target rather than the row being one: the row stands for several
 * entries, and a tap has to land on the one being corrected. Editing and deleting stay
 * exactly where they are for every other entry — on the entry itself.
 * Mirror of `MergedFoodTimelineRow` on Android.
 */
export default function MergedFoodTimelineRow({
  row,
  onOpenEntry,
}: {
  row: MergedFoodRow
  onOpenEntry: (entryId: string) => void
}) {
  return (
    <div className="flex flex-wrap items-center gap-x-2 gap-y-1 px-3.5 py-2.5 text-sm">
      <span className="text-lg leading-none shrink-0" aria-hidden>{row.icon}</span>
      <span className="font-semibold text-fg">{row.name}</span>
      {row.showQuantity && (
        <span className="text-accent-text font-bold">× {formatFoodNumber(row.totalQuantity)}</span>
      )}
      {row.componentTotals.length > 0 && (
        // Bracketed as one unit: these are what the whole row adds up to, not a
        // qualifier on the last time in the list beside them.
        <span className="text-[11px] text-fg-muted font-medium">
          ({row.componentTotals.map((total) => `${total.name} ${formatFoodNumber(total.amount)} ${total.unit}`).join(' · ')})
        </span>
      )}
      {row.occurrences.map((occurrence, index) => (
        <button
          key={occurrence.entryId}
          type="button"
          onClick={() => onOpenEntry(occurrence.entryId)}
          className="text-[13px] font-bold font-mono tabular-nums rounded px-1 -mx-1 hover:bg-surface-high transition-colors"
        >
          {formatTime(occurrence.occurredAt)}
          {index < row.occurrences.length - 1 && ','}
        </button>
      ))}
    </div>
  )
}
