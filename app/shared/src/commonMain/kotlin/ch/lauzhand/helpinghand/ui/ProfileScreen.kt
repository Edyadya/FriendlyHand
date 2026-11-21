package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import ch.lauzhand.helpinghand.map.centerLat
import ch.lauzhand.helpinghand.map.centerLng
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.data.ME_DONOR
import ch.lauzhand.helpinghand.model.*
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

private val BigButtonModifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)

@Composable
fun ProfileScreen(
    repo: HelpRepository,
    role: Role,
    personId: String,
    onBack: () -> Unit,
    onAddNeed: () -> Unit,
) {
    val person = repo.people.firstOrNull { it.id == personId }
    if (person == null) { onBack(); return }
    val zoneName = repo.zones.firstOrNull { it.id == person.zoneId }?.name.orEmpty()
    val needs = repo.needs.filter { it.personId == personId }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notify: (String) -> Unit = { msg -> scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(msg) } }
    val lastSeenSaved = stringResource(Res.string.feedback_last_seen)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { inner ->
    LazyColumn(
        Modifier.fillMaxSize().padding(inner),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TextButton(onClick = onBack) { Text(stringResource(Res.string.back)) } }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(person.firstName, style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(Res.string.zone_time, zoneName, person.usualTime),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                person.ownWords?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(Res.string.own_words, it),
                        style = MaterialTheme.typography.titleMedium,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
        }
        if (role == Role.VOLUNTEER || role == Role.NGO) {
            item { LastSeenSection(repo, role, person, zoneOf(repo, person)) { notify(lastSeenSaved) } }
        }
        item { Text(stringResource(Res.string.needs_title), style = MaterialTheme.typography.titleLarge) }
        if (needs.isEmpty()) {
            item { Text(stringResource(Res.string.no_needs), style = MaterialTheme.typography.bodyLarge) }
        }
        items(needs, key = { it.id }) { NeedCard(repo, role, person, it, notify) }
        if (role == Role.VOLUNTEER) {
            item {
                OutlinedButton(onClick = onAddNeed, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.add_need))
                }
            }
        }
    }
    }
}

@Composable
private fun NeedCard(repo: HelpRepository, role: Role, person: Person, need: Need, notify: (String) -> Unit) {
    Card(
        Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(need.title, style = MaterialTheme.typography.titleLarge)
            if (need.type == NeedType.BIG) {
                val target = (need.targetAmountChf ?: 0).coerceAtLeast(1)
                val progress by animateFloatAsState((need.fundedAmountChf.toFloat() / target).coerceIn(0f, 1f))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                )
                Text(
                    stringResource(Res.string.funded_of, need.fundedAmountChf, target),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            StatusChip(need.status)
            NeedActions(repo, role, person, need, notify)
        }
    }
}

@Composable
private fun NeedActions(repo: HelpRepository, role: Role, person: Person, need: Need, notify: (String) -> Unit) {
    val canGive = need.status == NeedStatus.OPEN || need.status == NeedStatus.FUNDING
    val msgClaimed = stringResource(Res.string.feedback_claimed, need.title)
    // amount dialog: null = closed, otherwise whether this is a "pay instead" (small) or a "give" (big goal)
    var asking by remember { mutableStateOf<NeedType?>(null) }
    val scope = rememberCoroutineScope()
    val msgHanded = stringResource(Res.string.feedback_handed_over, person.firstName)
    val msgConfirmed = stringResource(Res.string.feedback_confirmed, person.firstName)
    val msgDelivered = stringResource(Res.string.feedback_delivered)
    asking?.let { type ->
        AmountDialog(
            onDismiss = { asking = null },
            onConfirm = { amount ->
                asking = null
                repo.donate(need.id, ME_DONOR, amount)
                scope.launch {
                    notify(
                        if (type == NeedType.SMALL) getString(Res.string.feedback_paid, amount, need.title)
                        else getString(Res.string.feedback_given, amount, need.title),
                    )
                }
            },
        )
    }
    when (role) {
        Role.DONOR -> {
            if (need.type == NeedType.SMALL && need.status == NeedStatus.OPEN) {
                Button(onClick = { repo.claimNeed(need.id, ME_DONOR); notify(msgClaimed) }, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.action_bring), style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(onClick = { asking = NeedType.SMALL }, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.action_pay), style = MaterialTheme.typography.titleMedium)
                }
            } else if (need.type == NeedType.BIG && canGive) {
                Button(onClick = { asking = NeedType.BIG }, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.action_give), style = MaterialTheme.typography.titleMedium)
                }
            } else if (need.type == NeedType.SMALL && need.status == NeedStatus.CLAIMED &&
                need.claimedByDonorId == ME_DONOR && !need.paid
            ) {
                Button(onClick = { repo.markFulfilled(need.id); notify(msgHanded) }, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.action_handed_over), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Role.VOLUNTEER -> if (need.status == NeedStatus.FULFILLED) {
            Button(onClick = { repo.confirmReceived(need.id); notify(msgConfirmed) }, modifier = BigButtonModifier) {
                Text(stringResource(Res.string.action_confirm, person.firstName), style = MaterialTheme.typography.titleMedium)
            }
        }
        Role.NGO -> if (need.paid && need.status == NeedStatus.CLAIMED) {
            Button(onClick = { repo.markFulfilled(need.id); notify(msgDelivered) }, modifier = BigButtonModifier) {
                Text(stringResource(Res.string.action_delivered), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private fun zoneOf(repo: HelpRepository, person: Person): Zone? = repo.zones.firstOrNull { it.id == person.zoneId }

/**
 * Exact last-seen spot: ONLY for volunteers and NGOs (donors never get this section, and the
 * server doesn't send the data to them either). Only shown while the sighting is recent.
 */
@Composable
private fun LastSeenSection(repo: HelpRepository, role: Role, person: Person, zone: Zone?, onSaved: () -> Unit) {
    val now = remember(person.lastSeenAtMillis) { nowMillis() }
    val visible = person.lastSeenVisible(now)
    var note by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (visible) {
                val ago = timeAgo(person.lastSeenAtMillis!!, now)
                val text = person.lastSeenNote?.takeIf { it.isNotBlank() }
                    ?.let { stringResource(Res.string.last_seen_with_note, it, ago) }
                    ?: stringResource(Res.string.last_seen_no_note, ago)
                Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                OutlinedButton(
                    onClick = { uriHandler.openUri("https://www.google.com/maps?q=${person.lastSeenLat},${person.lastSeenLng}") },
                    modifier = BigButtonModifier,
                ) { Text(stringResource(Res.string.open_in_maps)) }
            }
            if (role == Role.VOLUNTEER && zone != null) {
                OutlinedTextField(
                    note, { note = it }, Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.last_seen_note_field)) }, singleLine = true,
                )
                // Demo coordinates: the centre of the zone, never a real GPS fix.
                Button(
                    onClick = {
                        repo.updateLastSeen(person.id, zone.centerLat, zone.centerLng, note.trim().ifBlank { null })
                        note = ""
                        onSaved()
                    },
                    modifier = BigButtonModifier,
                ) { Text(stringResource(Res.string.update_last_seen)) }
            }
        }
    }
}
