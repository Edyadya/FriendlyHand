package ch.lauzhand.helpinghand.ui

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.model.CommunityImpact
import ch.lauzhand.helpinghand.model.Coverage
import ch.lauzhand.helpinghand.model.Need
import ch.lauzhand.helpinghand.model.NeedType
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * The community impact strip: a navy panel with three big numbers (people helped, items delivered, CHF funded).
 * With [animate] the numbers count up, which is what the delivery moment uses.
 */
@Composable
fun ImpactStrip(impact: CommunityImpact, modifier: Modifier = Modifier, animate: Boolean = false, title: String? = stringResource(Res.string.impact_title)) {
    Column(
        modifier.fillMaxWidth().darkOutline(MaterialTheme.shapes.large).background(MaterialTheme.app.navy, MaterialTheme.shapes.large).padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        title?.let {
            Text(it, Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.app.onNavy.copy(alpha = 0.75f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ImpactNumber(impact.peopleHelped, Res.string.impact_people, animate, Modifier.weight(1f))
            ImpactNumber(impact.itemsDelivered, Res.string.impact_items, animate, Modifier.weight(1f))
            ImpactNumber(impact.chfFunded, Res.string.impact_chf, animate, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ImpactNumber(value: Int, label: org.jetbrains.compose.resources.StringResource, animate: Boolean, modifier: Modifier) {
    val shown by animateIntAsState(if (animate) value else value, if (animate) tween(1100, delayMillis = 350) else tween(0))
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            (if (animate) shown else value).toString(),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.app.onNavy,
            maxLines = 1,
        )
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.app.onNavy.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
    }
}

/** "18 of 25 needs fulfilled (72%)" with a progress bar, and how many are still waiting. */
@Composable
fun CoverageCard(coverage: Coverage, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(Res.string.coverage_text, coverage.fulfilled, coverage.total, coverage.percent),
                style = MaterialTheme.typography.titleMedium,
            )
            LinearProgressIndicator(
                progress = { coverage.fraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = MaterialTheme.app.accent,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
            )
            if (coverage.waiting > 0) {
                Text(
                    stringResource(Res.string.coverage_waiting, coverage.waiting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * The impact line of the delivery moment: "Your socks reached Luc near Flon". [mine] is true for the patron who
 * gave it; the volunteer who confirmed it sees "The socks reached Luc near Flon".
 */
@Composable
fun reachedLine(need: Need, personName: String, zoneName: String, mine: Boolean): String {
    val item = need.title.replaceFirstChar { it.lowercase() }
    return when {
        !mine -> stringResource(Res.string.moment_reached, item, personName, zoneName)
        need.type == NeedType.BIG -> stringResource(Res.string.moment_reached_gift, item, personName, zoneName)
        else -> stringResource(Res.string.moment_reached_yours, item, personName, zoneName)
    }
}
