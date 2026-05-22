package com.mountaincrab.logrhythm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.mountaincrab.logrhythm.data.local.dao.FoodEntryDao
import com.mountaincrab.logrhythm.data.local.dao.NoteEntryDao
import com.mountaincrab.logrhythm.data.local.dao.NoteTagDao
import com.mountaincrab.logrhythm.data.local.dao.PoopEntryDao
import com.mountaincrab.logrhythm.data.local.dao.PoopTagDao
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.NoteEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.NoteEntryTagCrossRef
import com.mountaincrab.logrhythm.data.local.entity.NoteTagEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryTagCrossRef
import com.mountaincrab.logrhythm.data.local.entity.PoopTagEntity
import com.mountaincrab.logrhythm.data.model.MealTag
import com.mountaincrab.logrhythm.data.model.SyncStatus

@Database(
    entities = [
        PoopEntryEntity::class,
        FoodEntryEntity::class,
        NoteEntryEntity::class,
        PoopTagEntity::class,
        PoopEntryTagCrossRef::class,
        NoteTagEntity::class,
        NoteEntryTagCrossRef::class,
    ],
    version = 7,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun poopEntryDao(): PoopEntryDao
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun noteEntryDao(): NoteEntryDao
    abstract fun poopTagDao(): PoopTagDao
    abstract fun noteTagDao(): NoteTagDao
}

class Converters {
    @TypeConverter fun fromSyncStatus(value: SyncStatus): String = value.name
    @TypeConverter fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter fun fromMealTag(value: MealTag?): String? = value?.name
    @TypeConverter fun toMealTag(value: String?): MealTag? = value?.let { MealTag.valueOf(it) }

    @TypeConverter fun bristolTypesToMask(types: Set<Int>): Int =
        types.fold(0) { acc, n -> acc or (1 shl (n - 1)) }

    @TypeConverter fun bristolTypesFromMask(mask: Int): Set<Int> =
        (1..7).filter { n -> (mask and (1 shl (n - 1))) != 0 }.toSet()
}
