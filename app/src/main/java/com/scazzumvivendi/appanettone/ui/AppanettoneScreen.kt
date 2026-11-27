package com.scazzumvivendi.appanettone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scazzumvivendi.appanettone.SupporterState
import com.scazzumvivendi.appanettone.data.AppRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

public enum class Page { Batches, Archive, Settings }

private enum class Dialog { Batch, ArchiveBatch, Template, RemoveTemplate }

@Composable
fun AppanettoneScreen(
    repository: AppRepository,
    supporter: SupporterState,
    onBecomeSupporter: () -> Unit,
    onRefreshSupporter: () -> Unit,
    notificationDestination: Long?,
    consumeNotificationDestination: () -> Unit,
    notificationsAllowed: Boolean,
    exactAlarmsAllowed: Boolean,
    enableNotifications: () -> Unit,
    enableExactAlarms: () -> Unit,
) {
    val batchLists by repository.batchLists.collectAsStateWithLifecycle(initialValue = null)
    val batches = batchLists?.active
    val templates by repository.templates.collectAsStateWithLifecycle(initialValue = emptyList())
    val preferences by repository.preferences.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var expandedBatch by rememberSaveable { mutableStateOf<Long?>(null) }
    var expandedArchivedBatch by rememberSaveable { mutableStateOf<Long?>(null) }
    var page by rememberSaveable { mutableStateOf(Page.Batches) }
    val showArchive = page == Page.Archive
    var dialog by rememberSaveable { mutableStateOf<Dialog?>(null) }
    var archiveBatchId by rememberSaveable { mutableStateOf<Long?>(null) }
    var templateId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editing by remember { mutableStateOf(false) }
    val dismiss = { dialog = null }
    val selectedTemplate = templates.firstOrNull { it.id == templateId }

    fun change(block: suspend () -> Unit) {
        if (editing) return
        editing = true
        scope.launch {
            try {
                block()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                android.util.Log.e("Appanettone", "Errore durante il salvataggio", exception)
                scope.launch { snackbar.showSnackbar("Impossibile salvare la modifica.") }
            } finally {
                editing = false
            }
        }
    }

    fun back() {
        if (page != Page.Batches) {
            page = Page.Batches
        } else {
            expandedBatch = null
        }
    }

    LaunchedEffect(repository) {
        repository.initialize()
    }
    val nextDeadline = batches.orEmpty().mapNotNull { it.currentPhase?.deadline }.minOrNull()
    LaunchedEffect(nextDeadline) {
        if (nextDeadline != null) {
            delay((nextDeadline - System.currentTimeMillis()).coerceAtLeast(0))
            repository.reconcileTimers()
        }
    }
    LaunchedEffect(notificationDestination, batches != null) {
        if (notificationDestination != null && batches != null) {
            if (batches.orEmpty().any { it.panettone.id == notificationDestination }) {
                expandedBatch = notificationDestination
                page = Page.Batches
                dialog = null
            }
            consumeNotificationDestination()
        }
    }
    BackHandler(enabled = page != Page.Batches || expandedBatch != null, onBack = ::back)

    Scaffold(
        topBar = {
            AppHeader(
                currentPage = page,
                isSupporter = supporter.owned,
                onSettings = { page = Page.Settings },
                onArchive = { page = Page.Archive },
                onPanettoni = { page = Page.Batches },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (page == Page.Batches && batches != null && preferences != null && templates.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { dialog = Dialog.Batch },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ) { Icon(Icons.Default.Add, contentDescription = "Aggiungi panettone") }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                batches == null || preferences == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                page == Page.Settings -> SettingsScreen(
                    templates = templates,
                    supporter = supporter,
                    onBecomeSupporter = onBecomeSupporter,
                    onRefreshSupporter = onRefreshSupporter,
                    reminders = preferences?.reminders == true,
                    notificationsAllowed = notificationsAllowed,
                    exactAlarmsAllowed = exactAlarmsAllowed,
                    onBack = ::back,
                    onAdd = {
                        templateId = null
                        dialog = Dialog.Template
                    },
                    onEdit = {
                        templateId = it.id
                        dialog = Dialog.Template
                    },
                    onRemove = {
                        templateId = it.id
                        dialog = Dialog.RemoveTemplate
                    },
                    onToggleReminders = { enabled ->
                        change {
                            repository.setReminders(enabled)
                            if (enabled && !notificationsAllowed) enableNotifications()
                        }
                    },
                    onNotificationPermission = enableNotifications,
                    onExactAlarmPermission = enableExactAlarms,
                )
                else -> BatchesScreen(
                    batches = if (showArchive) batchLists?.archived.orEmpty() else batches.orEmpty(),
                    expandedBatch = if (showArchive) expandedArchivedBatch else expandedBatch,
                    onExpand = {
                        if (showArchive) expandedArchivedBatch = it else expandedBatch = it
                    },
                    archiveBack = if (showArchive) ::back else null,
                    canCreateBatch = templates.isNotEmpty(),
                    onSettings = { page = Page.Settings },
                    onStart = { id, at ->
                        change {
                            repository.setPhaseStart(id, at)
                            if (preferences?.reminders == true && !notificationsAllowed) enableNotifications()
                        }
                    },
                    onComplete = { id -> change { repository.complete(id) } },
                    onArchive = {
                        archiveBatchId = it.panettone.id
                        dialog = Dialog.ArchiveBatch
                    },
                )
            }
        }
    }
    when (dialog) {
        Dialog.Batch -> NameDialog("Aggiungi panettone", dismiss) { name ->
            dismiss()
            change { expandedBatch = repository.addBatch(name) }
        }
        Dialog.ArchiveBatch -> batches?.firstOrNull { it.panettone.id == archiveBatchId }?.let { batch ->
            ArchiveBatchDialog(batch.panettone.name, dismiss) {
                dismiss()
                change {
                    val id = batch.panettone.id
                    val archived = repository.archiveCompletedBatch(id)
                    if (archived && expandedBatch == id) expandedBatch = null
                    scope.launch {
                        snackbar.showSnackbar(
                            if (archived) "Panettone archiviato." else "Il panettone non è più completato.",
                        )
                    }
                }
            }
        }
        Dialog.Template -> if (templateId == null || selectedTemplate != null) {
            TemplateDialog(selectedTemplate, dismiss) { template ->
                dismiss()
                change { repository.saveTemplate(template) }
            }
        }
        Dialog.RemoveTemplate -> selectedTemplate?.let { template ->
            RemoveTemplateDialog(template, dismiss) {
                dismiss()
                change { repository.removeTemplate(template) }
            }
        }
        null -> Unit
    }
}
