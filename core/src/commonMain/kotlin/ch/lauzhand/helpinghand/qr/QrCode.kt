package ch.lauzhand.helpinghand.qr

/**
 * A small QR code encoder: byte mode, error correction level M, versions 1 to 10 (up to 213 bytes). No library
 * needed. [isDark] tells whether the module at column x, row y is dark. Draw it on a light background with a
 * quiet zone of 4 modules around it.
 */
class QrCode private constructor(val version: Int, data: ByteArray) {
    val size: Int = version * 4 + 17
    private val modules = Array(size) { BooleanArray(size) }
    private val isFunction = Array(size) { BooleanArray(size) }

    fun isDark(x: Int, y: Int): Boolean = x in 0 until size && y in 0 until size && modules[y][x]

    init {
        drawFunctionPatterns()
        drawCodewords(addEccAndInterleave(data))
        // choose the mask with the lowest penalty
        var best = 0
        var bestPenalty = Int.MAX_VALUE
        for (m in 0 until 8) {
            applyMask(m)
            drawFormatBits(m)
            val p = penalty()
            if (p < bestPenalty) { best = m; bestPenalty = p }
            applyMask(m) // undo
        }
        applyMask(best)
        drawFormatBits(best)
    }

    // ---- function patterns
    private fun set(x: Int, y: Int, dark: Boolean) { modules[y][x] = dark; isFunction[y][x] = true }

    private fun drawFunctionPatterns() {
        for (i in 0 until size) { set(6, i, i % 2 == 0); set(i, 6, i % 2 == 0) }
        finder(3, 3); finder(size - 4, 3); finder(3, size - 4)
        val pos = alignmentPositions()
        val n = pos.size
        for (i in 0 until n) for (j in 0 until n) {
            if ((i == 0 && j == 0) || (i == 0 && j == n - 1) || (i == n - 1 && j == 0)) continue
            alignment(pos[i], pos[j])
        }
        drawFormatBits(0)
        drawVersion()
    }

    private fun finder(cx: Int, cy: Int) {
        for (dy in -4..4) for (dx in -4..4) {
            val d = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
            val x = cx + dx; val y = cy + dy
            if (x in 0 until size && y in 0 until size) set(x, y, d != 2 && d != 4)
        }
    }

    private fun alignment(cx: Int, cy: Int) {
        for (dy in -2..2) for (dx in -2..2) set(cx + dx, cy + dy, maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) != 1)
    }

    private fun alignmentPositions(): IntArray {
        if (version == 1) return IntArray(0)
        val n = version / 7 + 2
        val step = if (version == 32) 26 else (version * 4 + n * 2 + 1) / (n * 2 - 2) * 2
        val r = IntArray(n)
        r[0] = 6
        var i = n - 1
        var pos = size - 7
        while (i >= 1) { r[i] = pos; i--; pos -= step }
        return r
    }

    private fun bit(v: Int, i: Int) = ((v ushr i) and 1) != 0

    private fun drawFormatBits(mask: Int) {
        val data = (0 shl 3) or mask // error correction level M = 0
        var rem = data
        repeat(10) { rem = (rem shl 1) xor ((rem ushr 9) * 0x537) }
        val bits = ((data shl 10) or rem) xor 0x5412
        for (i in 0..5) set(8, i, bit(bits, i))
        set(8, 7, bit(bits, 6)); set(8, 8, bit(bits, 7)); set(7, 8, bit(bits, 8))
        for (i in 9..14) set(14 - i, 8, bit(bits, i))
        for (i in 0..7) set(size - 1 - i, 8, bit(bits, i))
        for (i in 8..14) set(8, size - 15 + i, bit(bits, i))
        set(8, size - 8, true)
    }

    private fun drawVersion() {
        if (version < 7) return
        var rem = version
        repeat(12) { rem = (rem shl 1) xor ((rem ushr 11) * 0x1F25) }
        val bits = (version shl 12) or rem
        for (i in 0 until 18) {
            val a = size - 11 + i % 3
            val b = i / 3
            set(a, b, bit(bits, i)); set(b, a, bit(bits, i))
        }
    }

    // ---- data
    private fun addEccAndInterleave(data: ByteArray): ByteArray {
        val numBlocks = BLOCKS[version]
        val eccLen = ECC_PER_BLOCK[version]
        val raw = rawDataModules(version) / 8
        val numShort = numBlocks - raw % numBlocks
        val shortLen = raw / numBlocks
        val divisor = rsDivisor(eccLen)
        val blocks = ArrayList<IntArray>()
        var k = 0
        for (i in 0 until numBlocks) {
            val datLen = shortLen - eccLen + (if (i < numShort) 0 else 1)
            val dat = IntArray(datLen) { data[k + it].toInt() and 0xFF }
            k += datLen
            val ecc = rsRemainder(dat, divisor)
            val block = IntArray(if (i < numShort) datLen + 1 + eccLen else datLen + eccLen)
            dat.copyInto(block)
            ecc.copyInto(block, block.size - eccLen)
            blocks.add(block)
        }
        val out = ByteArray(raw)
        var o = 0
        for (i in 0 until blocks[0].size + 1) {
            for ((j, block) in blocks.withIndex()) {
                if (i >= block.size) continue
                if (i != shortLen - eccLen || j >= numShort) out[o++] = block[i].toByte()
            }
        }
        return out
    }

    private fun drawCodewords(data: ByteArray) {
        var i = 0
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right = 5
            for (vert in 0 until size) for (j in 0 until 2) {
                val x = right - j
                val upward = ((right + 1) and 2) == 0
                val y = if (upward) size - 1 - vert else vert
                if (!isFunction[y][x] && i < data.size * 8) {
                    modules[y][x] = bit(data[i ushr 3].toInt(), 7 - (i and 7)); i++
                }
            }
            right -= 2
        }
    }

    private fun applyMask(mask: Int) {
        for (y in 0 until size) for (x in 0 until size) {
            val invert = when (mask) {
                0 -> (x + y) % 2 == 0
                1 -> y % 2 == 0
                2 -> x % 3 == 0
                3 -> (x + y) % 3 == 0
                4 -> (x / 3 + y / 2) % 2 == 0
                5 -> x * y % 2 + x * y % 3 == 0
                6 -> (x * y % 2 + x * y % 3) % 2 == 0
                else -> ((x + y) % 2 + x * y % 3) % 2 == 0
            }
            if (!isFunction[y][x] && invert) modules[y][x] = !modules[y][x]
        }
    }

    /** A simplified penalty (runs, 2x2 blocks, dark balance), only used to pick a mask. */
    private fun penalty(): Int {
        var result = 0
        for (y in 0 until size) {
            var run = 1
            for (x in 1 until size) {
                if (modules[y][x] == modules[y][x - 1]) { run++; if (run == 5) result += 3 else if (run > 5) result++ } else run = 1
            }
        }
        for (x in 0 until size) {
            var run = 1
            for (y in 1 until size) {
                if (modules[y][x] == modules[y - 1][x]) { run++; if (run == 5) result += 3 else if (run > 5) result++ } else run = 1
            }
        }
        for (y in 0 until size - 1) for (x in 0 until size - 1) {
            val c = modules[y][x]
            if (c == modules[y][x + 1] && c == modules[y + 1][x] && c == modules[y + 1][x + 1]) result += 3
        }
        var dark = 0
        for (row in modules) for (m in row) if (m) dark++
        val total = size * size
        result += (kotlin.math.abs(dark * 20 - total * 10) + total - 1) / total - 1
        return result
    }

    companion object {
        // error correction level M, versions 1..10 (index 0 unused)
        private val ECC_PER_BLOCK = intArrayOf(-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26)
        private val BLOCKS = intArrayOf(-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5)

        /** The largest text this encoder takes, in bytes. */
        const val MAX_BYTES = 213

        private fun rawDataModules(ver: Int): Int {
            var r = (16 * ver + 128) * ver + 64
            if (ver >= 2) {
                val n = ver / 7 + 2
                r -= (25 * n - 10) * n - 55
                if (ver >= 7) r -= 36
            }
            return r
        }

        private fun dataCodewords(ver: Int) = rawDataModules(ver) / 8 - ECC_PER_BLOCK[ver] * BLOCKS[ver]

        /** Encodes [text] as UTF-8. Throws if it does not fit in 213 bytes. */
        fun encode(text: String): QrCode {
            val bytes = text.encodeToByteArray()
            var ver = 1
            while (ver <= 10) {
                val countBits = if (ver <= 9) 8 else 16
                if (4 + countBits + bytes.size * 8 <= dataCodewords(ver) * 8) break
                ver++
            }
            require(ver <= 10) { "text too long for this QR encoder (max $MAX_BYTES bytes)" }
            val bits = ArrayList<Int>()
            fun put(value: Int, len: Int) { for (i in len - 1 downTo 0) bits.add((value ushr i) and 1) }
            put(0b0100, 4)
            put(bytes.size, if (ver <= 9) 8 else 16)
            for (b in bytes) put(b.toInt() and 0xFF, 8)
            val capacity = dataCodewords(ver) * 8
            put(0, minOf(4, capacity - bits.size))
            while (bits.size % 8 != 0) bits.add(0)
            var pad = 0xEC
            while (bits.size < capacity) { put(pad, 8); pad = pad xor (0xEC xor 0x11) }
            val data = ByteArray(bits.size / 8)
            for (i in bits.indices) data[i ushr 3] = (data[i ushr 3].toInt() or (bits[i] shl (7 - (i and 7)))).toByte()
            return QrCode(ver, data)
        }

        // ---- Reed-Solomon over GF(256), polynomial 0x11D
        private fun gfMul(x: Int, y: Int): Int {
            var z = 0
            for (i in 7 downTo 0) {
                z = (z shl 1) xor ((z ushr 7) * 0x11D)
                z = z xor (((y ushr i) and 1) * x)
            }
            return z
        }

        private fun rsDivisor(degree: Int): IntArray {
            val r = IntArray(degree)
            r[degree - 1] = 1
            var root = 1
            repeat(degree) {
                for (j in 0 until degree) {
                    r[j] = gfMul(r[j], root)
                    if (j + 1 < degree) r[j] = r[j] xor r[j + 1]
                }
                root = gfMul(root, 0x02)
            }
            return r
        }

        private fun rsRemainder(data: IntArray, divisor: IntArray): IntArray {
            val r = IntArray(divisor.size)
            for (b in data) {
                val factor = b xor r[0]
                for (i in 0 until r.size - 1) r[i] = r[i + 1]
                r[r.size - 1] = 0
                for (i in r.indices) r[i] = r[i] xor gfMul(divisor[i], factor)
            }
            return r
        }
    }
}
