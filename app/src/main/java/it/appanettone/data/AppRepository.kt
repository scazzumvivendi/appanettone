package it.appanettone.data

import androidx.room.withTransaction
import it.appanettone.timer.ReminderScheduler
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppRepository internal constructor(
    private val database: AppDatabase,
    private val scheduler: ReminderScheduler,
    private val archive: BatchArchive,
) {
    private val dao = database.dao
    private val reminderLock = Mutex()
    val batchLists = combine(dao.observeBatches(), archive.archivedIds) { all, archivedIds ->
        val (archived, active) = all.partition { it.panettone.id in archivedIds }
        BatchLists(active = active, archived = archived)
    }
    val templates = dao.observeTemplates()
    val preferences = dao.observePreferences()

    suspend fun initialize() = updateTemplates { _ ->
        if (dao.preferences() == null) {
            dao.insert(Preferences())
            dao.insert(PhaseTemplate(name = "Prima lievitazione", minutes = 720, color = 0))
            dao.insert(PhaseTemplate(name = "Seconda lievitazione", minutes = 360, color = 1))
        }
    }

    suspend fun addBatch(name: String): Long? = database.withTransaction {
        val defaults = dao.templates()
        if (defaults.isEmpty()) return@withTransaction null
        val panettone = Panettone(name = name.trim())
        val id = dao.insert(panettone)
        dao.insert(Batch(panettone.copy(id = id), emptyList()).applyTemplates(emptyList(), defaults))
        id
    }

    suspend fun archiveCompletedBatch(id: Long): Boolean = reminderLock.withLock {
        val batch = dao.batch(id)?.takeIf { it.isCompleted } ?: return@withLock false
        archive.archive(id)
        batch.phases.forEach { scheduler.cancel(it.id) }
        true
    }

    suspend fun saveTemplate(template: PhaseTemplate) = updateTemplates { previous ->
        if (template.id == 0L) {
            val nextColor = ((previous.lastOrNull()?.color ?: -1) + 1).mod(PhaseColor.entries.size)
            dao.insert(template.copy(color = nextColor))
        } else {
            dao.update(template)
        }
    }

    suspend fun removeTemplate(template: PhaseTemplate) = updateTemplates { dao.delete(template) }

    private suspend fun updateTemplates(change: suspend (List<PhaseTemplate>) -> Unit) = reminderLock.withLock {
        val changedTimers = database.withTransaction {
            val previous = dao.templates()
            change(previous)
            val templates = dao.templates()
            buildList {
                for (batch in dao.batches()) {
                    if (batch.panettone.id in archive.archivedIds.value) continue
                    val oldPhases = batch.orderedPhases
                    val updated = batch.applyTemplates(previous, templates)
                    if (updated == oldPhases) continue

                    dao.deletePhases(batch.panettone.id)
                    if (updated.isNotEmpty()) dao.insert(updated)

                    val oldById = oldPhases.associateBy(Phase::id)
                    addAll(updated.filter { phase ->
                        if (phase.startedAt == null || phase.completedAt != null) return@filter false
                        val old = oldById.getValue(phase.id)
                        old.name != phase.name || old.deadline != phase.deadline
                    })
                }
            }
        }

        reconcileTimersLocked(refreshReminders = changedTimers.mapTo(mutableSetOf(), Phase::id))
    }

    suspend fun setPhaseStart(phaseId: Long, start: Long) = reminderLock.withLock {
        if (dao.setPhaseStart(phaseId, start) != 1) return@withLock
        reconcileTimersLocked(refreshReminders = setOf(phaseId))
    }

    suspend fun complete(phaseId: Long) = reminderLock.withLock {
        reconcileTimersLocked(manualPhaseId = phaseId)
    }

    suspend fun setReminders(enabled: Boolean) = reminderLock.withLock {
        dao.update(Preferences(reminders = enabled))
        reconcileTimersLocked()
    }

    suspend fun reconcileTimers() = reminderLock.withLock { reconcileTimersLocked() }

    private suspend fun reconcileTimersLocked(
        manualPhaseId: Long? = null,
        refreshReminders: Set<Long> = emptySet(),
    ) {
        val now = System.currentTimeMillis()
        val updates = database.withTransaction {
            dao.batches().flatMap { it.advancePhases(now, manualPhaseId) }.also {
                if (it.isNotEmpty()) dao.updatePhases(it)
            }
        }
        updates.filter { it.completedAt != null }.forEach { scheduler.cancel(it.id) }

        val enabled = remindersEnabled()
        val refreshed = refreshReminders + updates.filter { it.completedAt == null }.map(Phase::id)
        dao.activeReminders().forEach { reminder ->
            val phase = reminder.phase
            scheduler.scheduleTransition(phase)
            if (!enabled || phase.id in refreshed) scheduler.cancelReminder(phase.id)
            if (enabled) scheduler.schedule(phase, notifyIfDue = phase.id in refreshed)
        }
    }

    suspend fun deliverReminder(phaseId: Long) = reminderLock.withLock {
        if (!remindersEnabled()) return@withLock
        val reminder = dao.activeReminders().firstOrNull { it.phase.id == phaseId } ?: return@withLock
        val deadline = reminder.phase.deadline ?: return@withLock
        val now = System.currentTimeMillis()
        if (now in (deadline - ReminderScheduler.LEAD_TIME_MS) until deadline) scheduler.show(reminder)
    }

    private suspend fun remindersEnabled() = dao.preferences()?.reminders == true
}
