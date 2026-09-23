package com.gota.agua.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** Círculo que se enche de água com ondas animadas conforme o progresso. */
@Composable
fun WaterGauge(
    progress: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val level by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "level",
    )
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = 3000, easing = LinearEasing)),
        label = "phase",
    )

    val front = MaterialTheme.colorScheme.primary
    val back = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
    val track = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    val ring = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)

    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val circle = Path().apply { addOval(Rect(center, radius)) }
            drawCircle(color = track, radius = radius)
            clipPath(circle) {
                val amplitude = radius * 0.06f
                drawPath(wavePath(size, level + 0.015f, phase + PI.toFloat(), amplitude * 0.8f), back)
                drawPath(wavePath(size, level, phase, amplitude), front)
            }
            drawCircle(color = ring, radius = radius - 4.dp.toPx(), style = Stroke(width = 8.dp.toPx()))
        }
        content()
    }
}

private fun wavePath(size: Size, level: Float, phase: Float, amplitude: Float): Path {
    val path = Path()
    if (level <= 0f) return path
    // Onda some suavemente quando o copo está quase vazio ou cheio.
    val fade = min(1f, min(level * 6f, (1f - level).coerceAtLeast(0f) * 6f))
    val amp = amplitude * fade
    val baseY = size.height * (1f - level.coerceAtMost(1f))
    val steps = 64
    path.moveTo(0f, size.height)
    for (i in 0..steps) {
        val x = size.width * i / steps
        val y = baseY + amp * sin(2.0 * PI * 1.4 * x / size.width + phase).toFloat()
        path.lineTo(x, y)
    }
    path.lineTo(size.width, size.height)
    path.close()
    return path
}

@Composable
fun IconBadge(
    icon: ImageVector,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    tint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            content()
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp)) {
            IconBadge(
                icon,
                container = MaterialTheme.colorScheme.secondaryContainer,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
