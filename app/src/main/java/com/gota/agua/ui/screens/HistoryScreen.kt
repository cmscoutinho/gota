package com.gota.agua.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gota.agua.data.WaterState
import com.gota.agua.ui.components.SectionCard
import com.gota.agua.ui.components.StatCard
import com.gota.agua.util.PtBr
import com.gota.agua.util.formatLiters
import com.gota.agua.util.formatMl
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(state: WaterState) {
    val goal = state.settings.goalMl
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val values = days.map { state.history[it] ?: 0 }
    val average = values.average().roundToInt()
    val hits = values.count { it >= goal }
    val streak = streak(state.history, goal, today)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text("Histórico", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Seus últimos 7 dias",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(title = "Semana", icon = Icons.Rounded.BarChart) {
            WeekChart(days = days, values = values, goal = goal, today = today)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Média por dia", formatMl(average), Icons.Rounded.WaterDrop, Modifier.weight(1f))
            StatCard("Metas batidas", "$hits de 7", Icons.Rounded.EmojiEvents, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                "Sequência atual",
                if (streak == 1) "1 dia" else "$streak dias",
                Icons.Rounded.LocalFireDepartment,
                Modifier.weight(1f),
            )
            StatCard("Total da semana", formatLiters(values.sum()), Icons.Rounded.CalendarMonth, Modifier.weight(1f))
        }

        SectionCard(title = "Dia a dia", icon = Icons.Rounded.CalendarMonth) {
            days.reversed().forEach { day ->
                val ml = state.history[day] ?: 0
                DayRow(day = day, ml = ml, goal = goal, isToday = day == today)
            }
        }
    }
}

@Composable
private fun WeekChart(days: List<LocalDate>, values: List<Int>, goal: Int, today: LocalDate) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(900)) }

    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val goalColor = MaterialTheme.colorScheme.tertiary
    val maxValue = maxOf(goal, values.maxOrNull() ?: 0) * 1.12f

    Column {
        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
            val slot = size.width / values.size
            val barWidth = slot * 0.56f
            values.forEachIndexed { i, v ->
                val x = slot * i + (slot - barWidth) / 2f
                drawRoundRect(
                    color = track,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(barWidth / 2f),
                )
                val h = size.height * (v / maxValue) * progress.value
                if (h > 0f) {
                    val r = min(barWidth / 2f, h / 2f)
                    drawRoundRect(
                        color = if (v >= goal) primary else primary.copy(alpha = 0.5f),
                        topLeft = Offset(x, size.height - h),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(r),
                    )
                }
            }
            val goalY = size.height * (1f - goal / maxValue)
            drawLine(
                color = goalColor,
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f)),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            days.forEach { day ->
                Text(
                    text = weekdayShort(day),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal,
                    color = if (day == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            "Linha tracejada: meta de ${formatMl(goal)}",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayRow(day: LocalDate, ml: Int, goal: Int, isToday: Boolean) {
    val label = if (isToday) "Hoje" else day.format(DateTimeFormatter.ofPattern("EEE, d/MM", PtBr))
        .replace(".", "").replaceFirstChar { it.uppercase() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                formatMl(ml) + if (ml >= goal) "  ✓" else "",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (ml >= goal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { (ml.toFloat() / goal).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

private fun weekdayShort(day: LocalDate): String =
    day.dayOfWeek.getDisplayName(DateTextStyle.SHORT, PtBr).removeSuffix(".").replaceFirstChar { it.uppercase() }

private fun streak(history: Map<LocalDate, Int>, goal: Int, today: LocalDate): Int {
    var day = if ((history[today] ?: 0) >= goal) today else today.minusDays(1)
    var count = 0
    while ((history[day] ?: 0) >= goal) {
        count++
        day = day.minusDays(1)
    }
    return count
}
