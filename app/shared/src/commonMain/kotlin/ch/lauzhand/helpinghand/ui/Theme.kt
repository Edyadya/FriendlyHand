package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import org.jetbrains.compose.resources.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import helpinghand.app.shared.generated.resources.*

// ---------------------------------------------------------------------------------------------
// Design system. Every colour is defined here once; screens use MaterialTheme.colorScheme or
// MaterialTheme.app (gold, success, accent, ...) and never hard-code a hex value.
// ---------------------------------------------------------------------------------------------

/** Colours that Material's ColorScheme has no slot for. */
@Immutable
class AppColors(
    /** Map bubbles, progress fill, links, decorative shapes, icons. NOT small text, NOT a fill for white text. */
    val accent: Color,
    /** Text on [accent]: navy, never white. */
    val onAccent: Color,
    /** ONLY for rewards: unlocked badges, completed stamps, voucher highlight, confetti. */
    val gold: Color,
    /** Text on [gold]: navy, never white. */
    val onGold: Color,
    /** Confirmed delivery, completed status. */
    val success: Color,
    val onSuccess: Color,
    /** The brand navy: stat panels, unlocked badge cards. Same in light and dark. */
    val navy: Color,
    val onNavy: Color,
    /** Volunteer map bubbles (teal), white text on it. Same in light and dark. */
    val volunteer: Color,
    /** Patron map bubbles, white number on it (dark mode uses a deeper blue so the number reaches about 5:1). */
    val bubble: Color,
    /** Raised cards (dark theme uses a lighter blue). */
    val surfaceHigh: Color,
    val onSurfaceHigh: Color,
    /** Locked / disabled fill and its text and icon colour. */
    val locked: Color,
    val lockedContent: Color,
    /** Unselected navigation items. */
    val navUnselected: Color,
    /** Soft screen gradient (login): strong end at the top, fading to [gradientEnd] behind the form. */
    val gradientStart: Color,
    val gradientEnd: Color,
    /** Pale blue, for confetti and decorative shapes only. */
    val pale: Color,
)

private val Navy = Color(0xFF02215E)
private val Gold = Color(0xFFF1B604)
private val Accent = Color(0xFF3B7BFF)

private val LightApp = AppColors(
    accent = Accent, onAccent = Navy,
    gold = Gold, onGold = Navy,
    success = Color(0xFF2E9E6B), onSuccess = Navy,
    navy = Navy, onNavy = Color(0xFFFFFFFF),
    volunteer = Color(0xFF1F7A8C), bubble = Accent,
    surfaceHigh = Color(0xFFFFFFFF), onSurfaceHigh = Navy,
    locked = Color(0xFFC9D3E8), lockedContent = Color(0xFF7A88A8),
    navUnselected = Color(0xFF7A88A8),
    gradientStart = Color(0xFFDCE6FF), gradientEnd = Color(0xFFF4F7FE),
    pale = Color(0xFF8EAEFF),
)

internal val DarkApp = AppColors(
    accent = Accent, onAccent = Navy,
    gold = Gold, onGold = Navy,
    success = Color(0xFF4CC38A), onSuccess = Navy,
    navy = Navy, onNavy = Color(0xFFFFFFFF),
    volunteer = Color(0xFF1F7A8C), bubble = Color(0xFF2A66EC),
    surfaceHigh = Color(0xFF103F9B), onSurfaceHigh = Color(0xFFF4F7FE),
    locked = Color(0xFF2A3F73), lockedContent = Color(0xFFE8EEFB),   // 8.8:1
    navUnselected = Color(0xFFD0D9F0),                                 // 10.8:1
    gradientStart = Color(0xFF02215E), gradientEnd = Color(0xFF01163F),
    pale = Color(0xFF8EAEFF),
)

private val LocalAppColors = staticCompositionLocalOf { LightApp }

/** True while the dark scheme is showing (the system setting, or the user's override in Settings). */
val LocalDarkTheme = staticCompositionLocalOf { false }
val MaterialTheme.isDark: Boolean
    @Composable @ReadOnlyComposable get() = LocalDarkTheme.current

/** Use as MaterialTheme.app.gold, MaterialTheme.app.success, ... */
val MaterialTheme.app: AppColors
    @Composable @ReadOnlyComposable get() = LocalAppColors.current

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF103F9B), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3EBFF), onPrimaryContainer = Navy,
    secondary = Accent, onSecondary = Navy,
    secondaryContainer = Color(0xFFDCE7FF), onSecondaryContainer = Navy,
    tertiary = Color(0xFF2E9E6B), onTertiary = Navy,
    tertiaryContainer = Color(0xFFD7F1E4), onTertiaryContainer = Navy,
    background = Color(0xFFF4F7FE), onBackground = Navy,
    surface = Color(0xFFFFFFFF), onSurface = Navy,
    surfaceVariant = Color(0xFFFFFFFF), onSurfaceVariant = Color(0xFF4A5B82),
    surfaceTint = Color(0xFF103F9B),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF), surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFFFFFFF),
    outline = Color(0xFF7A88A8), outlineVariant = Color(0xFFD5DEF0),
    inverseSurface = Navy, inverseOnSurface = Color(0xFFF4F7FE), inversePrimary = Color(0xFF8EAEFF),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
)

internal val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFB8CCFF), onPrimary = Navy,                     // 9.5:1 on the surface, navy on it 9.5:1
    primaryContainer = Color(0xFF103F9B), onPrimaryContainer = Color(0xFFF4F7FE),
    secondary = Accent, onSecondary = Navy,
    secondaryContainer = Color(0xFF16408F), onSecondaryContainer = Color(0xFFF4F7FE),
    tertiary = Color(0xFF4CC38A), onTertiary = Navy,
    tertiaryContainer = Color(0xFF14503A), onTertiaryContainer = Color(0xFFF4F7FE),
    background = Color(0xFF01163F), onBackground = Color(0xFFF4F7FE),
    surface = Color(0xFF02215E), onSurface = Color(0xFFF4F7FE),
    surfaceVariant = Color(0xFF02215E), onSurfaceVariant = Color(0xFFD0D9F0),   // 10.8:1
    surfaceTint = Color(0xFF8EAEFF),
    surfaceContainerLowest = Color(0xFF01163F), surfaceContainerLow = Color(0xFF02215E),
    surfaceContainer = Color(0xFF02215E), surfaceContainerHigh = Color(0xFF103F9B),
    surfaceContainerHighest = Color(0xFF103F9B),
    // outline: field borders, panel borders (7.7:1). outlineVariant: card borders, dividers, progress tracks (4.2:1, was 1.5:1)
    outline = Color(0xFFA9B8DC), outlineVariant = Color(0xFF6B85C2),
    inverseSurface = Color(0xFFF4F7FE), inverseOnSurface = Navy, inversePrimary = Color(0xFF103F9B),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
)

// Cards 16 dp, buttons and chips 12 dp, bottom sheets 24 dp on top.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Shapes screens use directly. */
object AppShape {
    val button = RoundedCornerShape(12.dp)
    val chip = RoundedCornerShape(12.dp)
    val card = RoundedCornerShape(16.dp)
    val sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}

/** Tabular numerals, so counters and amounts don't jiggle while they change. */
val NumeralStyle = TextStyle(fontFeatureSettings = "tnum")

@Composable
private fun appTypography(): Typography {
    val body = FontFamily(
        Font(Res.font.poppins_regular, FontWeight.Normal),
        Font(Res.font.poppins_medium, FontWeight.Medium),
        Font(Res.font.poppins_semibold, FontWeight.SemiBold),
        Font(Res.font.poppins_bold, FontWeight.Bold),
    )
    // Condensed, bold, capitals: headlines, screen titles and big stat numbers.
    val display = FontFamily(Font(Res.font.bebasneue_regular, FontWeight.Normal))
    fun head(size: Int, line: Int) = TextStyle(
        fontFamily = display, fontWeight = FontWeight.Normal, fontSize = size.sp, lineHeight = line.sp,
        letterSpacing = 0.5.sp, fontFeatureSettings = "tnum",
    )
    fun text(w: FontWeight, size: Int, line: Int, spacing: Double = 0.0) = TextStyle(
        fontFamily = body, fontWeight = w, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp,
    )
    return Typography(
        displayLarge = head(72, 76), displayMedium = head(60, 64), displaySmall = head(48, 52),
        headlineLarge = head(40, 44), headlineMedium = head(34, 38), headlineSmall = head(28, 32),
        titleLarge = text(FontWeight.SemiBold, 20, 28),
        titleMedium = text(FontWeight.SemiBold, 17, 24, 0.1),
        titleSmall = text(FontWeight.Medium, 14, 20, 0.1),
        bodyLarge = text(FontWeight.Normal, 17, 26),
        bodyMedium = text(FontWeight.Normal, 15, 22),
        bodySmall = text(FontWeight.Normal, 13, 18),
        labelLarge = text(FontWeight.SemiBold, 15, 20, 0.1),
        labelMedium = text(FontWeight.Medium, 13, 18, 0.2),
        labelSmall = text(FontWeight.Medium, 12, 16, 0.3),
    )
}

/** [darkOverride]: the user's choice in Settings; null follows the system. */
@Composable
fun HelpTheme(darkOverride: Boolean? = null, content: @Composable () -> Unit) {
    val dark = darkOverride ?: isSystemInDarkTheme()
    CompositionLocalProvider(
        LocalAppColors provides if (dark) DarkApp else LightApp,
        LocalDarkTheme provides dark,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = appTypography(),
            shapes = AppShapes,
            content = content,
        )
    }
}


/**
 * In dark mode a navy panel on the dark background is hard to tell apart (1.2:1), so it gets a bright outline
 * (7.7:1). In light mode nothing is added.
 */
@Composable
fun Modifier.darkOutline(shape: androidx.compose.ui.graphics.Shape): Modifier =
    if (MaterialTheme.isDark) this.border(1.5.dp, MaterialTheme.colorScheme.outline, shape) else this
