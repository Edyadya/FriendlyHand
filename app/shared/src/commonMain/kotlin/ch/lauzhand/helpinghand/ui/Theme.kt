package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6F6B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBDEBE6),
    onPrimaryContainer = Color(0xFF00201E),
    secondary = Color(0xFF8A5A2B),
    secondaryContainer = Color(0xFFFFDDBB),
    onSecondaryContainer = Color(0xFF2D1600),
    tertiaryContainer = Color(0xFFCFE4FF),
    onTertiaryContainer = Color(0xFF001D36),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD6CF),
    onPrimary = Color(0xFF003734),
    primaryContainer = Color(0xFF00504C),
    onPrimaryContainer = Color(0xFFBDEBE6),
    secondary = Color(0xFFF2BC8A),
    secondaryContainer = Color(0xFF6B4318),
    onSecondaryContainer = Color(0xFFFFDDBB),
    tertiaryContainer = Color(0xFF1F4A73),
    onTertiaryContainer = Color(0xFFCFE4FF),
)

// Slightly larger body text for readability.
private val AppTypography = Typography(
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
)

@Composable
fun HelpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
