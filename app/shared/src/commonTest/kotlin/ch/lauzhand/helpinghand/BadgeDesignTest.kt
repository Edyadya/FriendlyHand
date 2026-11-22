package ch.lauzhand.helpinghand

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import ch.lauzhand.helpinghand.ui.*
import kotlin.test.*

/** The badge medallions: contrast of rings, icons and text in both themes, and the "Next up" choice. */
class BadgeDesignTest {
    private fun ratio(a: Color, b: Color): Double {
        val hi = maxOf(a.luminance(), b.luminance()).toDouble()
        val lo = minOf(a.luminance(), b.luminance()).toDouble()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun atLeast(min: Double, name: String, a: Color, b: Color) {
        val r = ratio(a, b)
        assertTrue(r >= min, "$name: ${"%.2f".format(r)}:1, wanted at least $min:1")
    }

    @Test
    fun ringsAndIconsReachThreeToOneInBothThemes() {
        for ((theme, app, c) in listOf(Triple("light", LightApp, LightColors), Triple("dark", DarkApp, DarkColors))) {
            atLeast(3.0, "$theme: progress ring on the tile", app.progressFill, c.surface)
            atLeast(3.0, "$theme: gold ring of an unlocked badge on the tile", app.goldRing, c.surface)
            atLeast(3.0, "$theme: muted locked icon on the pale medallion", c.onSurfaceVariant.copy(alpha = 0.75f).compositeOver(app.medalLocked), app.medalLocked)
            atLeast(4.5, "$theme: navy icon on the gold medallion", app.onGold, app.gold)
            atLeast(4.5, "$theme: navy icon on the end of the gold gradient", app.onGold, app.goldLight)
            atLeast(3.0, "$theme: secondary shapes of an unlocked icon on gold", app.onGold.copy(alpha = 0.7f).compositeOver(app.gold), app.gold)
            atLeast(4.5, "$theme: name on the tile", c.onSurface, c.surface)
            atLeast(4.5, "$theme: count on the tile", c.onSurfaceVariant, c.surface)
            atLeast(4.5, "$theme: group heading on the page", c.onSurfaceVariant, c.background)
        }
    }

    @Test
    fun theThreeSpecifiedColoursAreUsed() {
        assertEquals(Color(0xFFE8EEFB), LightApp.medalLocked)
        assertEquals(Color(0xFF142B6B), DarkApp.medalLocked)
        assertEquals(Color(0xFFF1B604), LightApp.gold)
        assertEquals(Color(0xFFFFD966), LightApp.goldLight)
        assertEquals(Color(0xFF3B7BFF), LightApp.progressFill) // the blue ring
    }

    private class T(val key: String, val unlocked: Boolean, val progress: Int, val target: Int) {
        val fraction get() = if (unlocked) 1f else progress.toFloat() / target
    }

    private fun next(tiles: List<T>) = pickNextUp(tiles, { it.unlocked }, { it.fraction })?.key

    @Test
    fun nextUpIsTheLockedBadgeClosestToUnlocking() {
        val tiles = listOf(T("done", true, 1, 1), T("far", false, 1, 10), T("close", false, 4, 5), T("zero", false, 0, 3))
        assertEquals("close", next(tiles))
        // nothing started: the first locked one
        assertEquals("a", next(listOf(T("done", true, 1, 1), T("a", false, 0, 3), T("b", false, 0, 5))))
        // everything unlocked: no card
        assertNull(next(listOf(T("x", true, 1, 1))))
    }

    @Test
    fun everyBadgeHasItsOwnPicture() {
        val patron = ch.lauzhand.helpinghand.model.AchievementId.entries.map { it.glyph() }
        assertEquals(patron.size, patron.toSet().size, "patron badges share a picture")
        val volunteer = ch.lauzhand.helpinghand.model.VolunteerBadge.entries.map { it.glyph() }
        assertEquals(volunteer.size, volunteer.toSet().size, "volunteer badges share a picture")
    }
}
