package it.appanettone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.appanettone.data.PhaseTemplate

@Composable
fun SettingsScreen(
    templates: List<PhaseTemplate>,
    reminders: Boolean,
    notificationsAllowed: Boolean,
    exactAlarmsAllowed: Boolean,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (PhaseTemplate) -> Unit,
    onRemove: (PhaseTemplate) -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onNotificationPermission: () -> Unit,
    onExactAlarmPermission: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeading("Impostazioni", onBack) }
        item { Text("Fasi di lievitazione", style = MaterialTheme.typography.titleLarge) }
        items(templates, key = { it.id }) { template ->
            SoftCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { onEdit(template) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    ) {
                        Text(template.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        PhasePill(PhaseStatus(durationLabel(template.minutes)))
                    }
                    IconButton(onClick = { onRemove(template) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Rimuovi ${template.name}")
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null, Modifier.padding(end = 6.dp))
                Text("Aggiungi fase")
            }
        }
        item { Text("Notifiche", style = MaterialTheme.typography.titleLarge) }
        item {
            SoftCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Avviso 5 min prima", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = reminders,
                            onCheckedChange = onToggleReminders,
                            modifier = Modifier.semantics { contentDescription = "Avviso 5 min prima" },
                        )
                    }
                    if (reminders && !notificationsAllowed) {
                        Text("Consenti le notifiche per ricevere i promemoria di lievitazione.")
                        OutlinedButton(onClick = onNotificationPermission) { Text("Consenti notifiche") }
                    } else if (reminders && !exactAlarmsAllowed) {
                        Text("Per un avviso puntuale, consenti gli allarmi esatti nelle impostazioni Android.")
                        OutlinedButton(onClick = onExactAlarmPermission) { Text("Consenti allarmi esatti") }
                    }
                }
            }
        }
    }
}
