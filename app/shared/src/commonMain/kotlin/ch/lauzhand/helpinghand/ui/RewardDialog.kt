package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * What tapping a badge shows: the badge, its reward and the partner who gives it. Once the badge is unlocked it also
 * shows a QR code to scan at the partner. The code is random: made once per badge and then always the same.
 */
@Composable
fun RewardInfoDialog(
    repo: HelpRepository,
    title: StringResource,
    description: StringResource,
    reward: BadgeReward,
    unlocked: Boolean,
    pending: Boolean,
    progress: Int,
    target: Int,
    qrKey: String,
    icon: @Composable () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = colors.surface,
        confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.reward_close)) } },
        icon = icon,
        title = { Text(stringResource(title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(description), style = MaterialTheme.typography.bodyLarge)
                // the reward itself: surface card with the gold edge (gold = reward)
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                        .clip(AppShape.card)
                        .background(colors.surface)
                        .border(MaterialTheme.borderWidth, colors.outlineVariant, AppShape.card),
                ) {
                    Box(Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.app.gold))
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PartnerMonogram(reward.partner, Modifier.size(44.dp))
                        Column {
                            Text(stringResource(Res.string.reward_label), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                            Text(stringResource(reward.reward), style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                            Text(stringResource(Res.string.reward_from, reward.partner.displayName), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                        }
                    }
                }
                Text(
                    when {
                        unlocked -> stringResource(Res.string.reward_unlocked, reward.partner.displayName)
                        pending -> stringResource(Res.string.badge_pending_hint)
                        else -> stringResource(Res.string.reward_locked, progress, target)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (unlocked) colors.primary else colors.onSurfaceVariant,
                )
                if (unlocked) {
                    // the random QR code, to scan at the partner
                    val code = repo.rewardCodeFor(qrKey)
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        QrView(
                            text = "HELPINGHAND|${reward.partner.displayName}|$qrKey|$code",
                            description = stringResource(Res.string.reward_qr_hint, reward.partner.displayName),
                            modifier = Modifier.size(200.dp).clip(AppShape.chip),
                        )
                        Text(stringResource(Res.string.reward_qr_hint, reward.partner.displayName), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        Text(stringResource(Res.string.reward_code, code), style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    }
                }
            }
        },
    )
}

/** A round badge with the partner's initials (no logos are used). */
@Composable
internal fun PartnerMonogram(p: Partner, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.clip(CircleShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
        Text(p.monogram, color = colors.onPrimaryContainer, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}
