package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

/** Donor tab: the partner voucher progress and the badges (rules in core: donorAchievements). */
@Composable
fun AchievementsScreen(repo: HelpRepository) {
    val achievements = donorAchievements(repo.contributionsOf(ME_DONOR), repo.needs, repo.people)
        .sortedByDescending { it.unlocked } // unlocked first, otherwise in the rules' order

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
        item(span = { GridItemSpan(maxLineSpan) }) {
            VoucherCard(repo.completedGoalsFor(ME_DONOR))
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(stringResource(Res.string.badges_title), style = MaterialTheme.typography.titleLarge)
        }
        items(achievements, key = { it.id.name }) { BadgeCard(it) }
    }
}

@Composable
private fun BadgeCard(a: Achievement) {
    val colors = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth().heightIn(min = 150.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (a.unlocked) colors.secondaryContainer else colors.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Medal(a.unlocked, Modifier.size(36.dp))
            Text(
                stringResource(a.id.title()),
                style = MaterialTheme.typography.titleMedium,
                color = if (a.unlocked) colors.onSecondaryContainer else colors.onSurface,
            )
            Text(
                stringResource(a.id.description()),
                style = MaterialTheme.typography.bodyMedium,
                color = if (a.unlocked) colors.onSecondaryContainer else colors.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f, fill = false))
            if (a.unlocked) {
                Text(
                    stringResource(Res.string.badge_unlocked),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSecondaryContainer,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { a.progress.toFloat() / a.target },
                        modifier = Modifier.weight(1f).height(6.dp),
                    )
                    Text(
                        stringResource(Res.string.badge_progress, a.progress, a.target),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** A round medal with a tick; grey outline while locked. Drawn, so no icon library is needed. */
@Composable
private fun Medal(unlocked: Boolean, modifier: Modifier) {
    val fill = if (unlocked) MaterialTheme.colorScheme.secondary else Color.Transparent
    val line = if (unlocked) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.outline
    Canvas(modifier) {
        val r = size.minDimension / 2
        val c = Offset(size.width / 2, size.height / 2)
        if (unlocked) drawCircle(fill, r, c) else drawCircle(line, r - 1.5.dp.toPx(), c, style = Stroke(2.dp.toPx()))
        val w = 3.dp.toPx()
        drawLine(line, Offset(c.x - r * 0.42f, c.y + r * 0.02f), Offset(c.x - r * 0.1f, c.y + r * 0.34f), w, StrokeCap.Round)
        drawLine(line, Offset(c.x - r * 0.1f, c.y + r * 0.34f), Offset(c.x + r * 0.45f, c.y - r * 0.3f), w, StrokeCap.Round)
    }
}
