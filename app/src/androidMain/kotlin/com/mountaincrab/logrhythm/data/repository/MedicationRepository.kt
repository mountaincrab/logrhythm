package com.mountaincrab.logrhythm.data.repository

import com.mountaincrab.logrhythm.data.local.dao.MedicationDao
import com.mountaincrab.logrhythm.data.local.dao.MedicationEntryDao
import com.mountaincrab.logrhythm.data.local.dao.MedicationScheduleDao
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationScheduleEntity
import com.mountaincrab.logrhythm.data.model.MedicationForm
import com.mountaincrab.logrhythm.data.model.RepeatRule
import com.mountaincrab.logrhythm.data.model.SyncStatus
import com.mountaincrab.logrhythm.data.model.scheduleOccursOn
import com.mountaincrab.logrhythm.sync.SyncScheduler
import com.mountaincrab.logrhythm.util.currentTimeMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val scheduleDao: MedicationScheduleDao,
    private val entryDao: MedicationEntryDao,
    private val syncScheduler: SyncScheduler,
    private val activeProfileId: StateFlow<String>,
    private val getUserId: () -> String,
) {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    private fun profileId(): String = activeProfileId.value

    fun observeMedications(): Flow<List<MedicationEntity>> =
        activeProfileId.flatMapLatest { medicationDao.observeAll(it) }

    fun observeSchedules(): Flow<List<MedicationScheduleEntity>> =
        activeProfileId.flatMapLatest { scheduleDao.observeAll(it) }

    suspend fun getMedication(id: String): MedicationEntity? = medicationDao.getById(id)
    suspend fun getEntry(id: String): MedicationEntryEntity? = entryDao.getById(id)
    suspend fun getMedications(): List<MedicationEntity> = medicationDao.getAll(profileId())

    // ── catalog ────────────────────────────────────────────────────────────

    /** A medication is defined once: what it's called, what form it takes, how strong one unit is. */
    suspend fun saveMedication(
        id: String? = null,
        name: String,
        form: MedicationForm,
        dose: String,
    ): MedicationEntity {
        val existing = id?.let { medicationDao.getById(it) }
        val med = existing?.copy(
            name = name.trim(),
            form = form,
            dose = dose.trim(),
            updatedAt = currentTimeMillis(),
            syncStatus = SyncStatus.PENDING,
        ) ?: MedicationEntity(
            userId = getUserId(),
            profileId = profileId(),
            name = name.trim(),
            form = form,
            dose = dose.trim(),
        )
        medicationDao.upsert(med)
        syncScheduler.enqueue()
        return med
    }

    /** Deleting a medication retires its scheduled doses too; recorded history is left intact. */
    suspend fun deleteMedication(id: String) {
        medicationDao.softDelete(id)
        scheduleDao.softDeleteByMedication(id)
        syncScheduler.enqueue()
    }

    // ── schedule ───────────────────────────────────────────────────────────

    suspend fun saveSchedule(
        id: String? = null,
        medicationId: String,
        quantity: String,
        timeMinutes: Int,
        repeatRule: RepeatRule,
        daysMask: Int,
    ) {
        val existing = id?.let { scheduleDao.getById(it) }
        val schedule = existing?.copy(
            medicationId = medicationId,
            quantity = quantity.trim(),
            timeMinutes = timeMinutes,
            repeatRule = repeatRule,
            daysMask = daysMask,
            updatedAt = currentTimeMillis(),
            syncStatus = SyncStatus.PENDING,
        ) ?: MedicationScheduleEntity(
            userId = getUserId(),
            profileId = profileId(),
            medicationId = medicationId,
            quantity = quantity.trim(),
            timeMinutes = timeMinutes,
            repeatRule = repeatRule,
            daysMask = daysMask,
            startEpochDay = LocalDate.now(zone).toEpochDay(),
        )
        scheduleDao.upsert(schedule)
        syncScheduler.enqueue()
    }

    suspend fun setScheduleActive(id: String, active: Boolean) {
        scheduleDao.getById(id)?.let {
            scheduleDao.upsert(
                it.copy(isActive = active, updatedAt = currentTimeMillis(), syncStatus = SyncStatus.PENDING),
            )
            syncScheduler.enqueue()
        }
    }

    suspend fun deleteSchedule(id: String) {
        scheduleDao.softDelete(id)
        syncScheduler.enqueue()
    }

    // ── recorded doses ─────────────────────────────────────────────────────

    /**
     * Writes a dose the user entered by hand (the 💊 button on Home), or edits any existing
     * dose — including one the schedule added, which is how you correct a quantity.
     */
    suspend fun saveDose(
        id: String? = null,
        medicationId: String,
        occurredAt: Long,
        quantity: String,
        notes: String?,
    ) {
        val med = medicationDao.getById(medicationId) ?: return
        val existing = id?.let { entryDao.getById(it) }
        val entry = existing?.copy(
            medicationId = medicationId,
            medicationName = med.name,
            dose = med.dose,
            quantity = quantity.trim(),
            occurredAt = occurredAt,
            notes = notes?.takeIf { it.isNotBlank() },
            updatedAt = currentTimeMillis(),
            syncStatus = SyncStatus.PENDING,
        ) ?: MedicationEntryEntity(
            userId = getUserId(),
            profileId = profileId(),
            medicationId = medicationId,
            medicationName = med.name,
            dose = med.dose,
            quantity = quantity.trim(),
            occurredAt = occurredAt,
            notes = notes?.takeIf { it.isNotBlank() },
        )
        entryDao.upsert(entry)
        syncScheduler.enqueue()
    }

    suspend fun deleteEntry(id: String) {
        entryDao.softDelete(id)
        syncScheduler.enqueue()
    }

    suspend fun deleteProfileData(profileId: String) {
        medicationDao.softDeleteByProfile(profileId)
        scheduleDao.softDeleteByProfile(profileId)
        entryDao.softDeleteByProfile(profileId)
        syncScheduler.enqueue()
    }

    // ── materialisation ────────────────────────────────────────────────────

    /**
     * Turns scheduled doses whose time has passed into real [MedicationEntryEntity] rows.
     * This is the schedule's entire job: automating the adding of dose entries.
     *
     * Only doses in the past are written, so the timeline never claims a dose that hasn't
     * happened yet. Ids are derived from schedule + date, so the phone and the webapp
     * converge on one document rather than each creating their own. Existing ids —
     * including soft-deleted ones — are skipped, so a dose the user deleted because they
     * missed it is not silently resurrected on the next pass.
     *
     * Returns how many rows were created.
     */
    suspend fun materialiseDueDoses(): Int {
        val pid = profileId()
        val schedules = scheduleDao.getActive(pid)
        if (schedules.isEmpty()) return 0
        val medications = medicationDao.getAll(pid).associateBy { it.id }

        val nowMillis = currentTimeMillis()
        val today = LocalDate.now(zone)
        val earliest = today.minusDays(BACKFILL_DAYS)
        val existingIds = entryDao
            .existingIdsInRange(pid, earliest.startMillis(), nowMillis)
            .toSet()

        val userId = getUserId()
        val created = mutableListOf<MedicationEntryEntity>()

        for (schedule in schedules) {
            // A schedule whose medication was deleted produces nothing.
            val medication = medications[schedule.medicationId] ?: continue
            var date = maxOf(earliest, LocalDate.ofEpochDay(schedule.startEpochDay))
            while (!date.isAfter(today)) {
                val occurs = scheduleOccursOn(
                    rule = schedule.repeatRule,
                    daysMask = schedule.daysMask,
                    startEpochDay = schedule.startEpochDay,
                    epochDay = date.toEpochDay(),
                    isoDayOfWeek = date.dayOfWeek.value,
                )
                if (occurs) {
                    val dueAt = date.doseMillis(schedule.timeMinutes)
                    if (dueAt <= nowMillis) {
                        val id = MedicationEntryEntity.materialisedId(schedule.id, date.format(ISO_DATE))
                        if (id !in existingIds) {
                            created += MedicationEntryEntity(
                                id = id,
                                userId = userId,
                                profileId = pid,
                                medicationId = medication.id,
                                medicationName = medication.name,
                                dose = medication.dose,
                                quantity = schedule.quantity,
                                occurredAt = dueAt,
                                scheduleId = schedule.id,
                            )
                        }
                    }
                }
                date = date.plusDays(1)
            }
        }

        if (created.isEmpty()) return 0
        entryDao.insertIfAbsent(created)
        return created.size
    }

    private fun LocalDate.startMillis(): Long =
        atStartOfDay(zone).toInstant().toEpochMilli()

    private fun LocalDate.doseMillis(minutes: Int): Long =
        atStartOfDay(zone).plusMinutes(minutes.toLong()).toInstant().toEpochMilli()

    private companion object {
        /** How far back a first run (or a long absence) will fill in missed doses. */
        const val BACKFILL_DAYS = 14L
        val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
