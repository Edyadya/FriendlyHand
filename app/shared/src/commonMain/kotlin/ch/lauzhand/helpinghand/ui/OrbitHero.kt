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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import helpinghand.app.shared.generated.resources.Res
import helpinghand.app.shared.generated.resources.imc_logo
import helpinghand.app.shared.generated.resources.jetbrains_logo
import helpinghand.app.shared.generated.resources.logo
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class OrbitLogo { IMC, JETBRAINS }

/** One logo on a ring: which ring, where it starts (degrees) and which logo it is. */
private class Orbiter(val ring: Int, val startDeg: Float, val logo: OrbitLogo)

// Three rings, each with its own speed and direction (clockwise, anticlockwise, clockwise).
private val Orbiters = listOf(
    Orbiter(0, 20f, OrbitLogo.IMC), Orbiter(0, 200f, OrbitLogo.JETBRAINS),
    Orbiter(1, 0f, OrbitLogo.JETBRAINS), Orbiter(1, 120f, OrbitLogo.IMC), Orbiter(1, 240f, OrbitLogo.JETBRAINS),
    Orbiter(2, 60f, OrbitLogo.IMC), Orbiter(2, 240f, OrbitLogo.IMC),
)
private val RingRadius = floatArrayOf(0.40f, 0.68f, 0.96f) // fractions of the half-diameter
private val RingAlpha = floatArrayOf(0.40f, 0.32f, 0.25f)
private val RingDirection = floatArrayOf(1f, -1f, 1f)

/**
 * The login hero: the app logo in the middle, three thin accent rings, and the IMC and JetBrains logos slowly
 * orbiting on them. With reduce-motion on, the logos stand still.
 * Everything is drawn in one canvas, so the animation never recomposes anything.
 */
@Composable
fun OrbitHero(diameter: Dp, modifier: Modifier = Modifier, reduceMotion: Boolean = rememberReduceMotion()) {
    val accent = MaterialTheme.app.accent
    val imc: ImageBitmap = imageResource(Res.drawable.imc_logo)
    val jetbrains: ImageBitmap = imageResource(Res.drawable.jetbrains_logo)

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

            val discRadius = (half * 0.17f).coerceAtLeast(14.dp.toPx())
            val angles = floatArrayOf(inner, middle, outer)
            Orbiters.forEach { o ->
                val moved = if (reduceMotion) 0f else angles[o.ring] * RingDirection[o.ring]
                val rad = (o.startDeg + moved) * (PI.toFloat() / 180f)
                val at = Offset(c.x + RingRadius[o.ring] * half * cos(rad), c.y + RingRadius[o.ring] * half * sin(rad))
                // a white disc behind each logo keeps it readable in light and dark mode
                drawCircle(Color.White, discRadius, at)
                drawCircle(accent.copy(alpha = 0.55f), discRadius, at, style = Stroke(1.dp.toPx()))
                val image = if (o.logo == OrbitLogo.IMC) imc else jetbrains
                val side = (discRadius * (if (o.logo == OrbitLogo.IMC) 1.55f else 1.25f)).toInt()
                drawImage(
                    image,
                    srcSize = IntSize(image.width, image.height),
                    dstSize = IntSize(side, side),
                    dstOffset = IntOffset((at.x - side / 2f).toInt(), (at.y - side / 2f).toInt()),
                )
            }
        }
        Image(painterResource(Res.drawable.logo), contentDescription = null, modifier = Modifier.size(diameter * 0.26f))
    }
}
