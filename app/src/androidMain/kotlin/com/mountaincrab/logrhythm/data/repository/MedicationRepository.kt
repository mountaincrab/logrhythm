package com.mountaincrab.logrhythm.data.repository

import com.mountaincrab.logrhythm.data.local.dao.MedicationDao
import com.mountaincrab.logrhythm.data.local.dao.MedicationEntryDao
import com.mountaincrab.logrhythm.data.local.dao.MedicationScheduleDao
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationEntryEntity
import com.mountaincrab.logrhythm.data.local.entity.MedicationScheduleEntity
import com.mountaincrab.logrhythm.data.model.DoseStatus
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

/**
 * A dose the schedule says is still to come today. Purely derived — nothing is written
 * until its time passes (materialisation) or the user acts on it.
 */
data class UpcomingDose(
    val schedule: MedicationScheduleEntity,
    val medication: MedicationEntity,
    val dueAt: Long,
)

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

    /** Recorded doses for a single local day, oldest first — the Today list. */
    fun observeEntriesForDay(date: LocalDate): Flow<List<MedicationEntryEntity>> =
        activeProfileId.flatMapLatest {
            entryDao.observeInRange(it, date.startMillis(), date.endMillis())
        }

    suspend fun getMedication(id: String): MedicationEntity? = medicationDao.getById(id)
    suspend fun getEntry(id: String): MedicationEntryEntity? = entryDao.getById(id)
    suspend fun getMedications(): List<MedicationEntity> = medicationDao.getAll(profileId())

    // ── catalog ────────────────────────────────────────────────────────────

    suspend fun saveMedication(
        id: String? = null,
        name: String,
        form: MedicationForm,
        defaultAmount: String,
        defaultUnit: String,
    ): MedicationEntity {
        val existing = id?.let { medicationDao.getById(it) }
        val med = existing?.copy(
            name = name.trim(),
            form = form,
            defaultAmount = defaultAmount.trim(),
            defaultUnit = defaultUnit.trim(),
            updatedAt = currentTimeMillis(),
            syncStatus = SyncStatus.PENDING,
        ) ?: MedicationEntity(
            userId = getUserId(),
            profileId = profileId(),
            name = name.trim(),
            form = form,
            defaultAmount = defaultAmount.trim(),
            defaultUnit = defaultUnit.trim(),
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
        amount: String,
        unit: String,
        timeMinutes: Int,
        repeatRule: RepeatRule,
        daysMask: Int,
    ) {
        val existing = id?.let { scheduleDao.getById(it) }
        val schedule = existing?.copy(
            medicationId = medicationId,
            amount = amount.trim(),
            unit = unit.trim(),
            timeMinutes = timeMinutes,
            repeatRule = repeatRule,
            daysMask = daysMask,
            updatedAt = currentTimeMillis(),
            syncStatus = SyncStatus.PENDING,
        ) ?: MedicationScheduleEntity(
            userId = getUserId(),
            profileId = profileId(),
            medicationId = medicationId,
            amount = amount.trim(),
            unit = unit.trim(),
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

    /** Logs a one-off dose the user entered by hand (the 💊 button on Home). */
    suspend fun saveManualDose(
        id: String? = null,
        medicationId: String,
        occurredAt: Long,
        amount: String,
        unit: String,
        notes: String?,
    ) {
        val med = medicationDao.getById(medicationId) ?: return
        val existing = id?.let { entryDao.getById(it) }
        val entry = existing?.copy(
            medicationId = medicationId,
            medicationName = med.name,
            occurredAt = occurredAt,
            amount = amount.trim(),
            unit = unit.trim(),
            notes = notes?.takeIf { it.isNotBlank() },
            updatedAt = currentTimeMillis(),
            syncStatus = SyncStatus.PENDING,
        ) ?: MedicationEntryEntity(
            userId = getUserId(),
            profileId = profileId(),
            medicationId = medicationId,
            medicationName = med.name,
            occurredAt = occurredAt,
            amount = amount.trim(),
            unit = unit.trim(),
            status = DoseStatus.MANUAL,
            notes = notes?.takeIf { it.isNotBlank() },
        )
        entryDao.upsert(entry)
        syncScheduler.enqueue()
    }

    /**
     * Records what really happened to an already-materialised dose. [amount] is only passed
     * when the user is correcting the quantity, which is what makes a dose "adjusted".
     */
    suspend fun setDoseStatus(
        entryId: String,
        status: DoseStatus,
        amount: String? = null,
        unit: String? = null,
    ) {
        val entry = entryDao.getById(entryId) ?: return
        entryDao.upsert(
            entry.copy(
                status = status,
                amount = amount?.trim() ?: entry.amount,
                unit = unit?.trim() ?: entry.unit,
                updatedAt = currentTimeMillis(),
                syncStatus = SyncStatus.PENDING,
            ),
        )
        syncScheduler.enqueue()
    }

    /** Confirms an upcoming dose early — writes the row materialisation would have written. */
    suspend fun confirmUpcoming(dose: UpcomingDose, status: DoseStatus) {
        val date = java.time.Instant.ofEpochMilli(dose.dueAt).atZone(zone).toLocalDate()
        val entry = MedicationEntryEntity(
            id = MedicationEntryEntity.materialisedId(dose.schedule.id, date.format(ISO_DATE)),
            userId = getUserId(),
            profileId = profileId(),
            medicationId = dose.medication.id,
            medicationName = dose.medication.name,
            occurredAt = dose.dueAt,
            amount = dose.schedule.amount,
            unit = dose.schedule.unit,
            status = status,
            scheduleId = dose.schedule.id,
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
     * Turns scheduled doses whose time has passed into real [MedicationEntryEntity] rows,
     * so meds appear on the timeline without the user confirming anything.
     *
     * Only doses in the past are written; the rest of today stays derived (see [upcomingToday]),
     * which is what stops the timeline claiming a dose that hasn't happened yet. Ids are
     * derived from schedule + date, so the phone and the webapp converge on one document
     * rather than each creating their own. Existing ids — including soft-deleted ones — are
     * skipped, so a dose the user deleted is not silently resurrected on the next pass.
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
                                occurredAt = dueAt,
                                amount = schedule.amount,
                                unit = schedule.unit,
                                status = DoseStatus.SCHEDULED,
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

    /**
     * Doses still ahead of you today, derived straight from the schedule. These are shown as
     * "due" but never written — materialisation picks them up once their time passes.
     */
    suspend fun upcomingToday(): List<UpcomingDose> {
        val pid = profileId()
        val schedules = scheduleDao.getActive(pid)
        if (schedules.isEmpty()) return emptyList()
        val medications = medicationDao.getAll(pid).associateBy { it.id }
        val nowMillis = currentTimeMillis()
        val today = LocalDate.now(zone)

        return schedules.mapNotNull { schedule ->
            val medication = medications[schedule.medicationId] ?: return@mapNotNull null
            val occurs = scheduleOccursOn(
                rule = schedule.repeatRule,
                daysMask = schedule.daysMask,
                startEpochDay = schedule.startEpochDay,
                epochDay = today.toEpochDay(),
                isoDayOfWeek = today.dayOfWeek.value,
            )
            if (!occurs) return@mapNotNull null
            val dueAt = today.doseMillis(schedule.timeMinutes)
            if (dueAt <= nowMillis) null else UpcomingDose(schedule, medication, dueAt)
        }.sortedBy { it.dueAt }
    }

    private fun LocalDate.startMillis(): Long =
        atStartOfDay(zone).toInstant().toEpochMilli()

    private fun LocalDate.endMillis(): Long =
        plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    private fun LocalDate.doseMillis(minutes: Int): Long =
        atStartOfDay(zone).plusMinutes(minutes.toLong()).toInstant().toEpochMilli()

    private companion object {
        /** How far back a first run (or a long absence) will fill in missed doses. */
        const val BACKFILL_DAYS = 14L
        val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
