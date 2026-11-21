package ch.lauzhand.helpinghand.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.model.NeedStatus
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// One look per status. Status is never shown by colour alone: every chip has its own icon and label.
//   open        = outline chip, empty circle
//   in progress = accent tint, half circle      (claimed / being funded)
//   delivered   = primary tint, filled circle   (waiting for confirmation)
//   confirmed   = success fill, check mark
@Composable
private fun NeedStatus.fill(): Color = when (this) {
    NeedStatus.OPEN -> Color.Transparent
    NeedStatus.CLAIMED, NeedStatus.FUNDING -> MaterialTheme.app.accent.copy(alpha = 0.18f)
    NeedStatus.FULFILLED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    NeedStatus.CONFIRMED -> MaterialTheme.app.success
}

@Composable
private fun NeedStatus.textColor(): Color =
    if (this == NeedStatus.CONFIRMED) MaterialTheme.app.onSuccess else MaterialTheme.colorScheme.onSurface

@Composable
private fun NeedStatus.iconColor(): Color = when (this) {
    NeedStatus.OPEN -> MaterialTheme.colorScheme.onSurfaceVariant
    NeedStatus.CLAIMED, NeedStatus.FUNDING -> MaterialTheme.app.accent
    NeedStatus.FULFILLED -> MaterialTheme.colorScheme.primary
    NeedStatus.CONFIRMED -> MaterialTheme.app.onSuccess
}

fun NeedStatus.label(): StringResource = when (this) {
    NeedStatus.OPEN -> Res.string.status_open
    NeedStatus.CLAIMED -> Res.string.status_claimed
    NeedStatus.FUNDING -> Res.string.status_funding
    NeedStatus.FULFILLED -> Res.string.status_fulfilled
    NeedStatus.CONFIRMED -> Res.string.status_confirmed
}

@Composable
private fun StatusIcon(status: NeedStatus, color: Color) {
    Canvas(Modifier.size(14.dp)) {
        val w = 2.dp.toPx()
        val r = size.minDimension / 2 - w / 2
        val c = Offset(size.width / 2, size.height / 2)
        when (status) {
            NeedStatus.OPEN -> drawCircle(color, r, c, style = Stroke(w))
            NeedStatus.CLAIMED, NeedStatus.FUNDING -> {
                drawCircle(color, r, c, style = Stroke(w))
                drawArc(color, 90f, 180f, true, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r))
            }
            NeedStatus.FULFILLED -> drawCircle(color, r + w / 2, c)
            NeedStatus.CONFIRMED -> {
                val check = Path().apply {
                    moveTo(size.width * 0.18f, size.height * 0.55f)
                    lineTo(size.width * 0.42f, size.height * 0.78f)
                    lineTo(size.width * 0.85f, size.height * 0.25f)
                }
                drawPath(check, color, style = Stroke(w * 1.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

/** Status pill: colour, icon and text change with a short animation when the status changes. */
@Composable
fun StatusChip(status: NeedStatus, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(status.fill())
    val fg by animateColorAsState(status.textColor())
    val icon by animateColorAsState(status.iconColor())
    AnimatedContent(status, modifier = modifier) { s ->
        Row(
            Modifier
                .background(bg, AppShape.chip)
                .then(if (s == NeedStatus.OPEN) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, AppShape.chip) else Modifier)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusIcon(s, icon)
            Text(stringResource(s.label()), style = MaterialTheme.typography.labelLarge, color = fg)
        }
    }
}
