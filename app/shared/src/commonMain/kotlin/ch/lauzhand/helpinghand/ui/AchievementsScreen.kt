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
import ch.lauzhand.helpinghand.model.pendingGoals
import ch.lauzhand.helpinghand.model.peopleHelpedOverall
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal fun AchievementId.title(): StringResource = when (this) {
    AchievementId.FIRST_HELP -> Res.string.badge_first_help
    AchievementId.HELPED_3 -> Res.string.badge_helped_3
    AchievementId.HELPED_10 -> Res.string.badge_helped_10
    AchievementId.HAND_TO_HAND -> Res.string.badge_hand_to_hand
    AchievementId.GOAL_REACHED -> Res.string.badge_goal_reached
    AchievementId.GAVE_100 -> Res.string.badge_gave_100
    AchievementId.GAVE_50 -> Res.string.badge_gave_50
    AchievementId.GAVE_250 -> Res.string.badge_gave_250
    AchievementId.GAVE_500 -> Res.string.badge_gave_500
    AchievementId.GAVE_1000 -> Res.string.badge_gave_1000
    AchievementId.THREE_AREAS -> Res.string.badge_three_areas
    AchievementId.THANKS_3 -> Res.string.badge_thanks_3
}

internal fun AchievementId.description(): StringResource = when (this) {
    AchievementId.FIRST_HELP -> Res.string.badge_first_help_desc
    AchievementId.HELPED_3 -> Res.string.badge_helped_3_desc
    AchievementId.HELPED_10 -> Res.string.badge_helped_10_desc
    AchievementId.HAND_TO_HAND -> Res.string.badge_hand_to_hand_desc
    AchievementId.GOAL_REACHED -> Res.string.badge_goal_reached_desc
    AchievementId.GAVE_100 -> Res.string.badge_gave_100_desc
    AchievementId.GAVE_50 -> Res.string.badge_gave_50_desc
    AchievementId.GAVE_250 -> Res.string.badge_gave_250_desc
    AchievementId.GAVE_500 -> Res.string.badge_gave_500_desc
    AchievementId.GAVE_1000 -> Res.string.badge_gave_1000_desc
    AchievementId.THREE_AREAS -> Res.string.badge_three_areas_desc
    AchievementId.THANKS_3 -> Res.string.badge_thanks_3_desc
}

/** Donor tab: the goals panel, the partner voucher and the badges (rules in core: donorAchievements). */
@Composable
fun AchievementsScreen(repo: HelpRepository) {
    val giveawayChf = repo.giveawayContributionsOf(ME_DONOR).sumOf { it.amountChf }
    val achievements = donorAchievements(repo.contributionsOf(ME_DONOR), repo.needs, repo.people, giveawayChf)
        .sortedByDescending { it.unlocked } // unlocked first, otherwise in the rules' order
    // the badge whose reward is being shown, or null
    var opened by remember { mutableStateOf<Achievement?>(null) }
    val completed = repo.completedGoalsFor(ME_DONOR)
    // delivered but not verified yet by a volunteer: shown as pending, not counted
    val pending = pendingGoals(repo.contributionsOf(ME_DONOR), repo.needs)
    val peopleHelped = peopleHelpedOverall(repo.needs)

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
        // your personal badge: a pattern that is only yours, gold for the goals you completed
        item(span = { GridItemSpan(maxLineSpan) }) {
            val contributions = repo.contributionsOf(ME_DONOR)
            PersonalBadgeCard(
                name = repo.currentAccount?.name.orEmpty(), count = contributions.size,
                countLabel = stringResource(Res.string.art_count_contributions, contributions.size),
                goldDone = completed, caption = Res.string.art_caption_patron,
            )
        }
        // how many people the whole platform has helped (everyone's verified help, not just mine)
        item(span = { GridItemSpan(maxLineSpan) }) { PlatformHelped(peopleHelped) }
        item(span = { GridItemSpan(maxLineSpan) }) { GoalsHero(completed, pending = pending) }
        item(span = { GridItemSpan(maxLineSpan) }) { VoucherCard(completed, pending = pending) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(stringResource(Res.string.badges_title), style = MaterialTheme.typography.headlineSmall)
        }
        items(achievements, key = { it.id.name }) { a -> BadgeCard(a) { opened = a } }
    }

    opened?.let { a ->
        RewardInfoDialog(
            repo = repo, title = a.id.title(), description = a.id.description(), reward = a.id.reward,
            unlocked = a.unlocked, pending = a.pending, progress = a.progress, target = a.target,
            qrKey = "p:${a.id.name}",
            icon = { BadgeIcon(a.unlocked, Modifier.size(48.dp), pending = a.pending) },
        ) { opened = null }
    }
}

/**
 * Unlocked: navy card, gold medal, white label. Locked: grey "locked" fill, padlock, and an
 * "unlock by ..." hint with progress. Locked and unlocked also differ by icon and label, not only colour.
 */
@Composable
private fun BadgeCard(a: Achievement, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val app = MaterialTheme.app
    // locked: the grey fill in light mode; in dark mode a plain card with a border, so the empty bar stays visible
    val container = if (a.unlocked) app.navy else if (MaterialTheme.isDark) colors.surface else app.locked // pending uses the locked look, with a gold dashed icon
    val text = if (a.unlocked) app.onNavy else colors.onSurface
    val sub = if (a.unlocked) app.onNavy else colors.onSurface
    Column(
        Modifier.fillMaxWidth().heightIn(min = 170.dp)
            .darkOutline(AppShape.card)
            .clip(AppShape.card)
            .background(container)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BadgeIcon(a.unlocked, Modifier.size(40.dp), pending = a.pending)
        Text(stringResource(a.id.title()), style = MaterialTheme.typography.titleMedium, color = text)
        Text(
            when {
                a.unlocked -> stringResource(a.id.description())
                a.pending -> stringResource(Res.string.badge_pending_hint)
                else -> stringResource(Res.string.badge_unlock_by, stringResource(a.id.description()))
            },
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
                AppProgressBar(
                    progress = { a.progress.toFloat() / a.target },
                    modifier = Modifier.weight(1f).height(6.dp),
                    color = app.rewardFill,                                   // gold in dark mode
                    trackColor = if (MaterialTheme.isDark) app.progressTrack else colors.surface,
                )
                Text(
                    stringResource(Res.string.badge_progress, a.progress, a.target),
                    style = MaterialTheme.typography.labelMedium.merge(NumeralStyle),
                    color = text,
                )
            }
            Text(
                stringResource(if (a.pending) Res.string.badge_pending else Res.string.badge_locked),
                style = MaterialTheme.typography.labelMedium,
                color = text,
            )
        }
    }
}

/** The whole platform's number: how many different people have had help verified by a volunteer. */
@Composable
private fun PlatformHelped(count: Int) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().darkOutline(AppShape.card).background(colors.primaryContainer, AppShape.card).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(count.toString(), style = MaterialTheme.typography.displaySmall, color = colors.onPrimaryContainer)
        Column(Modifier.weight(1f)) {
            Text(
                if (count == 1) stringResource(Res.string.platform_helped_one) else stringResource(Res.string.platform_helped_many, count),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onPrimaryContainer,
            )
            Text(stringResource(Res.string.platform_helped_caption), style = MaterialTheme.typography.bodySmall, color = colors.onPrimaryContainer)
        }
    }
}
