package ch.lauzhand.helpinghand

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import ch.lauzhand.helpinghand.ui.DarkApp
import ch.lauzhand.helpinghand.ui.DarkColors
import kotlin.test.Test
import kotlin.test.assertTrue

/** Dark mode is high contrast: text 7:1 (AAA), large bold numbers and UI lines 3:1 or better. */
class DarkContrastTest {
    private fun ratio(a: Color, b: Color): Double {
        val hi = maxOf(a.luminance(), b.luminance()).toDouble()
        val lo = minOf(a.luminance(), b.luminance()).toDouble()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun atLeast(min: Double, name: String, a: Color, b: Color) {
        val r = ratio(a, b)
        assertTrue(r >= min, "$name: ${"%.2f".format(r)}:1, wanted at least $min:1")
    }

    private val c = DarkColors
    private val app = DarkApp

    @Test
    fun bodyTextIsAAAOnEverySurface() {
        for ((name, bg) in listOf("background" to c.background, "surface" to c.surface)) {
            atLeast(7.0, "onSurface on $name", c.onSurface, bg)
            atLeast(7.0, "onSurfaceVariant on $name", c.onSurfaceVariant, bg)
            atLeast(7.0, "primary text on $name", c.primary, bg)
            atLeast(7.0, "unselected tab on $name", app.navUnselected, bg)
        }
        atLeast(7.0, "text on raised card", c.onSurface, c.surfaceContainerHigh)
        atLeast(7.0, "text on primary container", c.onPrimaryContainer, c.primaryContainer)
    }

    @Test
    fun buttonsAndLockedItemsAreReadable() {
        atLeast(7.0, "button text", c.onPrimary, c.primary)
        atLeast(7.0, "locked / disabled text", app.lockedContent, app.locked)
        atLeast(7.0, "white on the navy panel", app.onNavy, app.navy)
        atLeast(7.0, "error text", c.error, c.surface)
    }

    @Test
    fun bordersDividersAndTracksAreVisible() {
        for ((name, bg) in listOf("background" to c.background, "surface" to c.surface)) {
            atLeast(3.0, "card border / divider on $name", c.outlineVariant, bg)
            atLeast(3.0, "field border on $name", c.outline, bg)
        }
        atLeast(3.0, "panel outline on the background", c.outline, c.background)
        atLeast(3.0, "accent progress fill on the surface", app.accent, c.surface)
        atLeast(3.0, "badge track on a locked card", c.outline, app.locked)
    }

    @Test
    fun mapBubbleNumbersAreReadable() {
        atLeast(4.5, "white number on the patron bubble", Color.White, app.bubble)
        atLeast(4.5, "white number on the volunteer bubble", Color.White, app.volunteer)
    }
}
