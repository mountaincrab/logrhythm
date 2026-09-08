package com.mountaincrab.logrhythm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mountaincrab.logrhythm.data.model.SyncStatus
import com.mountaincrab.logrhythm.util.currentTimeMillis
import com.mountaincrab.logrhythm.util.randomUUID

/** A reusable catalogue item. One quantity represents [amount] [unit]. */
@Entity(
    tableName = "food_items",
    indices = [Index(value = ["profileId", "isArchived", "sortOrder"])],
)
data class FoodItemEntity(
    @PrimaryKey val id: String = randomUUID(),
    val userId: String = "local",
    val profileId: String = DEFAULT_PROFILE_ID,
    val name: String,
    val amount: String,
    val unit: String,
    val sortOrder: Int = 0,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isArchived: Boolean = false,
)

/** How much of [componentId] is contained in one [foodItemId]. */
@Entity(
    tableName = "food_item_components",
    primaryKeys = ["foodItemId", "componentId"],
    foreignKeys = [
        ForeignKey(
            entity = FoodItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["componentId"])],
)
data class FoodItemComponentEntity(
    val foodItemId: String,
    val componentId: String,
    val amount: Double,
)

data class FoodItemWithComponents(
    val item: FoodItemEntity,
    val components: List<FoodItemComponentEntity>,
)
