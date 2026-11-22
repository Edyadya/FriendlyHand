package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.data.ME_DONOR
import ch.lauzhand.helpinghand.model.*
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Where a contribution is on its way: pledged -> picked up -> delivered. */
private enum class Stage(val label: StringResource) {
    PLEDGED(Res.string.stage_pledged),
    PICKED_UP(Res.string.stage_picked_up),
    DELIVERED(Res.string.stage_delivered),
}

private fun NeedStatus.stage(): Stage = when (this) {
    NeedStatus.OPEN, NeedStatus.CLAIMED, NeedStatus.FUNDING -> Stage.PLEDGED
    NeedStatus.FULFILLED -> Stage.PICKED_UP
    NeedStatus.CONFIRMED -> Stage.DELIVERED
}

@Composable
private fun Stage.dotColor(): Color = when (this) {
    Stage.PLEDGED -> MaterialTheme.app.accent
    Stage.PICKED_UP -> MaterialTheme.colorScheme.primary
    Stage.DELIVERED -> MaterialTheme.app.success
}

/** A coloured dot with its label, so the stage is never shown by colour alone. */
@Composable
private fun StageDot(stage: Stage) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(stage.dotColor()))
        Text(stringResource(stage.label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Donor tab: everything I brought, paid or gave, newest first. Tapping one opens the person. */
@Composable
fun ContributionsScreen(repo: HelpRepository, onOpenPerson: (String) -> Unit) {
    val contributions = repo.contributionsOf(ME_DONOR).sortedByDescending { it.atMillis }
    val giveawayPayments = repo.giveawayContributionsOf(ME_DONOR)
    // everything the patron did, newest first: help for people, and payments towards giveaways
    val timeline: List<Pair<Long, Any>> = (contributions.map { it.atMillis to (it as Any) } + giveawayPayments.map { it.atMillis to (it as Any) })
        .sortedByDescending { it.first }
    val needById = repo.needs.associateBy { it.id }
    val personById = repo.people.associateBy { it.id }
    val helpedPeople = contributions.mapNotNull { needById[it.needId]?.personId }.toSet().size

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(Res.string.contrib_title), style = MaterialTheme.typography.headlineSmall)
                if (helpedPeople > 0) {
                    Text(
                        if (helpedPeople == 1) stringResource(Res.string.contrib_summary_one)
                        else stringResource(Res.string.contrib_summary_many, helpedPeople),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        if (timeline.isEmpty()) {
            item {
                Text(
                    stringResource(Res.string.contrib_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(timeline, key = { i, e -> "${e.first}-$i" }) { _, entry ->
            when (val item = entry.second) {
                is Contribution -> {
                    val need = needById[item.needId] ?: return@itemsIndexed
                    val order = repo.orders.firstOrNull { it.needId == need.id }
                    ContributionCard(item, need, personById[need.personId], order) { onOpenPerson(need.personId) }
                }
                is GiveawayContribution -> GiveawayPaymentCard(item, repo.giveaways.firstOrNull { it.id == item.giveawayId })
            }
        }
    }
}

@Composable
private fun ContributionCard(c: Contribution, need: Need, person: Person?, order: DeliveryOrder?, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(Res.string.contrib_person_need, person?.firstName.orEmpty(), need.title),
                style = MaterialTheme.typography.titleLarge,
            )
            // the date stamp
            if (c.atMillis > 0) {
                Text(
                    stringResource(Res.string.contrib_on_date, formatDate(c.atMillis)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                when (c.kind) {
                    ContributionKind.BROUGHT -> stringResource(Res.string.contrib_brought)
                    ContributionKind.PAID -> stringResource(Res.string.contrib_paid, c.amountChf ?: 0)
                    ContributionKind.GAVE -> stringResource(Res.string.contrib_gave, c.amountChf ?: 0)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            val bigTarget = need.targetAmountChf
            if (need.type == NeedType.BIG && bigTarget != null) {
                val target = bigTarget.coerceAtLeast(1)
                LinearProgressIndicator(
                    progress = { (need.fundedAmountChf.toFloat() / target).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.app.accent,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                )
                Text(
                    stringResource(Res.string.funded_of, need.fundedAmountChf, target),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StageDot(need.status.stage())
            // the hand-over: which volunteer has the order
            order?.let {
                Text(it.status.line(it, person?.firstName.orEmpty()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun GiveawayPaymentCard(c: GiveawayContribution, giveaway: Giveaway?) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(giveaway?.brand.orEmpty(), style = MaterialTheme.typography.titleLarge)
            if (c.atMillis > 0) {
                Text(stringResource(Res.string.contrib_on_date, formatDate(c.atMillis)), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(stringResource(Res.string.giveaway_contribution_line, c.amountChf, giveaway?.title.orEmpty()), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
