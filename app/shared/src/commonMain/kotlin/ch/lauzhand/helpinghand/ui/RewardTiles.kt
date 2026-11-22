package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** What one reward tile shows: the same for patron and volunteer badges. */
class RewardTile(
    val key: String,
    val title: StringResource,
    val glyph: BadgeGlyph,
    val unlocked: Boolean,
    val pending: Boolean,
    val progress: Int,
    val target: Int,
) {
    /** 0..1: how close it is to unlocking. */
    val fraction: Float get() = if (unlocked) 1f else (progress.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
}

/** A heading and the tiles under it. */
class RewardGroup(val title: StringResource, val tiles: List<RewardTile>)

/** The locked badge that is closest to unlocking (the highest fraction; the first one if none has started), or null. */
fun nextUp(tiles: List<RewardTile>): RewardTile? = pickNextUp(tiles, { it.unlocked }, { it.fraction })

/** The not yet unlocked item with the highest fraction; the first of equals, so with nothing started, the first one. */
fun <T> pickNextUp(items: List<T>, isUnlocked: (T) -> Boolean, fraction: (T) -> Float): T? =
    items.filter { !isUnlocked(it) }.maxByOrNull(fraction)

/**
 * One reward tile: a big box that fills its column, with a 96 dp medallion and its progress ring, the name (as many lines
 * as it needs, never cut off) and a count. The same design on the web (four columns) and on a phone (two columns).
 * Tapping it opens the reward.
 */
@Composable
fun RewardTileCard(t: RewardTile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val app = MaterialTheme.app
    val shape = RoundedCornerShape(20.dp)
    val title = stringResource(t.title)
    val description = when {
        t.unlocked -> stringResource(Res.string.badge_a11y_unlocked, title)
        t.pending -> stringResource(Res.string.badge_a11y_pending, title, t.progress, t.target)
        else -> stringResource(Res.string.badge_a11y_locked, title, t.progress, t.target)
    }
    Column(
        modifier.fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(MaterialTheme.borderWidth, if (t.unlocked) app.goldRing else colors.outlineVariant, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = 12.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BadgeMedallion(t.glyph, t.unlocked, t.progress, t.target, size = 96.dp, pending = t.pending)
        Text(title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, textAlign = TextAlign.Center)
        Text(
            if (t.unlocked) stringResource(Res.string.badge_unlocked) else stringResource(Res.string.badge_progress_short, t.progress, t.target),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
        )
    }
}

/** The "Next up" card: the badge that is closest to unlocking. */
@Composable
fun NextUpCard(t: RewardTile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val title = stringResource(t.title)
    val description = stringResource(Res.string.next_up) + ": " + stringResource(Res.string.badge_a11y_locked, title, t.progress, t.target)
    Row(
        modifier.fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(MaterialTheme.borderWidth, colors.outlineVariant, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BadgeMedallion(t.glyph, t.unlocked, t.progress, t.target, size = 64.dp, pending = t.pending)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(Res.string.next_up), style = MaterialTheme.typography.labelLarge, color = colors.primary)
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Text(stringResource(Res.string.badge_progress_short, t.progress, t.target), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

/**
 * Adds the badges to a grid: the "Next up" card, then each group under its small heading, in big boxes that fill their
 * column. Every group has four badges, so with four columns (web) a group is one row and with two (phone) a neat 2 x 2.
 */
fun LazyGridScope.rewardGroups(groups: List<RewardGroup>, onOpen: (RewardTile) -> Unit) {
    nextUp(groups.flatMap { it.tiles })?.let { next ->
        item(span = { GridItemSpan(maxLineSpan) }) { NextUpCard(next, { onOpen(next) }) }
    }
    for (g in groups) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                stringResource(g.title).uppercase(),
                Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(g.tiles.size, key = { g.tiles[it].key }) { i -> RewardTileCard(g.tiles[i], { onOpen(g.tiles[i]) }) }
    }
}
