package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.model.VolunteerBadge
import ch.lauzhand.helpinghand.model.deliveriesDone
import ch.lauzhand.helpinghand.model.deliveriesInProgress
import ch.lauzhand.helpinghand.model.volunteerBadges
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private fun VolunteerBadge.title(): StringResource = when (this) {
    VolunteerBadge.FIRST_DELIVERY -> Res.string.vr_badge_first
    VolunteerBadge.FIVE_DELIVERIES -> Res.string.vr_badge_five
    VolunteerBadge.TEN_DELIVERIES -> Res.string.vr_badge_ten
}

private fun VolunteerBadge.description(): StringResource = when (this) {
    VolunteerBadge.FIRST_DELIVERY -> Res.string.vr_badge_first_desc
    VolunteerBadge.FIVE_DELIVERIES -> Res.string.vr_badge_five_desc
    VolunteerBadge.TEN_DELIVERIES -> Res.string.vr_badge_ten_desc
}

/**
 * Volunteers' Rewards tab: how many deliveries they have completed, and badges for 1, 5 and 10 deliveries.
 * Gold marks an unlocked badge, as everywhere else in the app (gold is only for rewards).
 */
@Composable
fun VolunteerRewardsScreen(repo: HelpRepository, volunteerId: String) {
    val done = deliveriesDone(repo.orders, volunteerId)
    val onTheWay = deliveriesInProgress(repo.orders, volunteerId)
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(stringResource(Res.string.achievements_title), style = MaterialTheme.typography.headlineSmall) }
        item {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(stringResource(Res.string.vr_your_deliveries), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(Res.string.vr_delivered_count, done), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                if (onTheWay > 0) Text(stringResource(Res.string.vr_in_progress, onTheWay), style = MaterialTheme.typography.bodyLarge)
            }
        }
        items(volunteerBadges(done), key = { it.badge.name }) { b ->
            val gold = MaterialTheme.app.gold
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(if (b.unlocked) 2.dp else 1.dp, if (b.unlocked) gold else MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(b.badge.title()), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        if (b.unlocked) Text(stringResource(Res.string.vr_unlocked), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(stringResource(b.badge.description()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(
                        progress = { b.progress.toFloat() / b.badge.target },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = if (b.unlocked) gold else MaterialTheme.app.accent,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Text(stringResource(Res.string.vr_progress, b.progress, b.badge.target), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
