package com.kaafisir.auramic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Brand palette: the "aura" runs violet -> cyan.
val AuraViolet = Color(0xFF7C5CFF)
val AuraVioletLight = Color(0xFF9B83FF)
val AuraIndigo = Color(0xFF5B3DF5)
val AuraCyan = Color(0xFF22D3EE)
val AuraPink = Color(0xFFFF5C93)
val AuraMint = Color(0xFF34D399)
val AuraRed = Color(0xFFFF4D6D)
val AuraAmber = Color(0xFFFFB84D)

val AuraGradient: Brush get() = Brush.linearGradient(listOf(AuraViolet, AuraCyan))
val AuraGradientWarm: Brush get() = Brush.linearGradient(listOf(AuraViolet, AuraPink))

private val DarkColors = darkColorScheme(
    primary = AuraVioletLight,
    onPrimary = Color(0xFF14083D),
    primaryContainer = Color(0xFF2A1F6B),
    onPrimaryContainer = Color(0xFFE6DEFF),
    secondary = AuraCyan,
    onSecondary = Color(0xFF00363F),
    secondaryContainer = Color(0xFF0F3A44),
    onSecondaryContainer = Color(0xFFBDF1FA),
    tertiary = AuraPink,
    background = Color(0xFF0A0A12),
    onBackground = Color(0xFFEDEDF7),
    surface = Color(0xFF12121C),
    onSurface = Color(0xFFEDEDF7),
    surfaceVariant = Color(0xFF1C1C2B),
    onSurfaceVariant = Color(0xFFA2A2BC),
    surfaceContainer = Color(0xFF171724),
    surfaceContainerHigh = Color(0xFF1E1E2D),
    outline = Color(0xFF3A3A55),
    outlineVariant = Color(0xFF26263A),
    error = AuraRed,
)

private val LightColors = lightColorScheme(
    primary = AuraIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E0FF),
    onPrimaryContainer = Color(0xFF1B0B66),
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFF4FC),
    onSecondaryContainer = Color(0xFF00363F),
    tertiary = Color(0xFFD6336C),
    background = Color(0xFFF6F6FB),
    onBackground = Color(0xFF15151F),
    surface = Color.White,
    onSurface = Color(0xFF15151F),
    surfaceVariant = Color(0xFFEDEBFA),
    onSurfaceVariant = Color(0xFF5C5C75),
    surfaceContainer = Color(0xFFF1F0FB),
    surfaceContainerHigh = Color(0xFFE9E7F8),
    outline = Color(0xFFC9C6E3),
    outlineVariant = Color(0xFFE0DDF2),
    error = Color(0xFFD92D4F),
)

private val AuraTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

private val AuraShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

@Composable
fun AuraTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AuraTypography,
        shapes = AuraShapes,
        content = content,
    )
}
