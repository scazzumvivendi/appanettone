package com.scazzumvivendi.appanettone.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DateTimeLabel = DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.ITALIAN)

fun phaseDateTime(timestamp: Long?): String {
    if (timestamp != null){
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeLabel)
    } else {
        return ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhaseStartPicker(initialStart: Long, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    val zone = ZoneId.systemDefault()
    val initial = Instant.ofEpochMilli(initialStart).atZone(zone)
    val date = rememberDatePickerState(
        initialSelectedDateMillis = initial.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        initialDisplayMode = DisplayMode.Input,
    )
    val time = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    var editingTime by rememberSaveable { mutableStateOf(false) }
    if (editingTime) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Orario di inizio") },
            text = { TimeInput(state = time) },
            dismissButton = { TextButton(onClick = { editingTime = false }) { Text("Indietro") } },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = date.selectedDateMillis ?: return@TextButton
                    val localDate = Instant.ofEpochMilli(selectedDate).atOffset(ZoneOffset.UTC).toLocalDate()
                    onSave(localDate.atTime(time.hour, time.minute).atZone(zone).toInstant().toEpochMilli())
                }) { Text("Salva") }
            },
        )
    } else {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
            confirmButton = {
                TextButton(onClick = { editingTime = true }, enabled = date.selectedDateMillis != null) {
                    Text("Continua")
                }
            },
        ) {
            DatePicker(state = date, title = { Text("Data di inizio", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) })
        }
    }
}
