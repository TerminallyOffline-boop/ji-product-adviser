package com.jitelecom.productadviser.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF0A1E33)
private val Blue = Color(0xFF1769AA)
private val Teal = Color(0xFF007E78)
private val Amber = Color(0xFFF09A22)

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EAFF),
    onPrimaryContainer = Color(0xFF003257),
    secondary = Amber,
    onSecondary = Color(0xFF3B2700),
    secondaryContainer = Color(0xFFFFE1AF),
    onSecondaryContainer = Color(0xFF2D1E00),
    tertiary = Teal,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB9F3ED),
    onTertiaryContainer = Color(0xFF00201E),
    background = Color(0xFFF6F8FC),
    onBackground = Ink,
    surface = Color(0xFFFCFCFF),
    onSurface = Ink,
    surfaceVariant = Color(0xFFE6EDF5),
    onSurfaceVariant = Color(0xFF435365),
    outline = Color(0xFF718295),
    outlineVariant = Color(0xFFC3CFDC),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DCCFF),
    onPrimary = Color(0xFF003257),
    primaryContainer = Color(0xFF004A78),
    onPrimaryContainer = Color(0xFFD1E9FF),
    secondary = Color(0xFFFFC563),
    onSecondary = Color(0xFF432D00),
    secondaryContainer = Color(0xFF604400),
    onSecondaryContainer = Color(0xFFFFDEA7),
    tertiary = Color(0xFF91D9D2),
    onTertiary = Color(0xFF003735),
    tertiaryContainer = Color(0xFF00504C),
    onTertiaryContainer = Color(0xFFAEF5EE),
    background = Color(0xFF0D1520),
    onBackground = Color(0xFFDEE7F2),
    surface = Color(0xFF121C28),
    onSurface = Color(0xFFDEE7F2),
    surfaceVariant = Color(0xFF263442),
    onSurfaceVariant = Color(0xFFBECCDC),
    outline = Color(0xFF8A9AAA),
    outlineVariant = Color(0xFF3F4E5D)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 36.sp, lineHeight = 41.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.25).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun JITheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = AppTypography, shapes = AppShapes, content = content)
}

