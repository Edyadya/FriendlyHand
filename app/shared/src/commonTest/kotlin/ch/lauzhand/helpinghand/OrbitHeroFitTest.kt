package ch.lauzhand.helpinghand

import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.ui.orbitExtent
import ch.lauzhand.helpinghand.ui.orbitHalf
import kotlin.test.Test
import kotlin.test.assertTrue

/** The login orbit's outer icons must stay inside the hero box (they used to be cropped by its edge). */
class OrbitHeroFitTest {

    // the login screen coerces the hero to 96..320 dp
    private val sizes = (96..320 step 8).map { it.dp } + listOf(97.dp, 193.dp, 194.dp, 195.dp, 255.dp)

    @Test
    fun outerIconsNeverReachPastTheBox() {
        for (d in sizes) {
            val room = d.value / 2
            val extent = orbitExtent(d).value
            assertTrue(extent <= room + 0.01f, "hero $d: artwork reaches $extent dp but the box only has $room dp")
        }
    }

    @Test
    fun theArtworkStillFillsTheBox() {
        // not needlessly small: the outermost icons touch the edge (within the outline margin)
        for (d in sizes) {
            val room = d.value / 2
            val extent = orbitExtent(d).value
            assertTrue(extent >= room - 0.01f, "hero $d: artwork only reaches $extent dp of $room dp")
        }
    }

    @Test
    fun ringsGrowWithTheBox() {
        var last = -1f
        for (d in sizes.sortedBy { it.value }) {
            val half = orbitHalf(d).value
            assertTrue(half >= last, "ring size should not shrink when the hero grows (at $d)")
            last = half
        }
    }
}
