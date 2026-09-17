import { MergedMedicationRow } from '../lib/medications'
import { formatTime } from '../lib/dates'
import { ENTRY_ICON_SIZES, MedicationFormIcon } from './MedicationIcons'

/**
 * A day's doses of one medication as a single row inside the grouped medicine box: the
 * medication once, what it adds up to, then every time it was taken.
 *
 * Each time is its own target rather than the row being one: the row stands for several
 * entries, and a tap has to land on the one being corrected. Editing and deleting stay
 * exactly where they are for every other entry — on the entry itself. Anything typed on a
 * dose follows underneath against its time, because merging must not swallow it.
 * Mirror of `MergedMedicationTimelineRow` on Android.
 */
export default function MergedMedicationTimelineRow({
  row,
  onOpenEntry,
}: {
  row: MergedMedicationRow
  onOpenEntry: (entryId: string) => void
}) {
  return (
    <div className="px-3.5 py-2.5 text-sm">
      <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
        {/* Name and form stay one unit — a bracket that wrapped away from what it qualifies
            would read as belonging to the dose amount instead. */}
        <span className="inline-flex items-center gap-1 font-semibold text-fg">
          {row.name}
          {row.form && (
            <span className="inline-flex items-center gap-px text-fg-faint">
              (<MedicationFormIcon form={row.form} size={ENTRY_ICON_SIZES.timelineFormIcon} />)
            </span>
          )}
        </span>
        {row.amountText && <span className="text-accent-text font-bold">{row.amountText}</span>}
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
      {/* Notes belong to one dose, not to the row, so each keeps the time that owns it. */}
      {row.occurrences.filter((occurrence) => occurrence.notes).map((occurrence) => (
        <div key={`${occurrence.entryId}-notes`} className="pt-1 text-fg-muted">
          <span className="font-mono tabular-nums">{formatTime(occurrence.occurredAt)}</span> · {occurrence.notes}
        </div>
      ))}
    </div>
  )
}
