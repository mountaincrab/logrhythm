package com.mountaincrab.logrhythm

import com.mountaincrab.logrhythm.data.local.entity.MedicationEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntryEntity
import com.mountaincrab.logrhythm.data.model.MedicationForm
import com.mountaincrab.logrhythm.data.model.ResolvedDose
import com.mountaincrab.logrhythm.data.model.mergeMedicationEntries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Folding a day's doses into one row per medication, for the grouped home timeline.
 * Keep in step with `mergeMedicationEntries` in webapp/src/lib/medications.ts.
 */
class MergedMedicationTest {

    private val pentasa = MedicationEntity(
        id = "pentasa",
        name = "Pentasa",
        form = MedicationForm.TABLET,
        doseAmount = "1",
        doseUnit = "g",
    )
    private val inhaler = MedicationEntity(
        id = "inhaler",
        name = "Inhaler",
        form = MedicationForm.FOAM,
        doseAmount = "1",
        doseUnit = "puff",
    )

    private fun dose(
        id: String,
        at: Long,
        medication: MedicationEntity?,
        quantity: String = "1",
        notes: String? = null,
        medicationId: String = medication?.id ?: "gone",
    ) = ResolvedDose(
        entry = MedicationEntryEntity(
            id = id,
            medicationId = medicationId,
            quantity = quantity,
            occurredAt = at,
            notes = notes,
        ),
        medication = medication,
    )

    @Test
    fun sameMedicationMergesIntoOneRowWithEveryTime() {
        // Two Pentasa tablets morning and night: one row, two times, one running total.
        val rows = mergeMedicationEntries(
            listOf(
                dose("a", 8_00, pentasa, quantity = "2"),
                dose("b", 20_00, pentasa, quantity = "2"),
            ),
        )
        val row = rows.single()
        assertEquals("Pentasa", row.name)
        assertEquals(MedicationForm.TABLET, row.form)
        assertEquals("4 × 1g", row.amountText)
        // Forwards in time, and each time still points at the entry it came from.
        assertEquals(listOf(8_00L, 20_00L), row.occurrences.map { it.occurredAt })
        assertEquals(listOf("a", "b"), row.occurrences.map { it.entryId })
    }

    @Test
    fun differentMedicationsStayApartAndRunNewestFirst() {
        val rows = mergeMedicationEntries(
            listOf(
                dose("a", 8_00, pentasa, quantity = "2"),
                dose("b", 21_00, inhaler, quantity = "1"),
            ),
        )
        // The inhaler was the last dose taken, so its row leads — the feed is newest-first.
        assertEquals(listOf("Inhaler", "Pentasa"), rows.map { it.name })
    }

    @Test
    fun aSingleDoseKeepsItsQuantityExactlyAsTyped() {
        // Nothing to add up, so merging must not rewrite what the one dose said.
        val row = mergeMedicationEntries(listOf(dose("a", 8_00, inhaler, quantity = "half"))).single()
        assertEquals("half × 1puff", row.amountText)
    }

    @Test
    fun aQuantityWithNoNumberInItStillCountsAsOneDose() {
        // The same rule doseUnits totals by: there is no number to add, so the dose is one unit.
        val row = mergeMedicationEntries(
            listOf(
                dose("a", 8_00, inhaler, quantity = "half"),
                dose("b", 20_00, inhaler, quantity = "1"),
            ),
        ).single()
        assertEquals("2 × 1puff", row.amountText)
    }

    @Test
    fun blankQuantitiesShowJustTheStrength() {
        val row = mergeMedicationEntries(listOf(dose("a", 8_00, pentasa, quantity = ""))).single()
        assertEquals("1g", row.amountText)
    }

    @Test
    fun dosesOfAMedicationThatNoLongerResolvesStillMergeOnTheirId() {
        val rows = mergeMedicationEntries(
            listOf(
                dose("a", 8_00, medication = null, medicationId = "gone"),
                dose("b", 20_00, medication = null, medicationId = "gone"),
            ),
        )
        val row = rows.single()
        assertEquals("Medication", row.name)
        assertNull(row.form)
        assertEquals(2, row.occurrences.size)
    }

    @Test
    fun notesStayAttachedToTheDoseThatCarriedThem() {
        val row = mergeMedicationEntries(
            listOf(
                dose("a", 8_00, pentasa, notes = "with food"),
                dose("b", 20_00, pentasa, notes = "   "),
            ),
        ).single()
        assertEquals(listOf("with food", null), row.occurrences.map { it.notes })
    }
}
