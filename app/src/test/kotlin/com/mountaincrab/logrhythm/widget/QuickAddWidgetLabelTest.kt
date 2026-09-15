package com.mountaincrab.logrhythm.widget

import com.mountaincrab.logrhythm.data.local.entity.FoodItemEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickAddWidgetLabelTest {

    private fun item(amount: String, unit: String) =
        FoodItemEntity(id = "tea", name = "Tea", icon = "☕", amount = amount, unit = unit)

    @Test
    fun oneServingReadsAsThatServing() {
        assertEquals("250 ml", servingLabel(1.0, item("250", "ml")))
    }

    @Test
    fun severalServingsMultiplyTheDefinition() {
        assertEquals("2 × 250 ml", servingLabel(2.0, item("250", "ml")))
        assertEquals("0.5 × 1 cup", servingLabel(0.5, item("1", "cup")))
    }

    @Test
    fun wholeQuantitiesDropTheirDecimal() {
        assertEquals("3", formatQuantity(3.0))
        assertEquals("1.5", formatQuantity(1.5))
    }
}
