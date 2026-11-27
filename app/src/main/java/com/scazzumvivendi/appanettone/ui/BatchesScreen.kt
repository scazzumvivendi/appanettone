package com.scazzumvivendi.appanettone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scazzumvivendi.appanettone.data.Batch
import com.scazzumvivendi.appanettone.data.Phase
import com.scazzumvivendi.appanettone.data.ScheduledPhase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

@Composable
fun BatchesScreen(
    batches: List<Batch>,
    expandedBatch: Long?,
    onExpand: (Long?) -> Unit,
    canCreateBatch: Boolean,
    onSettings: () -> Unit,
    onStart: (Long, Long) -> Unit,
    onComplete: (Long) -> Unit,
    onArchive: (Batch) -> Unit,
    archiveBack: (() -> Unit)? = null,
) {
    val archived = archiveBack != null
    val clock = remember(archived) {
        flow {
            while (!archived) {
                val now = System.currentTimeMillis()
                emit(now)
                delay(1_000L - now.mod(1_000L))
            }
        }
    }
    val now by clock.collectAsStateWithLifecycle(System.currentTimeMillis())

    val listState = rememberLazyListState()
    LaunchedEffect(expandedBatch, batches) {
        val index = batches.indexOfFirst { it.panettone.id == expandedBatch }
        if (index >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.key == expandedBatch }) {
            listState.scrollToItem(index + 1)
        }
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeading(if (archived) "Archivio" else "Panettoni", archiveBack) }
        if (batches.isEmpty() && archived) {
            item { EmptyCard("Archivio vuoto", "Qui trovi i panettoni che hai archiviato.") }
        } else if (batches.isEmpty() && canCreateBatch) {
            item { EmptyCard("Nessun panettone", "Tocca + per aggiungere il primo panettone.") }
        }
        items(batches, key = { it.panettone.id }) { batch ->
            val expanded = expandedBatch == batch.panettone.id
            BatchCard(
                batch = batch,
                expanded = expanded,
                now = now,
                onToggle = { onExpand(if (expanded) null else batch.panettone.id) },
                onStart = onStart,
                onComplete = onComplete,
                onArchive = if (archived) null else ({ onArchive(batch) }),
            )
        }
        if (!archived && !canCreateBatch) {
            item {
                SoftCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Aggiungi almeno una fase nelle impostazioni per creare un panettone.")
                        OutlinedButton(onClick = onSettings) { Text("Gestisci fasi") }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchCard(
    batch: Batch,
    expanded: Boolean,
    now: Long,
    onToggle: () -> Unit,
    onStart: (Long, Long) -> Unit,
    onComplete: (Long) -> Unit,
    onArchive: (() -> Unit)?,
) {
    val current = batch.currentPhase
    val status = when {
        batch.phases.isEmpty() -> PhaseStatus("Da avviare")
        current == null -> PhaseStatus("Completato", -2)
        current.startedAt == null -> PhaseStatus("Da avviare")
        else -> PhaseStatus(current.name, current.color)
    }
    val pillWidth = (LocalConfiguration.current.screenWidthDp * 0.43f).dp
    SoftCard {
        TextButton(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(batch.panettone.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            PhasePill(status, Modifier.widthIn(max = pillWidth))
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Chiudi fasi" else "Apri fasi",
            )
        }
        if (expanded) {
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (current?.deadline != null) PhaseProgress(current, now)
                val schedule = batch.phaseSchedule()
                if (schedule.isEmpty()) {
                    Text("Aggiungi una fase nelle impostazioni per avviare questo panettone.")
                }
                schedule.forEach { entry ->
                    PhaseRow(entry, active = entry.phase.id == current?.id)
                }
                if (current != null) {
                    PhaseAction(
                        current, schedule.first { it.phase.id == current.id }.start, now, onStart, onComplete,
                    )
                } else if (batch.isCompleted && onArchive != null) {
                    Button(onClick = onArchive, modifier = Modifier.fillMaxWidth()) {
                        Text("Archivia")
                    }
                }
            }
        }
    }
}

@Composable
private fun PhaseRow(entry: ScheduledPhase, active: Boolean) {
    val phase = entry.phase
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(phase.name, style = MaterialTheme.typography.bodyMedium)
            val start = entry.start
            val end = entry.end
            if (start != null && end != null) {
                val startLabel = if (phase.startedAt != null) "Inizio" else "Inizio previsto"
                Text("$startLabel ${phaseDateTime(start)}", style = MaterialTheme.typography.bodySmall)
            }
        }
        val endLabel = if (phase.completedAt != null) "Fine" else "Fine prevista"
        Text("$endLabel ${phaseDateTime(entry.end)}", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun PhaseProgress(phase: Phase, now: Long) {
    val remaining = (checkNotNull(phase.deadline) - now).coerceAtLeast(0)
    val progress = (1f - remaining / (phase.minutes * 60_000f)).coerceIn(0f, 1f)
    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    val text = when {
        now < checkNotNull(phase.startedAt) -> "Inizio programmato"
        remaining == 0L -> "Tempo scaduto"
        remaining < 60_000 -> "Meno di 1 min rimanente"
        else -> "${durationLabel(((remaining + 59_999) / 60_000).toInt())} rimanenti"
    }
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun PhaseAction(
    phase: Phase,
    proposedStart: Long?,
    now: Long,
    onStart: (Long, Long) -> Unit,
    onComplete: (Long) -> Unit,
) {
    var confirmEarly by rememberSaveable(phase.id) { mutableStateOf(false) }
    var pickStart by rememberSaveable(phase.id) { mutableStateOf(false) }
    val deadline = phase.deadline
    val scheduled = phase.startedAt?.let { now < it } == true

    if (deadline == null) {
        Button(onClick = { onStart(phase.id, System.currentTimeMillis()) }, modifier = Modifier.fillMaxWidth()) {
            Text("Avvia ora")
        }
    } else if (!scheduled) Button(
        onClick = {
            if (now < deadline) confirmEarly = true else onComplete(phase.id)
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Concludi fase")
    }

    OutlinedButton(onClick = { pickStart = true }, modifier = Modifier.fillMaxWidth()) {
        Text(if (deadline == null) "Imposta data e ora di inizio" else "Modifica inizio")
    }
    if (pickStart) {
        PhaseStartPicker(
            initialStart = proposedStart ?: now,
            onDismiss = { pickStart = false },
            onSave = {
                pickStart = false
                onStart(phase.id, it)
            },
        )
    }
    if (confirmEarly) {
        AlertDialog(
            onDismissRequest = { confirmEarly = false },
            title = { Text("Concludere la fase?") },
            text = { Text("Il timer è ancora in corso. Se concludi ora, il promemoria verrà annullato.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmEarly = false
                    onComplete(phase.id)
                }) { Text("Concludi fase") }
            },
            dismissButton = { TextButton(onClick = { confirmEarly = false }) { Text("Annulla") } },
        )
    }
}
