package ch.lauzhand.helpinghand.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.data.ME_DONOR
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private const val VOUCHER_GOAL = 5

@Composable
fun RewardScreen(repo: HelpRepository, onBack: () -> Unit) {
    val completed = repo.completedGoalsFor(ME_DONOR)
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack) { Text(stringResource(Res.string.back)) }
        Text(stringResource(Res.string.rewards_title), style = MaterialTheme.typography.headlineSmall)
        VoucherCard(completed)
    }
}

/** Progress towards the partner voucher; also shown on the Achievements tab. */
@Composable
fun VoucherCard(completed: Int, modifier: Modifier = Modifier, containerAlpha: Float = 1f) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = containerAlpha)),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedContent(completed) { n ->
                Text(
                    if (n == 1) stringResource(Res.string.rewards_goals_one)
                    else stringResource(Res.string.rewards_goals_many, n),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            val progress by animateFloatAsState((completed.toFloat() / VOUCHER_GOAL).coerceIn(0f, 1f))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(12.dp),
            )
            Text(
                stringResource(Res.string.rewards_progress, completed.coerceAtMost(VOUCHER_GOAL), VOUCHER_GOAL),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (completed >= VOUCHER_GOAL) {
                Text(
                    stringResource(Res.string.rewards_unlocked),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}
