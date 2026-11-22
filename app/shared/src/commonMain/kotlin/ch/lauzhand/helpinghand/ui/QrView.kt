package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ch.lauzhand.helpinghand.qr.QrCode

/**
 * A QR code for [text]. Always black on white with the 4-module quiet zone around it, in light and dark mode,
 * because scanners need a light background. [description] is what screen readers say.
 */
@Composable
fun QrView(text: String, description: String, modifier: Modifier = Modifier) {
    val qr = remember(text) { QrCode.encode(text) }
    Canvas(modifier.aspectRatio(1f).background(Color.White).semantics { contentDescription = description }) {
        val cells = qr.size + 8
        val cell = size.minDimension / cells
        for (y in 0 until qr.size) for (x in 0 until qr.size) {
            if (qr.isDark(x, y)) drawRect(Color.Black, Offset((x + 4) * cell, (y + 4) * cell), Size(cell + 0.5f, cell + 0.5f))
        }
    }
}
