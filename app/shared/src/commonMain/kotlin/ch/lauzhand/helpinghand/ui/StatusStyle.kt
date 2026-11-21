package ch.lauzhand.helpinghand.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.model.NeedStatus
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// One colour language for need statuses everywhere (all from the theme, so dark mode just works):
// open = warm, being handled = blue, delivered = teal, confirmed = solid teal.
@Composable
fun NeedStatus.containerColor(): Color = when (this) {
    NeedStatus.OPEN -> MaterialTheme.colorScheme.secondaryContainer
    NeedStatus.CLAIMED, NeedStatus.FUNDING -> MaterialTheme.colorScheme.tertiaryContainer
    NeedStatus.FULFILLED -> MaterialTheme.colorScheme.primaryContainer
    NeedStatus.CONFIRMED -> MaterialTheme.colorScheme.primary
}

@Composable
fun NeedStatus.contentColor(): Color = when (this) {
    NeedStatus.OPEN -> MaterialTheme.colorScheme.onSecondaryContainer
    NeedStatus.CLAIMED, NeedStatus.FUNDING -> MaterialTheme.colorScheme.onTertiaryContainer
    NeedStatus.FULFILLED -> MaterialTheme.colorScheme.onPrimaryContainer
    NeedStatus.CONFIRMED -> MaterialTheme.colorScheme.onPrimary
}

fun NeedStatus.label(): StringResource = when (this) {
    NeedStatus.OPEN -> Res.string.status_open
    NeedStatus.CLAIMED -> Res.string.status_claimed
    NeedStatus.FUNDING -> Res.string.status_funding
    NeedStatus.FULFILLED -> Res.string.status_fulfilled
    NeedStatus.CONFIRMED -> Res.string.status_confirmed
}

/** Status pill: the colour and the text change with a short animation when the status changes. */
@Composable
fun StatusChip(status: NeedStatus, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(status.containerColor())
    val fg by animateColorAsState(status.contentColor())
    AnimatedContent(status, modifier = modifier) { s ->
        Text(
            stringResource(s.label()),
            style = MaterialTheme.typography.titleSmall,
            color = fg,
            modifier = Modifier
                .background(bg, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}
