package it.appanettone.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhasePlanTest {
    private val first = PhaseTemplate(1, "Prima", 120, 0)
    private val second = PhaseTemplate(2, "Seconda", 60, 1)
    private val extra = PhaseTemplate(3, "Riposo", 30, 2)

    @Test
    fun editingActiveTemplateKeepsStartAndRecalculatesFollowingTimes() {
        val batch = batch(phase(first, 0, start = 60_000), phase(second, 1))
        val edited = first.copy(name = "Prima lievitazione", minutes = 90)
        val updated = batch.applyTemplates(listOf(first, second), listOf(edited, second))
        assertEquals(batch.phases.map(Phase::id), updated.map(Phase::id))
        assertEquals(edited.name, updated.first().name)
        assertEquals(60_000L, updated.first().startedAt)
        assertEquals(91 * 60_000L, batch.copy(phases = updated).phaseSchedule()[1].start)
    }

    @Test
    fun addingAndRemovingTemplatesUpdatesAlreadyCreatedBatch() {
        val batch = batch(phase(first, 0), phase(second, 1))
        val updated = batch.applyTemplates(listOf(first, second), listOf(second, extra))
        assertEquals(listOf(second.name, extra.name), updated.map(Phase::name))
        assertEquals(listOf(0, 1), updated.map(Phase::position))
        assertEquals(batch.phases.last().id, updated.first().id)
        assertNull(updated.last().startedAt)
    }

    @Test
    fun removalPreservesRecordedHistoryAndBackfillsMissingPendingPhases() {
        val old = phase(first, 0, start = 60_000, end = 7_260_000)
        val batch = batch(old)
        val updated = batch.applyTemplates(listOf(second, extra), listOf(second, extra))
        assertEquals(old, updated.first())
        assertEquals(listOf(first.name, second.name, extra.name), updated.map(Phase::name))
    }

    private fun batch(vararg phases: Phase) = Batch(Panettone(1, "Classico"), phases.toList())

    private fun phase(template: PhaseTemplate, position: Int, start: Long? = null, end: Long? = null) =
        Phase(
            id = template.id + 10,
            panettoneId = 1,
            position = position,
            name = template.name,
            minutes = template.minutes,
            color = template.color,
            startedAt = start,
            completedAt = end,
        )
}
