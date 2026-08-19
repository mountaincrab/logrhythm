package com.mountaincrab.logrhythm

import com.mountaincrab.logrhythm.data.model.doseUnits
import com.mountaincrab.logrhythm.data.model.formatMedicationValue
import com.mountaincrab.logrhythm.data.model.parseAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Totalling doses into units, for the Trends medication rows. Both a medication's strength
 * and a dose's quantity are free text, so this is where the two apps agree on what counts as
 * a number — keep it in step with webapp/src/lib/medications.ts.
 */
class MedicationTotalsTest {

    @Test
    fun parseAmount_readsNumbersAndRejectsTheRest() {
        assertEquals(1.0, parseAmount("1")!!, 0.0001)
        assertEquals(0.5, parseAmount(" 0.5 ")!!, 0.0001)
        // Someone typing on a comma-decimal keyboard still means half a gram.
        assertEquals(1.5, parseAmount("1,5")!!, 0.0001)
        assertNull(parseAmount(""))
        assertNull(parseAmount("   "))
        assertNull(parseAmount("1 puff"))
        assertNull(parseAmount("two"))
        assertNull(parseAmount("-1"))
    }

    @Test
    fun doseUnits_multipliesQuantityByStrength() {
        // Two 1g tablets is 2g; four of them across the day is 4g.
        assertEquals(2.0, doseUnits("2", 1.0), 0.0001)
        assertEquals(0.1, doseUnits("2", 0.05), 0.0001)
        // A blank or unreadable quantity is one unit — the dose happened either way.
        assertEquals(1.0, doseUnits("", 1.0), 0.0001)
        assertEquals(1.0, doseUnits("one", 1.0), 0.0001)
    }

    @Test
    fun doseUnits_countsBareUnitsWhenTheStrengthIsNotNumeric() {
        // "1 puff" has no number to multiply, so the row counts units taken instead.
        assertEquals(2.0, doseUnits("2", null), 0.0001)
        assertEquals(1.0, doseUnits("", null), 0.0001)
    }

    @Test
    fun formatMedicationValue_dropsTrailingZeros() {
        assertEquals("27", formatMedicationValue(27.0))
        assertEquals("0.5", formatMedicationValue(0.5))
        assertEquals("3.86", formatMedicationValue(3.857))
        assertEquals("3.9", formatMedicationValue(3.857, decimals = 1))
        assertEquals("16.4", formatMedicationValue(16.428, decimals = 1))
        assertEquals("100", formatMedicationValue(100.0, decimals = 1))
        assertEquals("0", formatMedicationValue(0.0))
        assertEquals("0.05", formatMedicationValue(0.05))
    }
}
