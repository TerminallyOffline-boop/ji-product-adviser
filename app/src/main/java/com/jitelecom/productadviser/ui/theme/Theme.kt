package com.jitelecom.productadviser.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val Navy = Color(0xFF12345B)
private val Gold = Color(0xFFFFB547)
private val Teal = Color(0xFF0F766E)
private val LightColors = lightColorScheme(
    primary = Navy, onPrimary = Color.White, primaryContainer = Color(0xFFD9E8FF), onPrimaryContainer = Color(0xFF092644),
    secondary = Gold, onSecondary = Color(0xFF382800), secondaryContainer = Color(0xFFFFE2AD), onSecondaryContainer = Color(0xFF332300),
    tertiary = Teal, tertiaryContainer = Color(0xFFB9F1EA), onTertiaryContainer = Color(0xFF00201D),
    background = Color(0xFFF6F8FC), surface = Color(0xFFFEFBFF), surfaceVariant = Color(0xFFE7ECF4),
    error = Color(0xFFBA1A1A)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9CAFF), onPrimary = Color(0xFF00315D), primaryContainer = Color(0xFF16466F), onPrimaryContainer = Color(0xFFD7E8FF),
    secondary = Color(0xFFFFC867), onSecondary = Color(0xFF432E00), secondaryContainer = Color(0xFF5D4300), onSecondaryContainer = Color(0xFFFFE2AD),
    tertiary = Color(0xFF83D5CC), tertiaryContainer = Color(0xFF005049), onTertiaryContainer = Color(0xFFA2F2E9),
    background = Color(0xFF0E141C), surface = Color(0xFF151C25), surfaceVariant = Color(0xFF252E3A)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun JITheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = Typography(), shapes = AppShapes, content = content)
}
