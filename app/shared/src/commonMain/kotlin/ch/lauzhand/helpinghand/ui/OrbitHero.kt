package ch.lauzhand.helpinghand.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import helpinghand.app.shared.generated.resources.Res
import helpinghand.app.shared.generated.resources.ic_kotlin_light
import helpinghand.app.shared.generated.resources.imc_logo
import helpinghand.app.shared.generated.resources.jetbrains_logo
import helpinghand.app.shared.generated.resources.logo
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class OrbitKind { HEART, PIN, COAT, SOCKS, COIN, BOOK, LOGO_IMC, LOGO_JETBRAINS, LOGO_KOTLIN }

/** One small icon on a ring: which ring, where it starts (degrees) and what it is. */
private class Orbiter(val ring: Int, val startDeg: Float, val kind: OrbitKind)

// Three rings, each with its own speed and direction (clockwise, anticlockwise, clockwise).
private val Orbiters = listOf(
    Orbiter(0, 30f, OrbitKind.HEART), Orbiter(0, 210f, OrbitKind.LOGO_IMC),
    Orbiter(1, 0f, OrbitKind.COAT), Orbiter(1, 180f, OrbitKind.BOOK),
    Orbiter(2, 80f, OrbitKind.LOGO_JETBRAINS), Orbiter(2, 260f, OrbitKind.LOGO_KOTLIN),
)
private val RingRadius = floatArrayOf(0.40f, 0.68f, 0.96f) // fractions of the half-diameter
private val RingAlpha = floatArrayOf(0.40f, 0.32f, 0.25f)
private val RingDirection = floatArrayOf(1f, -1f, 1f)

/**
 * The login hero: the logo in the middle, three thin accent rings, and a few small flat icons (a coat, a book, a heart)
 * together with the IMC and JetBrains logos slowly orbiting on them. With reduce-motion on, the icons stand still.
 * Everything is drawn in one canvas, so the animation never recomposes anything.
 */
@Composable
fun OrbitHero(diameter: Dp, modifier: Modifier = Modifier, reduceMotion: Boolean = rememberReduceMotion()) {
    val accent = MaterialTheme.app.accent
    val primary = MaterialTheme.colorScheme.primary
    val disc = MaterialTheme.colorScheme.surface
    val textMeasurer = rememberTextMeasurer()
    val label = MaterialTheme.typography.labelSmall
    val imc: ImageBitmap = imageResource(Res.drawable.imc_logo)
    val jetbrains: ImageBitmap = imageResource(Res.drawable.jetbrains_logo)
    // the Kotlin logo is a vector drawable, so it is drawn through a painter
    val kotlinLogo = painterResource(Res.drawable.ic_kotlin_light)

    val transition = rememberInfiniteTransition(label = "orbit")
    val inner by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(26_000, easing = LinearEasing)), label = "inner")
    val middle by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(40_000, easing = LinearEasing)), label = "middle")
    val outer by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(58_000, easing = LinearEasing)), label = "outer")

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val half = size.minDimension / 2
            val c = Offset(size.width / 2, size.height / 2)
            val stroke = Stroke(1.dp.toPx())
            for (i in 0..2) drawCircle(accent.copy(alpha = RingAlpha[i]), RingRadius[i] * half, c, style = stroke)

            val discRadius = (half * 0.15f).coerceAtLeast(13.dp.toPx())
            val angles = floatArrayOf(inner, middle, outer)
            Orbiters.forEach { o ->
                val moved = if (reduceMotion) 0f else angles[o.ring] * RingDirection[o.ring]
                val rad = (o.startDeg + moved) * (PI.toFloat() / 180f)
                val at = Offset(c.x + RingRadius[o.ring] * half * cos(rad), c.y + RingRadius[o.ring] * half * sin(rad))
                val logo = when (o.kind) { OrbitKind.LOGO_IMC -> imc; OrbitKind.LOGO_JETBRAINS -> jetbrains; else -> null }
                val isLogo = logo != null || o.kind == OrbitKind.LOGO_KOTLIN
                // logos sit on a white disc so they stay readable in dark mode too
                drawCircle(if (isLogo) Color.White else disc, discRadius, at)
                drawCircle(accent.copy(alpha = 0.55f), discRadius, at, style = Stroke(1.dp.toPx()))
                if (logo != null) {
                    val side = (discRadius * (if (o.kind == OrbitKind.LOGO_IMC) 1.55f else 1.25f)).toInt()
                    drawImage(
                        logo,
                        srcSize = IntSize(logo.width, logo.height),
                        dstSize = IntSize(side, side),
                        dstOffset = IntOffset((at.x - side / 2f).toInt(), (at.y - side / 2f).toInt()),
                    )
                } else if (o.kind == OrbitKind.LOGO_KOTLIN) {
                    val side = discRadius * 1.25f
                    translate(at.x - side / 2f, at.y - side / 2f) { with(kotlinLogo) { draw(androidx.compose.ui.geometry.Size(side, side)) } }
                } else {
                    glyph(o.kind, at, discRadius * 0.62f, if (o.ring == 1) accent else primary, disc, textMeasurer, label)
                }
            }
        }
        Image(painterResource(Res.drawable.logo), contentDescription = null, modifier = Modifier.size(diameter * 0.26f))
    }
}

/** A tiny flat icon of half-size [r] around [c]. Unit coordinates run from -1 to 1. */
private fun DrawScope.glyph(
    kind: OrbitKind, c: Offset, r: Float, color: Color, cutout: Color, text: TextMeasurer, style: TextStyle,
) {
    fun p(x: Float, y: Float) = Offset(c.x + x * r, c.y + y * r)
    fun poly(vararg pts: Pair<Float, Float>) = Path().apply {
        pts.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(p(x, y).x, p(x, y).y) else lineTo(p(x, y).x, p(x, y).y) }
        close()
    }
    when (kind) {
        OrbitKind.HEART -> {
            val heart = Path().apply {
                moveTo(p(0f, 0.9f).x, p(0f, 0.9f).y)
                cubicTo(p(-0.9f, 0.25f).x, p(-0.9f, 0.25f).y, p(-1f, -0.5f).x, p(-1f, -0.5f).y, p(-0.5f, -0.75f).x, p(-0.5f, -0.75f).y)
                cubicTo(p(-0.2f, -0.9f).x, p(-0.2f, -0.9f).y, p(-0.05f, -0.6f).x, p(-0.05f, -0.6f).y, p(0f, -0.45f).x, p(0f, -0.45f).y)
                cubicTo(p(0.05f, -0.6f).x, p(0.05f, -0.6f).y, p(0.2f, -0.9f).x, p(0.2f, -0.9f).y, p(0.5f, -0.75f).x, p(0.5f, -0.75f).y)
                cubicTo(p(1f, -0.5f).x, p(1f, -0.5f).y, p(0.9f, 0.25f).x, p(0.9f, 0.25f).y, p(0f, 0.9f).x, p(0f, 0.9f).y)
                close()
            }
            drawPath(heart, color)
        }
        OrbitKind.PIN -> {
            drawPath(poly(-0.55f to -0.1f, 0.55f to -0.1f, 0f to 1f), color)
            drawCircle(color, r * 0.62f, p(0f, -0.35f))
            drawCircle(cutout, r * 0.24f, p(0f, -0.35f))
        }
        OrbitKind.COAT -> {
            drawPath(poly(-0.45f to -0.85f, 0.45f to -0.85f, 0.95f to 0.2f, 0.6f to 0.32f, 0.5f to 0.0f, 0.5f to 0.9f, -0.5f to 0.9f, -0.5f to 0.0f, -0.6f to 0.32f, -0.95f to 0.2f), color)
            drawLine(cutout, p(0f, -0.85f), p(0f, 0.9f), 1.2.dp.toPx())
        }
        OrbitKind.SOCKS -> {
            drawPath(poly(-0.35f to -0.9f, 0.3f to -0.9f, 0.3f to 0.15f, 0.9f to 0.45f, 0.9f to 0.9f, -0.35f to 0.9f), color)
            drawLine(cutout, p(-0.35f, -0.5f), p(0.3f, -0.5f), 1.2.dp.toPx())
        }
        OrbitKind.COIN -> {
            drawCircle(color, r, c)
            drawLabel(text, "CHF", c, cutout, style.copy(fontSize = (r * 0.62f).toSp(), fontWeight = FontWeight.Bold))
        }
        OrbitKind.BOOK -> {
            // an open book: two pages with a gap at the spine
            drawPath(poly(-0.95f to -0.6f, -0.06f to -0.4f, -0.06f to 0.85f, -0.95f to 0.65f), color)
            drawPath(poly(0.95f to -0.6f, 0.06f to -0.4f, 0.06f to 0.85f, 0.95f to 0.65f), color)
            drawLine(cutout, p(-0.7f, -0.1f), p(-0.3f, 0.0f), 1.2.dp.toPx())
            drawLine(cutout, p(0.7f, -0.1f), p(0.3f, 0.0f), 1.2.dp.toPx())
        }
        OrbitKind.LOGO_IMC, OrbitKind.LOGO_JETBRAINS, OrbitKind.LOGO_KOTLIN -> Unit // drawn as images by the caller
    }
}

private fun DrawScope.drawLabel(measurer: TextMeasurer, text: String, c: Offset, color: Color, style: TextStyle) {
    val layout = measurer.measure(text, style.copy(color = color))
    drawText(layout, topLeft = Offset(c.x - layout.size.width / 2f, c.y - layout.size.height / 2f))
}
