package com.mountaincrab.logrhythm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.mountaincrab.logrhythm.data.local.dao.FoodEntryDao
import com.mountaincrab.logrhythm.data.local.dao.MedicationDao
import com.mountaincrab.logrhythm.data.local.dao.MedicationEntryDao
import com.mountaincrab.logrhythm.data.local.dao.MedicationScheduleDao
import com.mountaincrab.logrhythm.data.local.dao.NoteEntryDao
import com.mountaincrab.logrhythm.data.local.dao.NoteTagDao
import com.mountaincrab.logrhythm.data.local.dao.PoopEntryDao
import com.mountaincrab.logrhythm.data.local.dao.PoopTagDao
import com.mountaincrab.logrhythm.data.local.dao.ProfileDao
import com.mountaincrab.logrhythm.data.local.dao.TimelineDao
import com.mountaincrab.logrhythm.data.local.entity.FoodEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationScheduleEntity
import com.mountaincrab.logrhythm.data.local.entity.NoteEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.NoteEntryTagCrossRef
import com.mountaincrab.logrhythm.data.local.entity.NoteTagEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.PoopEntryTagCrossRef
import com.mountaincrab.logrhythm.data.local.entity.PoopTagEntity
import com.mountaincrab.logrhythm.data.local.entity.ProfileEntity
import com.mountaincrab.logrhythm.data.model.MealTag
import com.mountaincrab.logrhythm.data.model.MedicationForm
import com.mountaincrab.logrhythm.data.model.RepeatRule
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
        ProfileEntity::class,
        MedicationEntity::class,
        MedicationScheduleEntity::class,
        MedicationEntryEntity::class,
    ],
    version = 11,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun poopEntryDao(): PoopEntryDao
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun noteEntryDao(): NoteEntryDao
    abstract fun poopTagDao(): PoopTagDao
    abstract fun noteTagDao(): NoteTagDao
    abstract fun profileDao(): ProfileDao
    abstract fun timelineDao(): TimelineDao
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationScheduleDao(): MedicationScheduleDao
    abstract fun medicationEntryDao(): MedicationEntryDao

    companion object {
        const val CURRENT_VERSION = 11
    }
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

    @TypeConverter fun fromMedicationForm(value: MedicationForm): String = value.name
    @TypeConverter fun toMedicationForm(value: String): MedicationForm = MedicationForm.fromName(value)

    @TypeConverter fun fromRepeatRule(value: RepeatRule): String = value.name
    @TypeConverter fun toRepeatRule(value: String): RepeatRule = RepeatRule.fromName(value)
}
