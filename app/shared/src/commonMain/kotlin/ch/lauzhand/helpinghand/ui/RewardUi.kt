package ch.lauzhand.helpinghand.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

const val VOUCHER_GOAL = 5

/**
 * Navy stat panel (navy in light AND dark): a big condensed "2 / 5 GOALS" that counts up, the
 * progress sentence, and five stamp slots below it. Completed stamps are gold, empty ones are outlined.
 */
@Composable
fun GoalsHero(completed: Int, modifier: Modifier = Modifier, pending: Int = 0, goal: Int = VOUCHER_GOAL) {
    val app = MaterialTheme.app
    val shown by animateIntAsState(completed.coerceAtMost(goal), tween(900))
    Column(
        modifier.fillMaxWidth().darkOutline(AppShape.card).background(app.navy, AppShape.card).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // the count on the left, the stamps on the right: one row instead of three
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$shown / $goal", style = MaterialTheme.typography.displaySmall, color = app.onNavy)
                Text(
                    stringResource(Res.string.hero_goals),
                    Modifier.padding(bottom = 6.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = app.onNavy,
                )
            }
            StampRow(completed, goal, pending = pending, size = 30.dp, gap = 6.dp)
        }
        Text(
            stringResource(Res.string.rewards_progress, completed.coerceAtMost(goal), goal),
            style = MaterialTheme.typography.bodyMedium,
            color = app.onNavy,
        )
        if (pending > 0) {
            // delivered, but no volunteer has verified it yet: it does not count until they do
            Text(
                stringResource(Res.string.hero_pending, pending),
                style = MaterialTheme.typography.labelLarge,
                color = app.gold,
            )
        }
    }
}

/** [goal] stamp slots: completed = solid gold with a navy check, empty = outlined in the accent blue. */
@Composable
fun StampRow(completed: Int, goal: Int, modifier: Modifier = Modifier, pending: Int = 0, size: Dp = 40.dp, gap: Dp = 10.dp) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
        repeat(goal) { i -> Stamp(done = i < completed, pending = i >= completed && i < completed + pending, size = size) }
    }
}

@Composable
private fun Stamp(done: Boolean, pending: Boolean = false, size: Dp = 40.dp) {
    val gold = MaterialTheme.app.gold
    val onGold = MaterialTheme.app.onGold
    val accent = MaterialTheme.app.accent
    val fill by animateFloatAsState(if (done) 1f else 0f, tween(500))
    Canvas(Modifier.size(size)) {
        val w = 2.dp.toPx()
        val r = this.size.minDimension / 2 - w / 2
        val c = Offset(this.size.width / 2, this.size.height / 2)
        // empty slot outline stays under the gold fill, so a stamp "lands" on its slot
        if (pending) {
            // a dashed gold ring with an hourglass: earned, but waiting for a volunteer's verification
            drawCircle(gold, r, c, style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 2.2f, w * 1.6f))))
            drawHourglass(gold, w)
        } else {
            drawCircle(accent, r, c, style = Stroke(w))
        }
        if (fill > 0f) {
            drawCircle(gold, r * fill, c)
            drawCheck(onGold, w * 1.4f, alpha = fill)
        }
    }
}

private fun DrawScope.drawHourglass(color: Color, width: Float) {
    val l = size.width * 0.34f; val r = size.width * 0.66f
    val t = size.height * 0.28f; val b = size.height * 0.72f; val m = size.height * 0.5f
    val glass = Path().apply {
        moveTo(l, t); lineTo(r, t); lineTo(size.width * 0.54f, m); lineTo(r, b); lineTo(l, b); lineTo(size.width * 0.46f, m); close()
    }
    drawPath(glass, color, style = Stroke(width * 0.8f, join = StrokeJoin.Round))
}

private fun DrawScope.drawCheck(color: Color, width: Float, alpha: Float = 1f) {
    val check = Path().apply {
        moveTo(size.width * 0.28f, size.height * 0.52f)
        lineTo(size.width * 0.45f, size.height * 0.68f)
        lineTo(size.width * 0.74f, size.height * 0.34f)
    }
    drawPath(check, color.copy(alpha = alpha), style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Partner voucher: a surface card with a 2 dp gold edge on the left (gold = a reward). */
@Composable
fun VoucherCard(completed: Int, modifier: Modifier = Modifier, pending: Int = 0) {
    val colors = MaterialTheme.colorScheme
    val progress by animateFloatAsState((completed.toFloat() / VOUCHER_GOAL).coerceIn(0f, 1f), tween(700))
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min)
            .clip(AppShape.card)
            .background(colors.surface)
            .border(MaterialTheme.borderWidth, colors.outlineVariant, AppShape.card),
    ) {
        Box(Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.app.gold))
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(Res.string.voucher_title), style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            AppProgressBar(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.app.rewardFill,                // gold in dark mode
                trackColor = MaterialTheme.app.progressTrack,
            )
            Text(
                if (completed >= VOUCHER_GOAL) stringResource(Res.string.rewards_unlocked)
                else stringResource(Res.string.rewards_progress, completed.coerceAtMost(VOUCHER_GOAL), VOUCHER_GOAL),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
            )
            if (pending > 0) {
                Text(stringResource(Res.string.hero_pending, pending), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

/**
 * Badge icon. Unlocked: a solid gold medal with a navy check. Locked: a grey ring with a padlock.
 * The two differ by shape and icon as well as by colour.
 */
@Composable
fun BadgeIcon(unlocked: Boolean, modifier: Modifier = Modifier, pending: Boolean = false) {
    val gold = MaterialTheme.app.gold
    val onGold = MaterialTheme.app.onGold
    val grey = MaterialTheme.app.lockedContent
    Canvas(modifier) {
        val r = size.minDimension / 2
        val c = Offset(size.width / 2, size.height / 2)
        if (unlocked) {
            drawCircle(gold, r, c)
            drawCheck(onGold, 3.dp.toPx())
        } else if (pending) {
            drawCircle(gold, r - 1.5.dp.toPx(), c, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
            drawHourglass(gold, 2.5.dp.toPx())
        } else {
            drawCircle(grey, r - 1.5.dp.toPx(), c, style = Stroke(2.dp.toPx()))
            // padlock: body + shackle
            val w = 2.5.dp.toPx()
            val bodyW = size.width * 0.34f
            val bodyH = size.height * 0.26f
            val left = c.x - bodyW / 2
            val top = c.y - bodyH * 0.1f
            drawRoundRect(grey, Offset(left, top), Size(bodyW, bodyH), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            val shackle = Path().apply {
                moveTo(left + bodyW * 0.2f, top)
                lineTo(left + bodyW * 0.2f, top - bodyH * 0.35f)
                cubicTo(left + bodyW * 0.2f, top - bodyH * 0.95f, left + bodyW * 0.8f, top - bodyH * 0.95f, left + bodyW * 0.8f, top - bodyH * 0.35f)
                lineTo(left + bodyW * 0.8f, top)
            }
            drawPath(shackle, grey, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
