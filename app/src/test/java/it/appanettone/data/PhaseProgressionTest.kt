package it.appanettone.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseProgressionTest {
    @Test
    fun expiryStartsNextPhaseAtDeadline() {
        val first = phase(0, 1_000)
        val second = phase(1)
        val expectedEnd = first.deadline!!
        val batch = batch(first, second)
        assertTrue(batch.advancePhases(expectedEnd - 1).isEmpty())
        assertEquals(
            listOf(first.copy(completedAt = expectedEnd), second.copy(startedAt = expectedEnd)),
            batch.advancePhases(expectedEnd),
        )
    }

    @Test
    fun catchingUpAfterMultipleDeadlinesCompletesTheBatch() {
        val first = phase(0, 1_000)
        val second = phase(1)
        val startedSecond = second.copy(startedAt = first.deadline)
        val updates = batch(first, second).advancePhases(200_000)
        assertEquals(listOf(first.deadline, startedSecond.deadline), updates.map(Phase::completedAt))
    }

    @Test
    fun manualCompletionStartsNextPhaseImmediately() {
        val first = phase(0, 1_000)
        val second = phase(1)
        assertEquals(
            listOf(first.copy(completedAt = 2_000), second.copy(startedAt = 2_000)),
            batch(first, second).advancePhases(2_000, manualPhaseId = first.id),
        )
    }

    @Test
    fun batchWithNoStartedPhaseRemainsPending() {
        assertTrue(batch(phase(0), phase(1)).advancePhases(200_000).isEmpty())
    }

    private fun phase(position: Int, start: Long? = null) =
        Phase((position + 1).toLong(), 1, position, "Fase", 1, 0, startedAt = start)

    private fun batch(vararg phases: Phase) = Batch(Panettone(1, "Classico"), phases.toList())
}
