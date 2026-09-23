package com.gota.agua.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gota.agua.data.Settings
import com.gota.agua.data.WaterState
import com.gota.agua.ui.components.SectionCard
import com.gota.agua.util.formatInterval
import com.gota.agua.util.formatMinutesOfDay
import com.gota.agua.util.formatMl
import com.gota.agua.util.openBatterySettings
import kotlin.math.roundToInt

private val intervalOptions = listOf(15, 20, 30, 45, 60, 90, 120, 150, 180)
private val cupOptions = listOf(150, 200, 250, 300, 400, 500)

@Composable
fun SettingsScreen(
    state: WaterState,
    notificationsAllowed: Boolean,
    onRequestNotifications: () -> Unit,
    onUpdate: (reschedule: Boolean, transform: (Settings) -> Settings) -> Unit,
    onTestNotification: () -> Unit,
) {
    val s = state.settings
    val context = LocalContext.current
    var editingStart by remember { mutableStateOf(false) }
    var editingEnd by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium)

        if (!notificationsAllowed) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.NotificationsOff, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Notificações bloqueadas", style = MaterialTheme.typography.titleSmall)
                        Text("Sem elas, o Gota não consegue te lembrar.", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = onRequestNotifications) { Text("Permitir") }
                }
            }
        }

        // ---------- Lembretes ----------
        SectionCard(title = "Lembretes", icon = Icons.Rounded.NotificationsActive) {
            SwitchRow(
                title = "Receber lembretes",
                subtitle = "Uma notificação no intervalo escolhido",
                checked = s.remindersEnabled,
                onChange = { on -> onUpdate(true) { it.copy(remindersEnabled = on) } },
            )

            var intervalIndex by remember(s.intervalMinutes) {
                mutableFloatStateOf(intervalOptions.indexOf(s.intervalMinutes).coerceAtLeast(0).toFloat())
            }
            val intervalValue = intervalOptions[intervalIndex.roundToInt()]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Intervalo", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    "a cada ${formatInterval(intervalValue)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = intervalIndex,
                onValueChange = { intervalIndex = it },
                valueRange = 0f..(intervalOptions.size - 1).toFloat(),
                steps = intervalOptions.size - 2,
                enabled = s.remindersEnabled,
                onValueChangeFinished = {
                    val chosen = intervalOptions[intervalIndex.roundToInt()]
                    onUpdate(true) { it.copy(intervalMinutes = chosen) }
                },
            )

            TimeRow("Começar às", s.startMinutes, s.remindersEnabled) { editingStart = true }
            TimeRow("Parar às", s.endMinutes, s.remindersEnabled) { editingEnd = true }

            SwitchRow(
                title = "Pausar ao bater a meta",
                subtitle = "Sem avisos depois que você atingir a meta do dia",
                checked = s.stopWhenGoalReached,
                enabled = s.remindersEnabled,
                onChange = { on -> onUpdate(false) { it.copy(stopWhenGoalReached = on) } },
            )
        }

        // ---------- Meta ----------
        SectionCard(title = "Meta diária", icon = Icons.Rounded.Flag) {
            var goal by remember(s.goalMl) { mutableFloatStateOf(s.goalMl.coerceIn(1000, 5000).toFloat()) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Beber por dia", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    formatMl(goal.roundToInt()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = goal,
                onValueChange = { goal = it },
                valueRange = 1000f..5000f,
                steps = 39,
                onValueChangeFinished = {
                    val chosen = goal.roundToInt()
                    onUpdate(false) { it.copy(goalMl = chosen) }
                },
            )

            var weightText by remember(s.weightKg) {
                mutableStateOf(if (s.weightKg > 0) s.weightKg.toString() else "")
            }
            val weight = weightText.toIntOrNull()
            val validWeight = weight != null && weight in 20..300
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { text -> weightText = text.filter { it.isDigit() }.take(3) },
                    label = { Text("Seu peso (kg)") },
                    leadingIcon = { Icon(Icons.Rounded.MonitorWeight, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                FilledTonalButton(
                    enabled = validWeight,
                    onClick = {
                        if (weight != null) {
                            val suggested = ((weight * 35 + 50) / 100 * 100).coerceIn(1000, 5000)
                            onUpdate(false) { it.copy(weightKg = weight, goalMl = suggested) }
                        }
                    },
                ) { Text("Calcular") }
            }
            Text(
                "Usa a referência comum de 35 ml por kg. Para uma meta sob medida, converse com um profissional de saúde.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ---------- Copo ----------
        SectionCard(title = "Seu copo", icon = Icons.Rounded.LocalDrink) {
            Text(
                "Usado no botão principal e na ação \"Bebi\" da notificação.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                cupOptions.forEach { ml ->
                    val selected = s.cupMl == ml
                    FilterChip(
                        selected = selected,
                        onClick = { onUpdate(false) { it.copy(cupMl = ml) } },
                        label = { Text("$ml ml") },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else {
                            null
                        },
                    )
                }
            }
        }

        // ---------- Ferramentas ----------
        SectionCard(title = "Ferramentas", icon = Icons.Rounded.Build) {
            OutlinedButton(onClick = onTestNotification, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Enviar notificação de teste")
            }
            OutlinedButton(onClick = { openBatterySettings(context) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.BatteryAlert, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Otimização de bateria")
            }
            Text(
                "Se os lembretes atrasarem, marque o Gota como \"Não otimizar\" nas configurações de bateria.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            "Gota 1.0",
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (editingStart) {
        TimeDialog(
            title = "Começar a lembrar às",
            initialMinutes = s.startMinutes,
            onDismiss = { editingStart = false },
            onConfirm = { minutes ->
                onUpdate(true) { it.copy(startMinutes = minutes) }
                editingStart = false
            },
        )
    }
    if (editingEnd) {
        TimeDialog(
            title = "Parar de lembrar às",
            initialMinutes = s.endMinutes,
            onDismiss = { editingEnd = false },
            onConfirm = { minutes ->
                onUpdate(true) { it.copy(endMinutes = minutes) }
                editingEnd = false
            },
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun TimeRow(label: String, minutes: Int, enabled: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        FilledTonalButton(onClick = onClick, enabled = enabled) {
            Text(formatMinutesOfDay(minutes), fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    title: String,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true,
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                )
                TimePicker(state = pickerState)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    TextButton(onClick = { onConfirm(pickerState.hour * 60 + pickerState.minute) }) { Text("Salvar") }
                }
            }
        }
    }
}
