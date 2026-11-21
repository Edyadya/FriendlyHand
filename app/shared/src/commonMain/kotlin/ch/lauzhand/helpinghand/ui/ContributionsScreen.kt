package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.clickable
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

private fun NeedStatus.statusText(): StringResource = when (this) {
    NeedStatus.OPEN -> Res.string.status_open
    NeedStatus.CLAIMED -> Res.string.status_claimed
    NeedStatus.FUNDING -> Res.string.status_funding
    NeedStatus.FULFILLED -> Res.string.status_fulfilled
    NeedStatus.CONFIRMED -> Res.string.status_confirmed
}

/** Donor tab: everything I brought, paid or gave, newest first. Tapping one opens the person. */
@Composable
fun ContributionsScreen(repo: HelpRepository, onOpenPerson: (String) -> Unit) {
    val contributions = repo.contributionsOf(ME_DONOR).sortedByDescending { it.atMillis }
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
                if (contributions.isNotEmpty()) {
                    Text(
                        if (helpedPeople == 1) stringResource(Res.string.contrib_summary_one)
                        else stringResource(Res.string.contrib_summary_many, helpedPeople),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        if (contributions.isEmpty()) {
            item {
                Text(
                    stringResource(Res.string.contrib_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(contributions, key = { i, c -> "${c.needId}-${c.atMillis}-$i" }) { _, c ->
            val need = needById[c.needId] ?: return@itemsIndexed
            ContributionCard(c, need, personById[need.personId]) { onOpenPerson(need.personId) }
        }
    }
}

@Composable
private fun ContributionCard(c: Contribution, need: Need, person: Person?, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(Res.string.contrib_person_need, person?.firstName.orEmpty(), need.title),
                style = MaterialTheme.typography.titleLarge,
            )
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
                )
                Text(
                    stringResource(Res.string.funded_of, need.fundedAmountChf, target),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    stringResource(need.status.statusText()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
