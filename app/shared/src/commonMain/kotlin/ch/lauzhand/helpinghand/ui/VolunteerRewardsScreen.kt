package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.model.Account
import ch.lauzhand.helpinghand.model.VolunteerBadge
import ch.lauzhand.helpinghand.model.VolunteerProgress
import ch.lauzhand.helpinghand.model.deliveriesDone
import ch.lauzhand.helpinghand.model.deliveriesInProgress
import ch.lauzhand.helpinghand.model.volunteerBadges
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal fun VolunteerBadge.title(): StringResource = when (this) {
    VolunteerBadge.FIRST_DELIVERY -> Res.string.vr_badge_first
    VolunteerBadge.FIVE_DELIVERIES -> Res.string.vr_badge_five
    VolunteerBadge.TEN_DELIVERIES -> Res.string.vr_badge_ten
    VolunteerBadge.THREE_AREAS -> Res.string.vr_badge_areas
    VolunteerBadge.FIVE_PEOPLE -> Res.string.vr_badge_people
    VolunteerBadge.FIRST_PURCHASE -> Res.string.vr_badge_purchase
}

internal fun VolunteerBadge.description(): StringResource = when (this) {
    VolunteerBadge.FIRST_DELIVERY -> Res.string.vr_badge_first_desc
    VolunteerBadge.FIVE_DELIVERIES -> Res.string.vr_badge_five_desc
    VolunteerBadge.TEN_DELIVERIES -> Res.string.vr_badge_ten_desc
    VolunteerBadge.THREE_AREAS -> Res.string.vr_badge_areas_desc
    VolunteerBadge.FIVE_PEOPLE -> Res.string.vr_badge_people_desc
    VolunteerBadge.FIRST_PURCHASE -> Res.string.vr_badge_purchase_desc
}

/**
 * Volunteers' Rewards tab, a game like the patrons': a personal badge pattern, how many deliveries they completed, and
 * badges with partner rewards (a free coffee, a gift card, ...). Tap a badge to see its reward and, once it is
 * unlocked, its QR code. Gold marks an unlocked badge, as everywhere else in the app.
 */
@Composable
fun VolunteerRewardsScreen(repo: HelpRepository, account: Account) {
    val done = deliveriesDone(repo.orders, account.id)
    val onTheWay = deliveriesInProgress(repo.orders, account.id)
    val badges = volunteerBadges(repo.orders, account.id)
    var opened by remember { mutableStateOf<VolunteerProgress?>(null) }

    opened?.let { b ->
        RewardInfoDialog(
            repo = repo, title = b.badge.title(), description = b.badge.description(), reward = b.badge.reward,
            unlocked = b.unlocked, pending = false, progress = b.progress, target = b.badge.target,
            qrKey = "v:${b.badge.name}",
            icon = { BadgeIcon(b.unlocked, Modifier.size(48.dp)) },
        ) { opened = null }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(stringResource(Res.string.achievements_title), style = MaterialTheme.typography.headlineSmall) }
        item {
            PersonalBadgeCard(
                name = account.name, count = done, countLabel = stringResource(Res.string.art_count_deliveries, done),
                goldDone = badges.count { it.unlocked }, caption = Res.string.art_caption_volunteer,
            )
        }
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(Res.string.vr_your_deliveries), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(Res.string.vr_delivered_count, done), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                if (onTheWay > 0) Text(stringResource(Res.string.vr_in_progress, onTheWay), style = MaterialTheme.typography.bodyLarge)
            }
        }
        items(badges, key = { it.badge.name }) { b ->
            val gold = MaterialTheme.app.gold
            Card(
                Modifier.fillMaxWidth().clickable { opened = b },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(if (b.unlocked) 2.dp else MaterialTheme.borderWidth, if (b.unlocked) gold else MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    BadgeIcon(b.unlocked, Modifier.size(40.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(b.badge.title()), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            if (b.unlocked) Text(stringResource(Res.string.vr_unlocked), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(stringResource(b.badge.description()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(Res.string.reward_on_badge, b.badge.reward.partner.displayName), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        AppProgressBar(
                            progress = { b.progress.toFloat() / b.badge.target },
                            modifier = Modifier.fillMaxWidth().height(10.dp),
                            color = if (b.unlocked) gold else MaterialTheme.app.rewardFill,
                            trackColor = MaterialTheme.app.progressTrack,
                        )
                        Text(stringResource(Res.string.vr_progress, b.progress, b.badge.target), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
