package com.gota.agua.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF0A7BD6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EAFF),
    onPrimaryContainer = Color(0xFF00294D),
    secondary = Color(0xFF009E9A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8F1EE),
    onSecondaryContainer = Color(0xFF00302E),
    tertiary = Color(0xFF6A5AE0),
    background = Color(0xFFF4F8FD),
    onBackground = Color(0xFF0F1D2A),
    surface = Color(0xFFF4F8FD),
    onSurface = Color(0xFF0F1D2A),
    surfaceVariant = Color(0xFFDFE9F4),
    onSurfaceVariant = Color(0xFF4A5868),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEAF2FB),
    surfaceContainerHigh = Color(0xFFE2ECF7),
    surfaceContainerHighest = Color(0xFFDAE6F3),
    outlineVariant = Color(0xFFC9D6E3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8CC8FF),
    onPrimary = Color(0xFF00325A),
    primaryContainer = Color(0xFF0B4A80),
    onPrimaryContainer = Color(0xFFD6EAFF),
    secondary = Color(0xFF6FD9D4),
    onSecondary = Color(0xFF003735),
    secondaryContainer = Color(0xFF00504D),
    onSecondaryContainer = Color(0xFFC8F1EE),
    tertiary = Color(0xFFC4BBFF),
    background = Color(0xFF0D1620),
    onBackground = Color(0xFFE2EAF3),
    surface = Color(0xFF0D1620),
    onSurface = Color(0xFFE2EAF3),
    surfaceVariant = Color(0xFF263341),
    onSurfaceVariant = Color(0xFFB9C6D3),
    surfaceContainerLowest = Color(0xFF09111A),
    surfaceContainerLow = Color(0xFF15202B),
    surfaceContainer = Color(0xFF192531),
    surfaceContainerHigh = Color(0xFF1F2C39),
    surfaceContainerHighest = Color(0xFF263442),
    outlineVariant = Color(0xFF34424F),
)

private val base = Typography()

private val GotaTypography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
)

private val GotaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun GotaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = GotaTypography,
        shapes = GotaShapes,
        content = content,
    )
}
