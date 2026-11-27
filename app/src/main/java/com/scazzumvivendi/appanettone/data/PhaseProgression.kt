package com.scazzumvivendi.appanettone.data

/** Returns only the phases whose recorded times need updating. */
internal fun Batch.advancePhases(now: Long, manualPhaseId: Long? = null): List<Phase> = buildList {
    var nextStart: Long? = null
    for (phase in orderedPhases) {
        if (phase.completedAt != null) continue

        val start = phase.startedAt ?: nextStart ?: break
        val started = phase.copy(startedAt = start)
        val deadline = checkNotNull(started.deadline)
        val completedAt = when {
            deadline <= now -> deadline
            phase.id == manualPhaseId && start <= now -> now
            else -> null
        }
        val updated = started.copy(completedAt = completedAt)
        if (updated != phase) add(updated)
        nextStart = completedAt
        if (completedAt == null) break
    }
}
