package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.data.ME_DONOR
import ch.lauzhand.helpinghand.model.pendingGoals
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun RewardScreen(repo: HelpRepository, onBack: () -> Unit) {
    val completed = repo.completedGoalsFor(ME_DONOR)
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack) { Text(stringResource(Res.string.back)) }
        Text(stringResource(Res.string.rewards_title), style = MaterialTheme.typography.headlineSmall)
        val pending = pendingGoals(repo.contributionsOf(ME_DONOR), repo.needs)
        GoalsHero(completed, pending = pending)
        VoucherCard(completed, pending = pending)
    }
}
