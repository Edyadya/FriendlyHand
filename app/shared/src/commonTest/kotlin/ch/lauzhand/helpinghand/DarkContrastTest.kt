package ch.lauzhand.helpinghand

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import ch.lauzhand.helpinghand.ui.DarkApp
import ch.lauzhand.helpinghand.ui.DarkColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Dark mode is high contrast. Body text 7:1, small text 4.5:1, borders / icons / controls 3:1.
 * Page #010B22, cards #0B1F55, raised #142B6B, borders #4A67B8.
 */
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
    private val surfaces = listOf("page" to c.background, "card" to c.surface, "raised" to app.surfaceHigh)

    @Test
    fun theSurfacesAreTheSpecifiedColours() {
        assertEquals(Color(0xFF010B22), c.background)
        assertEquals(Color(0xFF0B1F55), c.surface)
        assertEquals(Color(0xFF142B6B), app.surfaceHigh)
        assertEquals(Color(0xFF4A67B8), c.outline)
        assertEquals(Color(0xFFFFFFFF), c.onSurface)
        assertEquals(Color(0xFFC9D6FF), c.onSurfaceVariant)
        assertEquals(Color(0xFF8EAEFF), c.primary)
    }

    @Test
    fun bodyAndSecondaryTextAreAAAOnEverySurface() {
        for ((name, bg) in surfaces) {
            atLeast(7.0, "primary text on $name", c.onSurface, bg)
            atLeast(7.0, "secondary text on $name", c.onSurfaceVariant, bg)
        }
        atLeast(7.0, "unselected tab on the bar", app.navUnselected, c.surface)
        atLeast(7.0, "white on the navy panel", app.onNavy, app.navy)
    }

    @Test
    fun smallBlueTextAndButtonsAreReadable() {
        for ((name, bg) in surfaces) atLeast(4.5, "blue link / outlined button text on $name", c.primary, bg)
        atLeast(6.9, "primary button text", c.onPrimary, c.primary)
        atLeast(4.5, "disabled button text", app.lockedContent, app.locked)
        atLeast(4.5, "error text on a card", c.error, c.surface)
        atLeast(4.5, "success text on a card", app.success, c.surface)
        atLeast(7.0, "text on a gold reward", app.onGold, app.gold)
    }

    @Test
    fun bordersAndControlsAreVisible() {
        atLeast(3.0, "border against the page", c.outline, c.background)
        atLeast(3.0, "card edge (outline) against the page", c.outlineVariant, c.background)
        atLeast(3.0, "accent against the card", app.accent, c.surface)
    }

    @Test
    fun progressBarsShowFilledAgainstEmpty() {
        atLeast(3.0, "blue fill on its track", app.progressFill, app.progressTrack)
        atLeast(3.0, "gold reward fill on its track", app.rewardFill, app.progressTrack)
        assertEquals(app.gold, app.rewardFill)
        // an empty bar: its track still shows against the card
        atLeast(1.3, "empty track on a card", app.progressTrack, c.surface)
    }

    @Test
    fun mapBubbleNumbersAreReadable() {
        assertEquals(Color(0xFF3B7BFF), app.bubble) // patrons keep the accent blue
        atLeast(3.0, "white number on the patron bubble (large, bold)", Color.White, app.bubble)
        atLeast(4.5, "white number on the volunteer bubble", Color.White, app.volunteer)
    }
}
