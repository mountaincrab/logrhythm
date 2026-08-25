package com.mountaincrab.logrhythm

import com.mountaincrab.logrhythm.data.local.entity.FoodEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.NoteEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryEntity
import com.mountaincrab.logrhythm.data.repository.TimelineEntry
import com.mountaincrab.logrhythm.ui.home.DayGroup
import com.mountaincrab.logrhythm.ui.home.EntryTypeFilter
import com.mountaincrab.logrhythm.ui.home.filterDays
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFilterTest {

    @Test
    fun filterDays_keepsOnlySelectedTypesAndDropsEmptyDays() {
        val today = LocalDate.of(2026, 8, 25)
        val yesterday = today.minusDays(1)
        val days = listOf(
            DayGroup(today, listOf(poop("poop"), food("food"))),
            DayGroup(yesterday, listOf(note("note"), medicine("medicine"))),
        )
        val poopAndNote = EntryTypeFilter.Poop.bit or EntryTypeFilter.Note.bit

        val result = filterDays(days, poopAndNote)

        assertEquals(listOf(today, yesterday), result.map { it.date })
        assertEquals(listOf("poop"), result[0].entries.map { it.id })
        assertEquals(listOf("note"), result[1].entries.map { it.id })
    }

    @Test
    fun filterDays_removesDaysWithoutASelectedEntry() {
        val today = LocalDate.of(2026, 8, 25)
        val yesterday = today.minusDays(1)
        val days = listOf(
            DayGroup(today, listOf(food("food"))),
            DayGroup(yesterday, listOf(note("note"))),
        )

        val result = filterDays(days, EntryTypeFilter.Note.bit)

        assertEquals(listOf(yesterday), result.map { it.date })
        assertEquals(listOf("note"), result.single().entries.map { it.id })
    }

    @Test
    fun filterDays_returnsNoDaysWhenNothingIsSelected() {
        val days = listOf(DayGroup(LocalDate.of(2026, 8, 25), listOf(poop("poop"))))

        assertTrue(filterDays(days, selectedMask = 0).isEmpty())
    }

    private fun poop(id: String) = TimelineEntry.Poop(
        PoopEntryEntity(id = id, occurredAt = 1L, blood = 1),
    )

    private fun food(id: String) = TimelineEntry.Food(
        FoodEntryEntity(id = id, occurredAt = 1L, items = "Lunch"),
    )

    private fun note(id: String) = TimelineEntry.Note(
        NoteEntryEntity(id = id, occurredAt = 1L, content = "Note"),
    )

    private fun medicine(id: String) = TimelineEntry.Medication(
        MedicationEntryEntity(id = id, medicationId = "med", occurredAt = 1L),
    )
}
