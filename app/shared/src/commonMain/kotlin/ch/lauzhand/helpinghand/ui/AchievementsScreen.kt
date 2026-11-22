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
    AchievementId.GIVEAWAY_SUPPORTER -> Res.string.badge_giveaway
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
    AchievementId.GIVEAWAY_SUPPORTER -> Res.string.badge_giveaway_desc
    AchievementId.THREE_AREAS -> Res.string.badge_three_areas_desc
    AchievementId.THANKS_3 -> Res.string.badge_thanks_3_desc
}

internal fun AchievementId.glyph(): BadgeGlyph = when (this) {
    AchievementId.FIRST_HELP -> BadgeGlyph.HAND
    AchievementId.HELPED_3 -> BadgeGlyph.PEOPLE
    AchievementId.HELPED_10 -> BadgeGlyph.CROWD
    AchievementId.HAND_TO_HAND -> BadgeGlyph.HAND_ITEM
    AchievementId.GOAL_REACHED -> BadgeGlyph.FLAG
    AchievementId.GAVE_50 -> BadgeGlyph.COINS_1
    AchievementId.GAVE_100 -> BadgeGlyph.COINS_2
    AchievementId.GAVE_250 -> BadgeGlyph.COINS_3
    AchievementId.GAVE_500 -> BadgeGlyph.COINS_4
    AchievementId.GIVEAWAY_SUPPORTER -> BadgeGlyph.PARCEL
    AchievementId.THREE_AREAS -> BadgeGlyph.PIN
    AchievementId.THANKS_3 -> BadgeGlyph.SPEECH_HEART
}

/** Donor tab: the goals panel, the partner voucher and the badges (rules in core: donorAchievements). */
@Composable
fun AchievementsScreen(repo: HelpRepository) {
    val giveawayChf = repo.giveawayContributionsOf(ME_DONOR).sumOf { it.amountChf }
    val achievements = donorAchievements(repo.contributionsOf(ME_DONOR), repo.needs, repo.people, giveawayChf)
    fun tile(id: AchievementId): RewardTile {
        val a = achievements.first { it.id == id }
        return RewardTile(id.name, id.title(), id.glyph(), a.unlocked, a.pending, a.progress, a.target)
    }
    // three groups, each under a small heading
    val groups = listOf(
        RewardGroup(Res.string.group_helping, listOf(AchievementId.FIRST_HELP, AchievementId.HELPED_3, AchievementId.HELPED_10, AchievementId.HAND_TO_HAND).map(::tile)),
        RewardGroup(Res.string.group_giving, listOf(AchievementId.GAVE_50, AchievementId.GAVE_100, AchievementId.GAVE_250, AchievementId.GAVE_500).map(::tile)),
        RewardGroup(Res.string.group_community, listOf(AchievementId.GOAL_REACHED, AchievementId.THREE_AREAS, AchievementId.THANKS_3, AchievementId.GIVEAWAY_SUPPORTER).map(::tile)),
    )
    // the badge whose reward is being shown, or null
    var opened by remember { mutableStateOf<Achievement?>(null) }
    val completed = repo.completedGoalsFor(ME_DONOR)
    // delivered but not verified yet by a volunteer: shown as pending, not counted
    val pending = pendingGoals(repo.contributionsOf(ME_DONOR), repo.needs)
    val peopleHelped = peopleHelpedOverall(repo.needs)

    // centred, and no wider than 960 dp on big screens
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(104.dp), // about three columns on a phone, more on a wide screen
        modifier = Modifier.widthIn(max = 960.dp).fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
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
                countLabel = if (contributions.size == 1) stringResource(Res.string.art_count_contributions_one) else stringResource(Res.string.art_count_contributions, contributions.size),
                goldDone = completed, caption = Res.string.art_caption_patron,
            )
        }
        // how many people the whole platform has helped (everyone's verified help, not just mine)
        item(span = { GridItemSpan(maxLineSpan) }) { PlatformHelped(peopleHelped) }
        item(span = { GridItemSpan(maxLineSpan) }) { GoalsHero(completed, pending = pending) }
        item(span = { GridItemSpan(maxLineSpan) }) { VoucherCard(completed, pending = pending) }
        rewardGroups(groups) { t -> opened = achievements.first { it.id.name == t.key } }
    }
    }

    opened?.let { a ->
        RewardInfoDialog(
            repo = repo, title = a.id.title(), description = a.id.description(), reward = a.id.reward, glyph = a.id.glyph(),
            unlocked = a.unlocked, pending = a.pending, progress = a.progress, target = a.target,
            qrKey = "p:${a.id.name}",
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
