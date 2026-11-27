package com.scazzumvivendi.appanettone.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseScheduleTest {
    @Test
    fun noProjectionBeforeFirstStart() {
        val schedule = schedule(phase(0), phase(1))
        assertTrue(schedule.all { it.start == null && it.end == null })
    }

    @Test
    fun projectedTimesAccumulateDurationsInPhaseOrder() {
        val schedule = schedule(phase(2), phase(0, startMinute = 10), phase(1))
        assertEquals(listOf(10L, 40L, 70L), schedule.map { it.start?.div(60_000) })
        assertEquals(listOf(40L, 70L, 100L), schedule.map { it.end?.div(60_000) })
    }

    @Test
    fun realCompletionAndCustomStartUpdateFollowingProjections() {
        val first = phase(0, startMinute = 10, completedMinute = 32)
        val projected = schedule(first, phase(1), phase(2))
        assertEquals(32L, projected[1].start?.div(60_000))

        val updated = schedule(first, phase(1, startMinute = 45), phase(2))
        assertEquals(listOf(10L, 45L, 75L), updated.map { it.start?.div(60_000) })
    }

    private fun phase(position: Int, startMinute: Long? = null, completedMinute: Long? = null) = Phase(
        panettoneId = 1,
        position = position,
        name = "Fase ${position + 1}",
        minutes = 30,
        color = position,
        startedAt = startMinute?.times(60_000),
        completedAt = completedMinute?.times(60_000),
    )

    private fun schedule(vararg phases: Phase) = Batch(
        Panettone(id = 1, name = "Classico"),
        phases.toList(),
    ).phaseSchedule()
}
