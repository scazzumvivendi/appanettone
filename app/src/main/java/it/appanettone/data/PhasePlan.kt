package it.appanettone.data

internal fun Batch.applyTemplates(
    previousTemplates: List<PhaseTemplate>,
    templates: List<PhaseTemplate>,
): List<Phase> {
    val available = orderedPhases.toMutableList()
    val phasesByTemplate = previousTemplates.associate { template ->
        val candidates = available.filter { it.name == template.name && it.color == template.color }
        val match = candidates.firstOrNull { it.minutes == template.minutes } ?: candidates.firstOrNull()
        if (match != null) available.remove(match)
        template.id to match
    }
    val templatesById = templates.associateBy(PhaseTemplate::id)
    val templateByPhase = phasesByTemplate.entries.mapNotNull { (templateId, phase) ->
        phase?.let { it.id to templateId }
    }.toMap()

    val recorded = orderedPhases.filter { it.startedAt != null }
    val recordedTemplates = recorded.mapNotNull { templateByPhase[it.id] }.toSet()
    val updatedRecorded = recorded.map { phase ->
        val template = templatesById[templateByPhase[phase.id]] ?: return@map phase
        phase.copy(
            name = template.name,
            minutes = if (phase.completedAt == null) template.minutes else phase.minutes,
            color = template.color,
        )
    }
    val pending = templates.filterNot { it.id in recordedTemplates }.map { template ->
        Phase(
            id = phasesByTemplate[template.id]?.id ?: 0,
            panettoneId = panettone.id,
            position = 0,
            name = template.name,
            minutes = template.minutes,
            color = template.color,
        )
    }
    return (updatedRecorded + pending).mapIndexed { position, phase -> phase.copy(position = position) }
}
