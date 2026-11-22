package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** What one reward block shows: the same for patron and volunteer badges. */
class RewardTile(
    val key: String,
    val title: StringResource,
    val unlocked: Boolean,
    val pending: Boolean,
    val progress: Int,
    val target: Int,
)

/**
 * One compact reward block, four to a row. Unlocked: the navy card with a gold medal. Locked: the grey "locked" card
 * with a padlock and its progress. Pending: the locked look with a dashed gold hourglass. Locked and unlocked also differ
 * by icon and label, not only by colour. Tapping it opens the reward.
 */
@Composable
fun RewardTileCard(t: RewardTile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val app = MaterialTheme.app
    // locked: the grey fill in light mode; in dark mode a plain card with a border, so the empty bar stays visible
    val container = if (t.unlocked) app.navy else if (MaterialTheme.isDark) colors.surface else app.locked
    val text = if (t.unlocked) app.onNavy else colors.onSurface
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.fillMaxWidth().heightIn(min = 118.dp)
            .darkOutline(shape)
            .clip(shape)
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        BadgeIcon(t.unlocked, Modifier.size(34.dp), pending = t.pending)
        Text(
            stringResource(t.title),
            style = MaterialTheme.typography.labelMedium,
            color = text,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        if (t.unlocked) {
            Text(stringResource(Res.string.badge_unlocked), style = MaterialTheme.typography.labelSmall, color = text, maxLines = 1)
        } else {
            AppProgressBar(
                progress = { t.progress.toFloat() / t.target.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = app.rewardFill,
                trackColor = if (MaterialTheme.isDark) app.progressTrack else colors.surface,
            )
            Text(
                stringResource(if (t.pending) Res.string.badge_pending_short else Res.string.badge_progress_short, t.progress, t.target),
                style = MaterialTheme.typography.labelSmall, color = text, maxLines = 1,
            )
        }
    }
}
