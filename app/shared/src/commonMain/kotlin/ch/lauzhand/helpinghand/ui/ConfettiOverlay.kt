package ch.lauzhand.helpinghand.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

private class Piece(
    val angle: Float,     // launch direction, radians
    val speed: Float,     // dp per second
    val size: Float,      // dp
    val colour: Int,      // index into the palette
    val spin: Float,      // degrees per second
    val spin0: Float,
    val round: Boolean,
)

/**
 * A short confetti burst (about 1.8 s) in gold, accent blue and pale blue, with two expanding rings.
 *
 * It plays when [trigger] CHANGES to a new positive value, so bump the counter from the click that completes
 * a goal or confirms a delivery. It does not play on recomposition or when the screen is entered again
 * (the counter starts at 0 and 0 never plays). It draws only, and never takes touch input.
 * With reduce-motion on, it shows a static gold check for a moment instead. A light haptic goes with it
 * (the common Compose haptic API: it does nothing where the platform has no haptics).
 */
@Composable
fun ConfettiOverlay(trigger: Int, modifier: Modifier = Modifier, reduceMotion: Boolean = rememberReduceMotion()) {
    val app = MaterialTheme.app
    val haptics = LocalHapticFeedback.current
    var active by remember { mutableStateOf(false) }
    var seed by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        seed = trigger
        active = true
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (reduceMotion) {
            delay(1600)
        } else {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(DURATION_MS, easing = LinearEasing))
        }
        active = false
    }
    if (!active) return

    if (reduceMotion) {
        // A calm, static gold check instead of the burst.
        Box(modifier, contentAlignment = Alignment.TopCenter) {
            Canvas(Modifier.padding(top = 96.dp).size(72.dp)) {
                drawCircle(app.gold)
                val check = Path().apply {
                    moveTo(size.width * 0.28f, size.height * 0.52f)
                    lineTo(size.width * 0.44f, size.height * 0.68f)
                    lineTo(size.width * 0.74f, size.height * 0.34f)
                }
                drawPath(check, app.onGold, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        return
    }

    val pieces = remember(seed) {
        val rnd = Random(seed * 7919)
        List(PIECES) {
            Piece(
                angle = (-PI * (0.12 + 0.76 * rnd.nextDouble())).toFloat(), // an upward fan
                speed = 260f + 520f * rnd.nextFloat(),
                size = 6f + 7f * rnd.nextFloat(),
                colour = rnd.nextInt(3),
                spin = (rnd.nextFloat() - 0.5f) * 900f,
                spin0 = rnd.nextFloat() * 360f,
                round = rnd.nextFloat() < 0.25f,
            )
        }
    }
    val palette = listOf(app.gold, app.accent, app.pale)

    // No pointer handling anywhere in here: touches pass straight through to what is underneath.
    Canvas(modifier.fillMaxSize()) {
        val t = progress.value * DURATION_MS / 1000f // seconds
        val origin = Offset(size.width / 2f, size.height * 0.34f)
        val density = this.density
        val fade = if (progress.value < 0.7f) 1f else (1f - (progress.value - 0.7f) / 0.3f).coerceIn(0f, 1f)

        // two rings spreading outwards, one after the other
        for (i in 0..1) {
            val p = ((progress.value - i * 0.18f) / 0.6f).coerceIn(0f, 1f)
            if (p > 0f && p < 1f) {
                drawCircle(app.accent.copy(alpha = 0.30f * (1f - p)), size.minDimension * 0.55f * p, origin, style = Stroke(3.dp.toPx()))
            }
        }

        pieces.forEach { pc ->
            val v = pc.speed * density
            val x = origin.x + cos(pc.angle) * v * (1f - exp(-2.2f * t)) / 2.2f
            val y = origin.y + sin(pc.angle) * v * (1f - exp(-1.2f * t)) / 1.2f + 0.5f * 1500f * density * t * t
            val s = pc.size * density
            val colour = palette[pc.colour].copy(alpha = fade)
            if (pc.round) {
                drawCircle(colour, s * 0.45f, Offset(x, y))
            } else {
                rotate(pc.spin0 + pc.spin * t, pivot = Offset(x, y)) {
                    drawRect(colour, topLeft = Offset(x - s / 2, y - s * 0.28f), size = Size(s, s * 0.56f))
                }
            }
        }
    }
}

private const val DURATION_MS = 1800
private const val PIECES = 90
