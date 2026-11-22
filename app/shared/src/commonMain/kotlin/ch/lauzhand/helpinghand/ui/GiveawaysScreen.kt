package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.data.ME_DONOR
import ch.lauzhand.helpinghand.model.Giveaway
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Menu page: brands offer their leftover goods (Zara and its clothes, for example) for free, and patrons pay for
 * the LOGISTICS as a sum goal. When the goal is reached the volunteers collect the goods and hand them out.
 */
@Composable
fun GiveawaysScreen(repo: HelpRepository, onBack: () -> Unit) {
    var paying by remember { mutableStateOf<Giveaway?>(null) }
    paying?.let { g ->
        AmountDialog(
            initial = minOf(20, g.remainingChf).coerceAtLeast(1),
            max = g.remainingChf.coerceAtLeast(1),
            onDismiss = { paying = null },
            onConfirm = { amount -> paying = null; repo.giveToGiveaway(g.id, ME_DONOR, amount) },
        )
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onBack) { Text(stringResource(Res.string.back)) }
                Text(stringResource(Res.string.giveaways_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(Res.string.giveaways_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(repo.giveaways, key = { it.id }) { g ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(g.brand, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(g.title, style = MaterialTheme.typography.titleLarge)
                    Text(g.description, style = MaterialTheme.typography.bodyMedium)
                    // the sum goal for the logistics
                    LinearProgressIndicator(
                        progress = { (g.fundedChf.toFloat() / g.logisticsGoalChf.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = MaterialTheme.app.accent,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Text(
                        stringResource(Res.string.giveaway_goal, g.fundedChf.coerceAtMost(g.logisticsGoalChf), g.logisticsGoalChf),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (g.isFunded) {
                        Text(stringResource(Res.string.giveaway_funded), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.app.success)
                    } else {
                        AppButton(onClick = { paying = g }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Text(stringResource(Res.string.giveaway_give), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    if (g.demo) {
                        Text(
                            stringResource(Res.string.giveaway_demo),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
