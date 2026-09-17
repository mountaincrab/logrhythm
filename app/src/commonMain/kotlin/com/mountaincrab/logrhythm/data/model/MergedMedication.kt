package com.mountaincrab.logrhythm.data.model

import com.mountaincrab.logrhythm.data.local.entity.MedicationEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.dose

/** What a row falls back to when the catalog row a dose points at has vanished entirely. */
const val UNRESOLVED_MEDICATION = "Medication"

/** One dose in a merged row: which entry recorded it, when, and anything typed on it. */
data class MedicationOccurrence(
    val entryId: String,
    val occurredAt: Long,
    val notes: String? = null,
)

/**
 * A recorded dose with the catalog row it reads its name, form and strength from — null only
 * if the definition has vanished, which archiving rather than deleting is designed to prevent.
 */
data class ResolvedDose(
    val entry: MedicationEntryEntity,
    val medication: MedicationEntity?,
)

/**
 * A day's doses of one medication, folded into a single row: the medication once, how much
 * of it in total, and the time of each dose.
 *
 * Two Pentasa doses are two entries — deleting or editing one still happens on that entry,
 * which is why [occurrences] keeps the entry ids rather than just the times.
 */
data class MergedMedicationRow(
    val key: String,
    val name: String,
    /** Null when the definition can't be resolved — there's no form to draw then. */
    val form: MedicationForm?,
    /** How much the row adds up to, e.g. "4 × 1g" — blank for a medication with no strength. */
    val amountText: String,
    val occurrences: List<MedicationOccurrence>,
)

/**
 * Fold a day's doses into one row per medication, for the grouped home timeline.
 *
 * Doses merge on their `medicationId` — the catalog row is a live reference, so two doses are
 * the same medication however it has been renamed since, and a dose never carries a drug name
 * of its own to merge on instead.
 *
 * Quantities add up the way [doseUnits] already counts them: a blank or non-numeric quantity
 * is one unit, because there is no number to add. A row standing for a single dose keeps that
 * dose's quantity exactly as typed, so merging never rewrites what one dose said.
 *
 * Rows come back newest-dosed first, matching the feed around them, while the times inside a
 * row run forwards — the row is that medication's day, read left to right.
 * Mirror of `mergeMedicationEntries` in webapp/src/lib/medications.ts.
 */
fun mergeMedicationEntries(doses: List<ResolvedDose>): List<MergedMedicationRow> {
    val accumulators = LinkedHashMap<String, MergedMedicationAccumulator>()

    doses.forEach { resolved ->
        val key = "med:${resolved.entry.medicationId}"
        val accumulator = accumulators.getOrPut(key) {
            MergedMedicationAccumulator(
                key = key,
                name = resolved.medication?.name ?: UNRESOLVED_MEDICATION,
                form = resolved.medication?.form,
                dose = resolved.medication?.dose.orEmpty(),
            )
        }
        accumulator.totalQuantity += parseAmount(resolved.entry.quantity) ?: 1.0
        resolved.entry.quantity.trim().takeIf { it.isNotEmpty() }?.let { accumulator.quantities += it }
        accumulator.occurrences += MedicationOccurrence(
            entryId = resolved.entry.id,
            occurredAt = resolved.entry.occurredAt,
            notes = resolved.entry.notes?.takeIf { it.isNotBlank() },
        )
    }

    return accumulators.values
        .map { accumulator ->
            val quantityText = when {
                accumulator.quantities.isEmpty() -> ""
                accumulator.occurrences.size == 1 -> accumulator.quantities.first()
                else -> formatMedicationValue(accumulator.totalQuantity)
            }
            MergedMedicationRow(
                key = accumulator.key,
                name = accumulator.name,
                form = accumulator.form,
                amountText = formatDoseAmount(quantityText, accumulator.dose),
                occurrences = accumulator.occurrences.sortedBy { it.occurredAt },
            )
        }
        .sortedWith(
            compareByDescending<MergedMedicationRow> { row -> row.occurrences.maxOf { it.occurredAt } }
                .thenBy { it.name }
        )
}

private class MergedMedicationAccumulator(
    val key: String,
    val name: String,
    val form: MedicationForm?,
    val dose: String,
    var totalQuantity: Double = 0.0,
    val quantities: MutableList<String> = mutableListOf(),
    val occurrences: MutableList<MedicationOccurrence> = mutableListOf(),
)
