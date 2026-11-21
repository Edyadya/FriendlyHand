package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.data.ME_DONOR
import ch.lauzhand.helpinghand.model.Achievement
import ch.lauzhand.helpinghand.model.AchievementId
import ch.lauzhand.helpinghand.model.donorAchievements
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private fun AchievementId.title(): StringResource = when (this) {
    AchievementId.FIRST_HELP -> Res.string.badge_first_help
    AchievementId.HELPED_3 -> Res.string.badge_helped_3
    AchievementId.HELPED_10 -> Res.string.badge_helped_10
    AchievementId.HAND_TO_HAND -> Res.string.badge_hand_to_hand
    AchievementId.GOAL_REACHED -> Res.string.badge_goal_reached
    AchievementId.GAVE_100 -> Res.string.badge_gave_100
    AchievementId.THREE_AREAS -> Res.string.badge_three_areas
    AchievementId.THANKS_3 -> Res.string.badge_thanks_3
}

private fun AchievementId.description(): StringResource = when (this) {
    AchievementId.FIRST_HELP -> Res.string.badge_first_help_desc
    AchievementId.HELPED_3 -> Res.string.badge_helped_3_desc
    AchievementId.HELPED_10 -> Res.string.badge_helped_10_desc
    AchievementId.HAND_TO_HAND -> Res.string.badge_hand_to_hand_desc
    AchievementId.GOAL_REACHED -> Res.string.badge_goal_reached_desc
    AchievementId.GAVE_100 -> Res.string.badge_gave_100_desc
    AchievementId.THREE_AREAS -> Res.string.badge_three_areas_desc
    AchievementId.THANKS_3 -> Res.string.badge_thanks_3_desc
}

/** Donor tab: the goals panel, the partner voucher and the badges (rules in core: donorAchievements). */
@Composable
fun AchievementsScreen(repo: HelpRepository) {
    val achievements = donorAchievements(repo.contributionsOf(ME_DONOR), repo.needs, repo.people)
        .sortedByDescending { it.unlocked } // unlocked first, otherwise in the rules' order
    // the badge whose reward is being shown, or null
    var opened by remember { mutableStateOf<Achievement?>(null) }
    val completed = repo.completedGoalsFor(ME_DONOR)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 170.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(stringResource(Res.string.achievements_title), style = MaterialTheme.typography.headlineSmall)
        }
        item(span = { GridItemSpan(maxLineSpan) }) { GoalsHero(completed) }
        item(span = { GridItemSpan(maxLineSpan) }) { VoucherCard(completed) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(stringResource(Res.string.badges_title), style = MaterialTheme.typography.headlineSmall)
        }
        items(achievements, key = { it.id.name }) { a -> BadgeCard(a) { opened = a } }
    }

    opened?.let { RewardDialog(it) { opened = null } }
}

/**
 * Unlocked: navy card, gold medal, white label. Locked: grey "locked" fill, padlock, and an
 * "unlock by ..." hint with progress. Locked and unlocked also differ by icon and label, not only colour.
 */
@Composable
private fun BadgeCard(a: Achievement, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val app = MaterialTheme.app
    val container = if (a.unlocked) app.navy else app.locked
    val text = if (a.unlocked) app.onNavy else colors.onSurface
    val sub = if (a.unlocked) app.onNavy else colors.onSurface
    Column(
        Modifier.fillMaxWidth().heightIn(min = 170.dp)
            .clip(AppShape.card)
            .background(container)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BadgeIcon(a.unlocked, Modifier.size(40.dp))
        Text(stringResource(a.id.title()), style = MaterialTheme.typography.titleMedium, color = text)
        Text(
            if (a.unlocked) stringResource(a.id.description())
            else stringResource(Res.string.badge_unlock_by, stringResource(a.id.description())),
            style = MaterialTheme.typography.bodySmall,
            color = sub,
        )
        Text(
            stringResource(Res.string.reward_on_badge, a.id.reward.partner.displayName),
            style = MaterialTheme.typography.labelMedium,
            // gold text is allowed here: it names a reward, and it sits on navy
            color = if (a.unlocked) app.gold else colors.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f, fill = false))
        if (a.unlocked) {
            Text(stringResource(Res.string.badge_unlocked), style = MaterialTheme.typography.labelLarge, color = text)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { a.progress.toFloat() / a.target },
                    modifier = Modifier.weight(1f).height(6.dp),
                    color = app.accent,
                    trackColor = colors.surface,
                )
                Text(
                    stringResource(Res.string.badge_progress, a.progress, a.target),
                    style = MaterialTheme.typography.labelMedium.merge(NumeralStyle),
                    color = text,
                )
            }
            Text(stringResource(Res.string.badge_locked), style = MaterialTheme.typography.labelMedium, color = text)
        }
    }
}

/** What tapping a badge shows: its reward and the partner who gives it. */
@Composable
private fun RewardDialog(a: Achievement, onDismiss: () -> Unit) {
    val r = a.id.reward
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = colors.surface,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.reward_close)) } },
        icon = { BadgeIcon(a.unlocked, Modifier.size(48.dp)) },
        title = { Text(stringResource(a.id.title())) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(a.id.description()), style = MaterialTheme.typography.bodyLarge)
                // the reward itself: surface card with the gold edge (gold = reward)
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                        .background(colors.surface, AppShape.card)
                        .border(1.dp, colors.outlineVariant, AppShape.card),
                ) {
                    Box(Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.app.gold))
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PartnerMonogram(r.partner, Modifier.size(44.dp))
                        Column {
                            Text(stringResource(Res.string.reward_label), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                            Text(stringResource(r.reward), style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                            Text(stringResource(Res.string.reward_from, r.partner.displayName), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                        }
                    }
                }
                Text(
                    if (a.unlocked) stringResource(Res.string.reward_unlocked, r.partner.displayName)
                    else stringResource(Res.string.reward_locked, a.progress, a.target),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (a.unlocked) colors.primary else colors.onSurfaceVariant,
                )
            }
        },
    )
}

/** A round badge with the partner's initials (no logos are used). */
@Composable
private fun PartnerMonogram(p: Partner, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.clip(CircleShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
        Text(p.monogram, color = colors.onPrimaryContainer, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}
