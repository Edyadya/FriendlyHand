package ch.lauzhand.helpinghand.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import ch.lauzhand.helpinghand.model.Person

/** A round, white-ringed copy of [src] for use as a map marker: the centre square is cropped, then clipped to a circle. */
fun circleAvatar(src: ImageBitmap, size: Int = 112, ring: Color = Color.White): ImageBitmap {
    val out = ImageBitmap(size, size)
    val canvas = Canvas(out)
    val half = size / 2f
    val border = size * 0.07f
    canvas.drawCircle(Offset(half, half), half, Paint().apply { isAntiAlias = true; color = ring })
    canvas.save()
    canvas.clipPath(Path().apply { addOval(Rect(border, border, size - border, size - border)) })
    val side = minOf(src.width, src.height)
    canvas.drawImageRect(
        image = src,
        srcOffset = IntOffset((src.width - side) / 2, (src.height - side) / 2),
        srcSize = IntSize(side, side),
        dstOffset = IntOffset(border.toInt(), border.toInt()),
        dstSize = IntSize(size - 2 * border.toInt(), size - 2 * border.toInt()),
        paint = Paint().apply { isAntiAlias = true; filterQuality = FilterQuality.High },
    )
    canvas.restore()
    return out
}

/** The person's photo as a round map marker, only when they gave their own consent for it (otherwise null). */
@Composable
fun rememberMapAvatar(person: Person): ImageBitmap? {
    val photo = rememberPersonPhoto(person)
    return remember(photo) { photo?.let { circleAvatar(it) } }
}
