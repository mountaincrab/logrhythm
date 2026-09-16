package com.mountaincrab.logrhythm

import com.mountaincrab.logrhythm.data.local.entity.FoodEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryLineEntity
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryWithLines
import com.mountaincrab.logrhythm.data.local.entity.FoodItemEntity
import com.mountaincrab.logrhythm.data.local.entity.ResolvedFoodEntryLine
import com.mountaincrab.logrhythm.data.local.entity.TrackedComponentEntity
import com.mountaincrab.logrhythm.data.model.mergeFoodEntries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Folding a day's food entries into one row per food, for the grouped home timeline.
 * Keep in step with `mergeFoodEntries` in webapp/src/lib/food.ts.
 */
class MergedFoodTest {

    private val caffeine = TrackedComponentEntity(id = "caffeine", name = "Caffeine", unit = "mg", sortOrder = 0)
    private val alcohol = TrackedComponentEntity(id = "alcohol", name = "Alcohol", unit = "UK units", sortOrder = 1)
    private val components = mapOf(caffeine.id to caffeine, alcohol.id to alcohol)

    private val tea = FoodItemEntity(id = "tea", name = "Tea", icon = "🫖", amount = "1", unit = "cup")
    private val beer = FoodItemEntity(id = "beer", name = "Beer", icon = "🍺", amount = "1", unit = "pint")

    private fun catalogueEntry(
        id: String,
        at: Long,
        item: FoodItemEntity,
        quantity: Double,
        amounts: Map<String, Double>,
    ) = FoodEntryWithLines(
        entry = FoodEntryEntity(id = id, occurredAt = at),
        lines = listOf(
            ResolvedFoodEntryLine(
                line = FoodEntryLineEntity(id = "$id-line", entryId = id, position = 0, foodItemId = item.id, quantity = quantity),
                foodItem = item,
                componentAmounts = amounts,
            ),
        ),
        componentsById = components,
    )

    private fun customEntry(id: String, at: Long, text: String, amounts: Map<String, Double> = emptyMap()) =
        FoodEntryWithLines(
            entry = FoodEntryEntity(id = id, occurredAt = at),
            lines = listOf(
                ResolvedFoodEntryLine(
                    line = FoodEntryLineEntity(id = "$id-line", entryId = id, position = 0, customText = text),
                    foodItem = null,
                    componentAmounts = amounts,
                ),
            ),
            componentsById = components,
        )

    @Test
    fun sameFoodMergesIntoOneRowWithEveryTime() {
        // Four cups of tea across a morning: one row, four times, one running total.
        val perCup = mapOf(caffeine.id to 75.0)
        val rows = mergeFoodEntries(
            listOf(
                catalogueEntry("a", 6_30, tea, 1.0, perCup),
                catalogueEntry("b", 7_30, tea, 1.0, perCup),
                catalogueEntry("c", 10_52, tea, 1.0, perCup),
                catalogueEntry("d", 11_48, tea, 1.0, perCup),
            ),
        )
        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("Tea", row.name)
        assertEquals("🫖", row.icon)
        assertEquals(4.0, row.totalQuantity, 0.0001)
        assertEquals(300.0, row.componentTotals.single().amount, 0.0001)
        assertEquals("mg", row.componentTotals.single().unit)
        // Forwards in time, and each time still points at the entry it came from.
        assertEquals(listOf(6_30L, 7_30L, 10_52L, 11_48L), row.occurrences.map { it.occurredAt })
        assertEquals(listOf("a", "b", "c", "d"), row.occurrences.map { it.entryId })
    }

    @Test
    fun quantityScalesEachLoggingsComponents() {
        // Two cups at once then one more: three cups' worth of caffeine.
        val rows = mergeFoodEntries(
            listOf(
                catalogueEntry("a", 8_00, tea, 2.0, mapOf(caffeine.id to 75.0)),
                catalogueEntry("b", 9_00, tea, 1.0, mapOf(caffeine.id to 75.0)),
            ),
        )
        val row = rows.single()
        assertEquals(3.0, row.totalQuantity, 0.0001)
        assertEquals(225.0, row.componentTotals.single().amount, 0.0001)
        assertEquals(2, row.occurrences.size)
    }

    @Test
    fun differentFoodsStayApartAndRunNewestFirst() {
        val rows = mergeFoodEntries(
            listOf(
                catalogueEntry("a", 8_00, tea, 1.0, mapOf(caffeine.id to 75.0)),
                catalogueEntry("b", 20_00, beer, 2.0, mapOf(alcohol.id to 2.0)),
            ),
        )
        // The beer was logged last, so its row leads — the feed around it is newest-first.
        assertEquals(listOf("Beer", "Tea"), rows.map { it.name })
        assertEquals(4.0, rows.first().componentTotals.single().amount, 0.0001)
    }

    @Test
    fun oneEntryListingAFoodTwiceIsStillOneLogging() {
        val entry = FoodEntryWithLines(
            entry = FoodEntryEntity(id = "a", occurredAt = 8_00),
            lines = listOf(
                ResolvedFoodEntryLine(
                    FoodEntryLineEntity(id = "l1", entryId = "a", position = 0, foodItemId = tea.id, quantity = 1.0),
                    tea,
                    mapOf(caffeine.id to 75.0),
                ),
                ResolvedFoodEntryLine(
                    FoodEntryLineEntity(id = "l2", entryId = "a", position = 1, foodItemId = tea.id, quantity = 2.0),
                    tea,
                    mapOf(caffeine.id to 75.0),
                ),
            ),
            componentsById = components,
        )
        val row = mergeFoodEntries(listOf(entry)).single()
        assertEquals(3.0, row.totalQuantity, 0.0001)
        assertEquals(225.0, row.componentTotals.single().amount, 0.0001)
        assertEquals(1, row.occurrences.size)
    }

    @Test
    fun customLinesMergeOnTheirTextAndKeepTheirOwnTotals() {
        val rows = mergeFoodEntries(
            listOf(
                // A custom line's amounts are already the total typed in — no quantity to scale by.
                customEntry("a", 8_00, "Toast", mapOf(caffeine.id to 10.0)),
                customEntry("b", 9_00, " toast ", mapOf(caffeine.id to 10.0)),
                customEntry("c", 10_00, "Soup"),
            ),
        )
        val toast = rows.single { it.name == "Toast" }
        assertEquals(20.0, toast.componentTotals.single().amount, 0.0001)
        assertEquals(2, toast.occurrences.size)
        // A repeated custom line earns a multiplier; a one-off has no serving to multiply.
        assertTrue(toast.showQuantity)
        assertFalse(rows.single { it.name == "Soup" }.showQuantity)
    }

    @Test
    fun aSingleCatalogueLoggingStillNamesItsMultiplier() {
        val row = mergeFoodEntries(listOf(catalogueEntry("a", 8_00, tea, 1.0, emptyMap()))).single()
        assertTrue(row.showQuantity)
        assertEquals(1.0, row.totalQuantity, 0.0001)
        assertTrue(row.componentTotals.isEmpty())
    }

    @Test
    fun componentTotalsFollowTheCatalogueOrder() {
        val rows = mergeFoodEntries(
            listOf(
                catalogueEntry("a", 8_00, tea, 1.0, mapOf(alcohol.id to 1.0, caffeine.id to 75.0)),
            ),
        )
        assertEquals(listOf("Caffeine", "Alcohol"), rows.single().componentTotals.map { it.name })
    }
}
