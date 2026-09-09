package com.mountaincrab.logrhythm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mountaincrab.logrhythm.util.randomUUID

/** One ordered catalogue or custom line inside a food entry. */
@Entity(
    tableName = "food_entry_lines",
    foreignKeys = [
        ForeignKey(
            entity = FoodEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["entryId", "position"]),
        Index(value = ["foodItemId"]),
    ],
)
data class FoodEntryLineEntity(
    @PrimaryKey val id: String = randomUUID(),
    val entryId: String,
    val position: Int,
    val foodItemId: String? = null,
    val quantity: Double? = null,
    val customText: String? = null,
)

/** Optional direct component total for a custom entry line. */
@Entity(
    tableName = "food_entry_line_components",
    primaryKeys = ["lineId", "componentId"],
    foreignKeys = [
        ForeignKey(
            entity = FoodEntryLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["lineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["componentId"])],
)
data class FoodEntryLineComponentEntity(
    val lineId: String,
    val componentId: String,
    val amount: Double,
)

data class ResolvedFoodEntryLine(
    val line: FoodEntryLineEntity,
    val foodItem: FoodItemEntity?,
    val componentAmounts: Map<String, Double>,
)

data class FoodEntryWithLines(
    val entry: FoodEntryEntity,
    val lines: List<ResolvedFoodEntryLine>,
    val componentsById: Map<String, TrackedComponentEntity> = emptyMap(),
) {
    val displayText: String
        get() = lines.joinToString(", ") { resolved ->
            resolved.foodItem?.let { item ->
                val quantity = resolved.line.quantity ?: 1.0
                if (quantity == 1.0) item.name else "${formatFoodNumber(quantity)} × ${item.name}"
            } ?: resolved.line.customText.orEmpty()
        }
}

data class ComponentContribution(
    val occurredAt: Long,
    val componentId: String,
    val amount: Double,
)

fun formatFoodNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString().trimEnd('0').trimEnd('.')
