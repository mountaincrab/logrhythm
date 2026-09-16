package com.mountaincrab.logrhythm.data.model

import com.mountaincrab.logrhythm.data.local.entity.DEFAULT_FOOD_ITEM_ICON
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryWithLines
import com.mountaincrab.logrhythm.data.local.entity.TrackedComponentEntity

/** One logging of a merged row's food: which entry said so, and when. */
data class FoodOccurrence(val entryId: String, val occurredAt: Long)

/** A merged row's running total for one tracked component, in that component's own unit. */
data class MergedFoodComponentTotal(
    val componentId: String,
    val name: String,
    val unit: String,
    val amount: Double,
)

/**
 * A day's loggings of one food, folded into a single row: how much of it in total, what
 * that adds up to per tracked component, and the time of each logging.
 *
 * Four cups of tea are four entries — deleting or editing one still happens on that entry,
 * which is why [occurrences] keeps the entry ids rather than just the times.
 */
data class MergedFoodRow(
    val key: String,
    val icon: String,
    val name: String,
    val totalQuantity: Double,
    /**
     * Catalogue foods always name their multiplier (one serving is "1 ×"), a custom line
     * only once it has repeated — it has no serving size to be a multiple of.
     */
    val showQuantity: Boolean,
    val componentTotals: List<MergedFoodComponentTotal>,
    val occurrences: List<FoodOccurrence>,
)

/**
 * Fold a day's food entries into one row per food, for the grouped home timeline.
 *
 * Catalogue lines merge on their `foodItemId` — the item is a live reference, so two
 * loggings of it are the same food however it has been renamed since. Custom lines have
 * only their text to go on, so they merge case-insensitively on that. Component amounts
 * follow the same rule as a single row: a catalogue line's are per serving and scale with
 * the quantity, a custom line's are already the total it typed in.
 *
 * Rows come back newest-logged first, matching the feed around them, while the times
 * inside a row run forwards — the row is that food's day, read left to right.
 * Mirror of `mergeFoodEntries` in webapp/src/lib/food.ts.
 */
fun mergeFoodEntries(entries: List<FoodEntryWithLines>): List<MergedFoodRow> {
    val components = entries.flatMap { it.componentsById.entries }.associate { it.key to it.value }
    val accumulators = LinkedHashMap<String, MergedFoodAccumulator>()

    entries.forEach { food ->
        food.lines.sortedBy { it.line.position }.forEach { resolved ->
            val itemId = resolved.line.foodItemId
            val customText = resolved.line.customText?.trim().orEmpty()
            val key = if (itemId != null) "item:$itemId" else "custom:${customText.lowercase()}"
            val quantity = if (itemId != null) resolved.line.quantity ?: 1.0 else 1.0
            val accumulator = accumulators.getOrPut(key) {
                MergedFoodAccumulator(
                    key = key,
                    icon = resolved.foodItem?.icon ?: DEFAULT_FOOD_ITEM_ICON,
                    name = resolved.foodItem?.name
                        ?: customText.takeIf { it.isNotEmpty() }
                        ?: UNAVAILABLE_FOOD_ITEM,
                    isCatalogueItem = itemId != null,
                )
            }
            accumulator.totalQuantity += quantity
            resolved.componentAmounts.forEach { (componentId, amount) ->
                val contribution = if (itemId != null) amount * quantity else amount
                accumulator.componentTotals[componentId] =
                    (accumulator.componentTotals[componentId] ?: 0.0) + contribution
            }
            // One occurrence per entry: an entry listing the same food twice is still one
            // logging at one time, its quantities simply add up.
            accumulator.occurrences.getOrPut(food.entry.id) { food.entry.occurredAt }
        }
    }

    return accumulators.values
        .map { accumulator ->
            MergedFoodRow(
                key = accumulator.key,
                icon = accumulator.icon,
                name = accumulator.name,
                totalQuantity = accumulator.totalQuantity,
                showQuantity = accumulator.isCatalogueItem || accumulator.totalQuantity > 1.0,
                componentTotals = accumulator.componentTotals
                    .filterValues { it > 0.0 }
                    .mapNotNull { (componentId, amount) ->
                        components[componentId]?.let { component ->
                            MergedFoodComponentTotal(componentId, component.name, component.unit, amount)
                        }
                    }
                    .sortedWith(compareBy({ componentSortOrder(components, it.componentId) }, { it.name })),
                occurrences = accumulator.occurrences
                    .map { (entryId, occurredAt) -> FoodOccurrence(entryId, occurredAt) }
                    .sortedBy { it.occurredAt },
            )
        }
        .sortedWith(compareByDescending<MergedFoodRow> { row -> row.occurrences.maxOf { it.occurredAt } }
            .thenBy { it.name })
}

const val UNAVAILABLE_FOOD_ITEM = "Unavailable food item"

private fun componentSortOrder(
    components: Map<String, TrackedComponentEntity>,
    componentId: String,
): Int = components[componentId]?.sortOrder ?: Int.MAX_VALUE

private class MergedFoodAccumulator(
    val key: String,
    val icon: String,
    val name: String,
    val isCatalogueItem: Boolean,
    var totalQuantity: Double = 0.0,
    val componentTotals: LinkedHashMap<String, Double> = LinkedHashMap(),
    val occurrences: LinkedHashMap<String, Long> = LinkedHashMap(),
)
