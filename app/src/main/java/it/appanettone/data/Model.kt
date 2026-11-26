package it.appanettone.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity
data class Panettone(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Entity
data class PhaseTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val minutes: Int,
    val color: Int,
)

@Entity(
    foreignKeys = [ForeignKey(Panettone::class, ["id"], ["panettoneId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["panettoneId", "position"], unique = true)],
)
data class Phase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val panettoneId: Long,
    val position: Int,
    val name: String,
    val minutes: Int,
    val color: Int,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
) {
    val deadline: Long? get() = startedAt?.plus(minutes * 60_000L)
}

@Entity
data class Preferences(
    @PrimaryKey val id: Int = 0,
    val reminders: Boolean = true,
)

data class Batch(
    @Embedded val panettone: Panettone,
    @Relation(parentColumn = "id", entityColumn = "panettoneId") val phases: List<Phase>,
) {
    val orderedPhases get() = phases.sortedBy(Phase::position)
    val currentPhase get() = orderedPhases.firstOrNull { it.completedAt == null }

    val isCompleted get() = phases.isNotEmpty() && currentPhase == null

    fun phaseSchedule(): List<ScheduledPhase> {
        var nextStart: Long? = null
        return orderedPhases.map { phase ->
            val start = phase.startedAt ?: nextStart
            val end = phase.completedAt ?: start?.plus(phase.minutes * 60_000L)
            nextStart = end
            ScheduledPhase(phase, start, end)
        }
    }
}

data class BatchLists(val active: List<Batch>, val archived: List<Batch>)

data class ScheduledPhase(val phase: Phase, val start: Long?, val end: Long?)

data class Reminder(
    @Embedded val phase: Phase,
    val batchName: String,
)
