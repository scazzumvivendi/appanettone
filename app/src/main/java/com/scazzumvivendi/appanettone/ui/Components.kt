package com.scazzumvivendi.appanettone.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scazzumvivendi.appanettone.R
import com.scazzumvivendi.appanettone.data.PhaseColor
import com.scazzumvivendi.appanettone.ui.Page

private val monochromeLogo = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    currentPage: Page,
    isSupporter: Boolean,
    onSettings: () -> Unit,
    onArchive: () -> Unit,
    onPanettoni: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var menuPage by remember { mutableStateOf(currentPage) }
    TopAppBar(
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.panettone),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)),
                    colorFilter = if (isSupporter) null else monochromeLogo,
                )
                Text("Appanettone", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        actions = {
            Box {
                IconButton(onClick = {
                    menuPage = currentPage
                    menuOpen = true
                }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Apri menu")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (menuPage != Page.Archive){
                    DropdownMenuItem(
                        text = { Text("Archivio") },
                        onClick = {
                            menuOpen = false
                            onArchive()
                        },
                    )}
                    if (menuPage != Page.Batches){
                        DropdownMenuItem(
                            text = { Text("Panettoni") },
                            onClick = {
                                menuOpen = false
                                onPanettoni()
                            },
                        )}
                    if (menuPage != Page.Settings) {
                        DropdownMenuItem(
                            text = { Text("Impostazioni") },
                            onClick = {
                                menuOpen = false
                                onSettings()
                            },
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

@Composable
fun ScreenHeading(title: String, onBack: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun SoftCard(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    val shape = RoundedCornerShape(22.dp)
    if (onClick == null) {
        Card(Modifier.fillMaxWidth(), shape = shape, colors = colors) { content() }
    } else {
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = shape, colors = colors) { content() }
    }
}

private fun phaseColors(index: Int): Pair<Color, Color> =
    when (PhaseColor.entries[index.mod(PhaseColor.entries.size)]) {
        PhaseColor.Amber -> Color(0xFFFFDC9A) to Color(0xFF392800)
        PhaseColor.Blue -> Color(0xFFCFE0F8) to Color(0xFF152E4D)
        PhaseColor.Lilac -> Color(0xFFE5D9F3) to Color(0xFF37234C)
        PhaseColor.Rose -> Color(0xFFF1D5CF) to Color(0xFF512923)
        PhaseColor.Teal -> Color(0xFFBEE8E1) to Color(0xFF123E3A)
        PhaseColor.Apricot -> Color(0xFFFFBF95) to Color(0xFF55260E)
        PhaseColor.Indigo -> Color(0xFFC6CDF8) to Color(0xFF272D60)
    }

data class PhaseStatus(val label: String, val color: Int = -1)

@Composable
fun PhasePill(status: PhaseStatus, modifier: Modifier = Modifier) {
    val (background, foreground) = when (status.color) {
        -2 -> Color(0xFFD5E5CE) to Color(0xFF263F23)
        -1 -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface
        else -> phaseColors(status.color)
    }
    Surface(modifier = modifier, shape = CircleShape, color = background, contentColor = foreground) {
        Text(
            status.label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun EmptyCard(title: String, message: String) {
    SoftCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun durationLabel(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours == 0 -> "$remainder min"
        remainder == 0 -> "$hours h"
        else -> "$hours h $remainder min"
    }
}
