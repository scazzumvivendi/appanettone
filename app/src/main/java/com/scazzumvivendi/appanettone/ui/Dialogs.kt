package com.scazzumvivendi.appanettone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.scazzumvivendi.appanettone.data.PhaseTemplate

@Composable
fun NameDialog(title: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    EditorDialog(title, name.isNotBlank(), onDismiss, { onSave(name.trim()) }) {
        NameInput(name, { name = it })
    }
}

@Composable
fun TemplateDialog(template: PhaseTemplate?, onDismiss: () -> Unit, onSave: (PhaseTemplate) -> Unit) {
    var name by rememberSaveable { mutableStateOf(template?.name.orEmpty()) }
    var hours by rememberSaveable { mutableStateOf(template?.let { (it.minutes / 60).toString() }.orEmpty()) }
    var minutes by rememberSaveable { mutableStateOf(template?.let { (it.minutes % 60).toString() }.orEmpty()) }
    val hoursValue = hours.toIntOrNull() ?: 0
    val minutesValue = minutes.toIntOrNull() ?: 0
    val duration = hoursValue * 60 + minutesValue
    val valid = name.isNotBlank() && minutesValue in 0..59 && duration > 0

    EditorDialog(
        title = if (template == null) "Aggiungi fase" else "Modifica fase",
        valid = valid,
        onDismiss = onDismiss,
        onSave = { onSave(PhaseTemplate(template?.id ?: 0, name.trim(), duration, template?.color ?: 0)) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NameInput(name, { name = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DurationInput("Ore", hours, { hours = it }, Modifier.weight(1f))
                DurationInput("Minuti", minutes, { minutes = it }, Modifier.weight(1f), minutesValue > 59)
            }
        }
    }
}

@Composable
fun RemoveTemplateDialog(template: PhaseTemplate, onDismiss: () -> Unit, onRemove: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rimuovere la fase?") },
        text = { Text("«${template.name}» verrà rimossa dai panettoni in cui deve ancora iniziare. Le fasi già avviate resteranno visibili.") },
        confirmButton = { TextButton(onClick = onRemove) { Text("Rimuovi") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

@Composable
private fun EditorDialog(
    title: String,
    valid: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = content,
        confirmButton = { TextButton(onClick = onSave, enabled = valid) { Text("Salva") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

@Composable
private fun NameInput(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 80) onChange(it) },
        label = { Text("Nome") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
}

@Composable
private fun DurationInput(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 2 && it.all { char -> char in '0'..'9' }) onChange(it) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        isError = isError,
    )
}

@Composable
fun ArchiveBatchDialog(name: String, onDismiss: () -> Unit, onArchive: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Archiviare il panettone?") },
        text = { Text("«$name» verrà rimosso dalla lista. Il suo storico sarà disponibile nell'archivio") },
        confirmButton = {
            TextButton(onClick = onArchive) {
                Text("Archivia")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
