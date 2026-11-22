package ch.lauzhand.helpinghand.qr

import kotlin.test.*

class QrCodeTest {
    @Test
    fun sizeGrowsWithTheTextAndStaysWithinVersion10() {
        assertEquals(21, QrCode.encode("hi").size)                    // version 1
        assertTrue(QrCode.encode("x".repeat(60)).size > 21)
        assertEquals(57, QrCode.encode("x".repeat(213)).size)         // version 10
        assertFailsWith<IllegalArgumentException> { QrCode.encode("x".repeat(214)) }
    }

    @Test
    fun hasTheThreeFinderPatternsAndTheTimingLines() {
        val q = QrCode.encode("HELPINGHAND|JETBRAINS|AB12CD34")
        for ((ox, oy) in listOf(0 to 0, q.size - 7 to 0, 0 to q.size - 7)) {
            for (i in 0 until 7) {
                assertTrue(q.isDark(ox + i, oy) && q.isDark(ox + i, oy + 6) && q.isDark(ox, oy + i) && q.isDark(ox + 6, oy + i))
            }
            assertFalse(q.isDark(ox + 1, oy + 1)) // the light ring inside the dark one
            assertTrue(q.isDark(ox + 3, oy + 3))  // the dark centre
        }
        for (i in 8 until q.size - 8) assertEquals(i % 2 == 0, q.isDark(i, 6)) // timing pattern
    }

    @Test
    fun theSameTextAlwaysGivesTheSameCodeAndDifferentTextDiffers() {
        fun dump(q: QrCode) = (0 until q.size).joinToString("") { y -> (0 until q.size).joinToString("") { x -> if (q.isDark(x, y)) "1" else "0" } }
        assertEquals(dump(QrCode.encode("abc")), dump(QrCode.encode("abc")))
        assertNotEquals(dump(QrCode.encode("abc")), dump(QrCode.encode("abd")))
    }
}
