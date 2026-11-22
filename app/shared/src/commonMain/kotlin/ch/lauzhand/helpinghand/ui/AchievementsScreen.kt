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
        columns = GridCells.Fixed(4), // four reward blocks in a line
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(stringResource(Res.string.achievements_title), style = MaterialTheme.typography.headlineSmall)
        }
        // your personal badge: a pattern that is only yours, gold for the goals you completed
        item(span = { GridItemSpan(maxLineSpan) }) {
            val contributions = repo.contributionsOf(ME_DONOR)
            PersonalBadgeCard(
                name = repo.currentAccount?.name.orEmpty(), count = contributions.size,
                countLabel = if (contributions.size == 1) stringResource(Res.string.art_count_contributions_one) else stringResource(Res.string.art_count_contributions, contributions.size),
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
        items(achievements, key = { it.id.name }) { a ->
            RewardTileCard(RewardTile(a.id.name, a.id.title(), a.unlocked, a.pending, a.progress, a.target), { opened = a })
        }
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

/** The whole platform's number: how many different people have had help verified by a volunteer. */
@Composable
internal fun PlatformHelped(count: Int) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().darkOutline(AppShape.card).background(colors.primaryContainer, AppShape.card).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(count.toString(), style = MaterialTheme.typography.headlineLarge, color = colors.onPrimaryContainer)
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
