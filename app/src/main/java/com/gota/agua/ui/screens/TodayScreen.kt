package com.gota.agua.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gota.agua.data.WaterState
import com.gota.agua.ui.components.IconBadge
import com.gota.agua.ui.components.WaterGauge
import com.gota.agua.util.PtBr
import com.gota.agua.util.formatInterval
import com.gota.agua.util.formatMinutesOfDay
import com.gota.agua.util.formatMl
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun TodayScreen(state: WaterState, onDrink: (Int) -> Unit, onUndo: () -> Unit) {
    val settings = state.settings
    val today = state.todayMl
    val remaining = (settings.goalMl - today).coerceAtLeast(0)
    var showCustom by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(greeting(), style = MaterialTheme.typography.headlineMedium)
            Text(
                todayLabel(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        WaterGauge(
            progress = state.progress,
            modifier = Modifier.fillMaxWidth(0.82f).widthIn(max = 340.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            ) {
                Column(
                    Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        formatMl(today),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "de ${formatMl(settings.goalMl)} · ${(state.progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(
            text = if (remaining == 0) {
                "Meta de hoje batida! 🎉"
            } else {
                val cups = ceil(remaining / settings.cupMl.toDouble()).toInt()
                "Faltam ${formatMl(remaining)}, cerca de $cups ${if (cups == 1) "copo" else "copos"}"
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )

        val amounts = (listOf(settings.cupMl) + listOf(150, 250, 350, 500)).distinct().take(3)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            amounts.forEachIndexed { index, ml ->
                DrinkButton(
                    title = "+$ml",
                    subtitle = if (index == 0) "seu copo" else "ml",
                    icon = if (index == 0) Icons.Rounded.LocalDrink else Icons.Rounded.WaterDrop,
                    highlighted = index == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { onDrink(ml) },
                )
            }
            DrinkButton(
                title = "Outro",
                subtitle = "valor",
                icon = Icons.Rounded.Add,
                highlighted = false,
                modifier = Modifier.weight(1f),
                onClick = { showCustom = true },
            )
        }

        AnimatedVisibility(visible = state.todayEntries.isNotEmpty()) {
            TextButton(onClick = onUndo) {
                Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Desfazer último (${formatMl(state.todayEntries.lastOrNull() ?: 0)})")
            }
        }

        NextReminderCard(state)
    }

    if (showCustom) {
        CustomAmountDialog(
            onDismiss = { showCustom = false },
            onConfirm = { ml ->
                onDrink(ml)
                showCustom = false
            },
        )
    }
}

@Composable
private fun DrinkButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    highlighted: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val container = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
    val content = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(24.dp),
        color = container,
        contentColor = content,
    ) {
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun NextReminderCard(state: WaterState) {
    val s = state.settings
    val title = when {
        !s.remindersEnabled -> "Lembretes desligados"
        state.nextReminderAt <= 0L -> "Agendando lembretes…"
        else -> {
            val at = Instant.ofEpochMilli(state.nextReminderAt).atZone(ZoneId.systemDefault())
            val time = at.format(DateTimeFormatter.ofPattern("HH:mm"))
            if (at.toLocalDate() == LocalDate.now()) "Próximo lembrete às $time" else "Próximo lembrete amanhã, às $time"
        }
    }
    val subtitle = if (s.remindersEnabled) {
        "A cada ${formatInterval(s.intervalMinutes)}, das ${formatMinutesOfDay(s.startMinutes)} às ${formatMinutesOfDay(s.endMinutes)}"
    } else {
        "Ligue os lembretes na aba Ajustes"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                if (s.remindersEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                container = MaterialTheme.colorScheme.secondaryContainer,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CustomAmountDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var value by remember { mutableFloatStateOf(200f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.WaterDrop, contentDescription = null) },
        title = { Text("Quanto você bebeu?") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatMl(value.roundToInt()),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 50f..1000f,
                    steps = 18,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(value.roundToInt()) }) { Text("Adicionar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private fun greeting(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 5 -> "Boa noite"
        hour < 12 -> "Bom dia"
        hour < 18 -> "Boa tarde"
        else -> "Boa noite"
    }
}

private fun todayLabel(): String =
    LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PtBr))
        .replaceFirstChar { it.uppercase() }
