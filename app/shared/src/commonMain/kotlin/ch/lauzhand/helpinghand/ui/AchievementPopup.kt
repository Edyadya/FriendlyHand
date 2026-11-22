package ch.lauzhand.helpinghand.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.data.ME_DONOR
import ch.lauzhand.helpinghand.model.Account
import ch.lauzhand.helpinghand.model.Role
import ch.lauzhand.helpinghand.model.donorAchievements
import ch.lauzhand.helpinghand.model.volunteerBadges
import helpinghand.app.shared.generated.resources.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Counts the "delivery confirmed" moments on screen. While one is showing, the badge pop-up waits, so two pop-ups
 * never stack. [ConfirmedMoment] adds itself to this count while it is on screen.
 */
val LocalMomentGate = compositionLocalOf<MutableIntState?> { null }

/** A badge the person has just unlocked and has not been told about yet. */
private class UnlockedBadge(val key: String, val title: StringResource, val description: StringResource, val reward: BadgeReward, val glyph: BadgeGlyph, val target: Int)

/**
 * The pop-up for a newly unlocked badge, then the confetti. Shown once per badge, after any delivery moment.
 * "See my reward" opens the reward with its QR code.
 */
@Composable
fun AchievementHost(repo: HelpRepository, account: Account) {
    val gate = LocalMomentGate.current
    val blocked = (gate?.intValue ?: 0) > 0

    val fresh: List<UnlockedBadge> = if (account.role == Role.DONOR) {
        val giveawayChf = repo.giveawayContributionsOf(ME_DONOR).sumOf { it.amountChf }
        donorAchievements(repo.contributionsOf(ME_DONOR), repo.needs, repo.people, giveawayChf)
            .filter { it.unlocked }
            .map { UnlockedBadge("p:${it.id.name}", it.id.title(), it.id.description(), it.id.reward, it.id.glyph(), it.target) }
    } else {
        volunteerBadges(repo.orders, account.id)
            .filter { it.unlocked }
            .map { UnlockedBadge("v:${it.badge.name}", it.badge.title(), it.badge.description(), it.badge.reward, it.badge.glyph(), it.badge.target) }
    }.filter { !repo.isBadgeCelebrated(it.key) }

    // wait a moment, so a delivery moment that opens in the same instant goes first
    val keys = fresh.map { it.key }
    var settled by remember(keys, blocked) { mutableStateOf(false) }
    LaunchedEffect(keys, blocked) { settled = false; delay(700); settled = true }

    var rewardFor by remember { mutableStateOf<UnlockedBadge?>(null) }
    val current = fresh.firstOrNull()
    if (current != null && rewardFor == null && !blocked && settled) {
        AchievementDialog(
            current,
            onSeeReward = { repo.markBadgesCelebrated(listOf(current.key)); rewardFor = current },
            onLater = { repo.markBadgesCelebrated(listOf(current.key)) },
        )
    }
    rewardFor?.let { b ->
        RewardInfoDialog(
            repo = repo, title = b.title, description = b.description, reward = b.reward, glyph = b.glyph,
            unlocked = true, pending = false, progress = b.target, target = b.target, qrKey = b.key,
        ) { rewardFor = null }
    }
}

@Composable
private fun AchievementDialog(b: UnlockedBadge, onSeeReward: () -> Unit, onLater: () -> Unit) {
    // the pop-up comes first, the confetti follows a moment later
    var confetti by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { delay(450); confetti = 1 }
    // the medallion starts locked, then the ring fills and it turns gold with a little pop (see BadgeMedallion)
    var unlocked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(250); unlocked = true }

    Dialog(onDismissRequest = onLater, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // full screen, so the confetti covers everything; a tap outside the card closes it
        Box(
            Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onLater() },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.padding(horizontal = 32.dp).widthIn(max = 420.dp).fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
                    .darkOutline(MaterialTheme.shapes.large)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BadgeMedallion(b.glyph, unlocked, if (unlocked) b.target else b.target - 1, b.target, size = 132.dp, animate = true)
                Text(stringResource(Res.string.achievement_unlocked), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(stringResource(b.title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                Text(stringResource(b.description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text(
                    stringResource(Res.string.reward_on_badge, b.reward.partner.displayName) + ": " + stringResource(b.reward.reward),
                    style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                AppButton(onClick = onSeeReward, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text(stringResource(Res.string.achievement_see_reward), style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = onLater, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.achievement_later)) }
            }
            ConfettiOverlay(trigger = confetti)
        }
    }
}
