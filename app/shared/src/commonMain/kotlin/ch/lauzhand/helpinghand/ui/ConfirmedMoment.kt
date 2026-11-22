package ch.lauzhand.helpinghand.ui

import androidx.compose.animation.core.*
import ch.lauzhand.helpinghand.model.CommunityImpact
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import helpinghand.app.shared.generated.resources.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * The "delivery confirmed" moment: concentric rings in the accent blue spread outwards, a gold stamp lands in
 * the middle (gold = reward), then the impact line ("Your socks reached Luc near Flon") and the updated community
 * totals, counting up. It closes itself after a few seconds, or on tap-away.
 */
@Composable
fun ConfirmedMoment(impactLine: String, impact: CommunityImpact, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) { delay(4500); onDismiss() }

    val stamp = remember { Animatable(0f) }
    LaunchedEffect(Unit) { stamp.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 220f)) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // Full-screen, so the confetti covers the whole screen; a tap anywhere outside the card closes it.
        Box(
            Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
        Column(
            Modifier.padding(horizontal = 32.dp).widthIn(max = 420.dp).fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                Rings(MaterialTheme.app.accent)
                GoldStamp(Modifier.size(84.dp).scale(stamp.value))
            }
            Text(
                stringResource(Res.string.moment_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            // the impact line: where it went
            Text(impactLine, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            // then the updated totals, counting up
            ImpactStrip(impact, animate = true, title = stringResource(Res.string.moment_totals))
        }
        ConfettiOverlay(trigger = 1)
        }
    }
}

/** Three rings, one after the other, spreading out and fading. */
@Composable
private fun Rings(color: androidx.compose.ui.graphics.Color) {
    val transition = rememberInfiniteTransition()
    val phases = (0 until 3).map { i ->
        transition.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(2100, easing = LinearEasing), initialStartOffset = StartOffset(i * 700)),
        )
    }
    Canvas(Modifier.fillMaxSize()) {
        val start = 42.dp.toPx()
        val end = size.minDimension / 2
        phases.forEach { p ->
            val t = p.value
            drawCircle(color.copy(alpha = 0.45f * (1f - t)), start + (end - start) * t, style = Stroke(3.dp.toPx()))
        }
    }
}

@Composable
private fun GoldStamp(modifier: Modifier) {
    val gold = MaterialTheme.app.gold
    val onGold = MaterialTheme.app.onGold
    Canvas(modifier) {
        drawCircle(gold, size.minDimension / 2, Offset(size.width / 2, size.height / 2))
        val check = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.52f)
            lineTo(size.width * 0.44f, size.height * 0.68f)
            lineTo(size.width * 0.74f, size.height * 0.34f)
        }
        drawPath(check, onGold, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
