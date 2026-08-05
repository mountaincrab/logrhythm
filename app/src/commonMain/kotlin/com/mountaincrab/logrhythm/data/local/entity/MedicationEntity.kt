package com.mountaincrab.logrhythm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mountaincrab.logrhythm.data.model.MedicationForm
import com.mountaincrab.logrhythm.data.model.SyncStatus
import com.mountaincrab.logrhythm.util.currentTimeMillis
import com.mountaincrab.logrhythm.util.randomUUID

/**
 * A medication the user takes, defined once and referenced everywhere: by scheduled
 * doses ([MedicationScheduleEntity]) and by recorded doses ([MedicationEntryEntity]).
 * Keeping the catalog separate is what stops drug names becoming free text in three places.
 *
 * A medication is name + form + strength — "Pentasa, tablet, 1g". How *many* you take is
 * not part of the definition; that's the quantity on a schedule or a recorded dose.
 */
@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey val id: String = randomUUID(),
    val userId: String = "local",
    val profileId: String = DEFAULT_PROFILE_ID,
    val name: String,
    val form: MedicationForm = MedicationForm.TABLET,
    /** Strength of a single unit of this medication, e.g. "1g" — free text so odd units fit. */
    val dose: String = "",
    val sortOrder: Int = 0,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val isDeleted: Boolean = false,
)
