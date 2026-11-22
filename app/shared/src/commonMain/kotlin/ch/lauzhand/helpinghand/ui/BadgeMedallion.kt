package ch.lauzhand.helpinghand.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A badge medallion with its picture and a circular progress ring around it.
 *
 * Locked: a pale medallion with a muted picture, a small padlock in the corner and a blue ring that fills with the
 * progress. Unlocked: a gold medallion with a navy picture, a full gold ring and a check mark in the corner. Pending
 * (delivered, waiting for a volunteer): the locked look with a dashed gold ring and an hourglass. Locked and unlocked
 * differ by the corner badge and the ring as well as the colour. With [animate] the ring fills and the medallion pops
 * when a badge unlocks (skipped when the device asks for reduced motion).
 */
@Composable
fun BadgeMedallion(
    glyph: BadgeGlyph,
    unlocked: Boolean,
    progress: Int,
    target: Int,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    pending: Boolean = false,
    animate: Boolean = false,
) {
    val app = MaterialTheme.app
    val colors = MaterialTheme.colorScheme
    val motion = animate && !rememberReduceMotion()
    val fraction by animateFloatAsState(
        if (unlocked) 1f else (progress.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f),
        if (motion) tween(800) else snap(),
    )
    val gold by animateFloatAsState(if (unlocked) 1f else 0f, if (motion) tween(500) else snap())
    val pop = remember { Animatable(1f) }
    LaunchedEffect(unlocked) {
        if (motion && unlocked) { pop.snapTo(0.8f); pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 260f)) }
    }

    val progressFill = app.progressFill
    val track = app.progressTrack
    val ringGold = app.goldRing
    val goldA = app.gold
    val goldB = app.goldLight
    val locked = app.medalLocked
    val navy = app.onGold
    val muted = colors.onSurfaceVariant
    val cornerFill = colors.surface
    val cornerLine = colors.outline

    Box(modifier.size(size).scale(pop.value), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = this.size.minDimension
            val stroke = s / 18f                      // 4 dp on a 72 dp medallion
            val c = Offset(s / 2, s / 2)
            val ringR = (s - stroke) / 2
            val discR = s / 2 - stroke                // a 64 dp disc inside the ring
            // the ring: its empty track, then the progress
            drawCircle(track, ringR, c, style = Stroke(stroke))
            if (pending) {
                drawCircle(ringGold, ringR, c, style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 1.6f, stroke * 1.2f))))
            } else if (fraction > 0f) {
                drawArc(
                    lerp(progressFill, ringGold, gold), -90f, 360f * fraction, false,
                    Offset(c.x - ringR, c.y - ringR), Size(2 * ringR, 2 * ringR), style = Stroke(stroke, cap = if (fraction >= 1f) StrokeCap.Butt else StrokeCap.Round),
                )
            }
            // the medallion: pale, with gold fading in over it
            drawCircle(locked, discR, c)
            if (gold > 0f) {
                drawCircle(Brush.linearGradient(listOf(goldA, goldB), Offset(c.x - discR, c.y - discR), Offset(c.x + discR, c.y + discR)), discR, c, alpha = gold)
            }
            // the picture
            val side = discR * 1.62f // the pictures fill about 70% of this square
            val ink = lerp(muted.copy(alpha = 0.95f), navy, gold)
            val soft = lerp(muted.copy(alpha = 0.75f), navy.copy(alpha = 0.7f), gold)
            val cut = lerp(locked, goldA, gold)
            inset((s - side) / 2f) { drawGlyph(glyph, ink, soft, cut) }
            // the corner badge: a padlock, an hourglass or a check mark
            val b = s * 0.34f
            val bc = Offset(s - b / 2f - stroke * 0.2f, s - b / 2f - stroke * 0.2f)
            drawCircle(if (unlocked) navy else cornerFill, b / 2f, bc)
            drawCircle(if (unlocked) navy else (if (pending) ringGold else cornerLine), b / 2f, bc, style = Stroke(stroke * 0.4f))
            when {
                unlocked -> drawCheckMark(bc, b, Color.White)
                pending -> drawHourglassMark(bc, b, ringGold)
                else -> drawPadlock(bc, b, muted)
            }
        }
    }
}

private fun DrawScope.drawCheckMark(c: Offset, b: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x - b * 0.2f, c.y + b * 0.01f); lineTo(c.x - b * 0.05f, c.y + b * 0.16f); lineTo(c.x + b * 0.22f, c.y - b * 0.15f)
    }
    drawPath(p, color, style = Stroke(b * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawPadlock(c: Offset, b: Float, color: Color) {
    drawRoundRect(color, Offset(c.x - b * 0.17f, c.y - b * 0.02f), Size(b * 0.34f, b * 0.26f), CornerRadius(b * 0.05f))
    drawArc(color, 180f, 180f, false, Offset(c.x - b * 0.11f, c.y - b * 0.22f), Size(b * 0.22f, b * 0.3f), style = Stroke(b * 0.07f))
}

private fun DrawScope.drawHourglassMark(c: Offset, b: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x - b * 0.14f, c.y - b * 0.2f); lineTo(c.x + b * 0.14f, c.y - b * 0.2f); lineTo(c.x + b * 0.02f, c.y); lineTo(c.x + b * 0.14f, c.y + b * 0.2f)
        lineTo(c.x - b * 0.14f, c.y + b * 0.2f); lineTo(c.x - b * 0.02f, c.y); close()
    }
    drawPath(p, color, style = Stroke(b * 0.07f, join = StrokeJoin.Round))
}
