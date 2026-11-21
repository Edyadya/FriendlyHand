package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import org.jetbrains.compose.resources.stringResource

private fun NeedStatus.label(): StringResource = when (this) {
    NeedStatus.OPEN -> Res.string.status_open
    NeedStatus.CLAIMED -> Res.string.status_claimed
    NeedStatus.FUNDING -> Res.string.status_funding
    NeedStatus.FULFILLED -> Res.string.status_fulfilled
    NeedStatus.CONFIRMED -> Res.string.status_confirmed
}

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

    LazyColumn(
        Modifier.fillMaxSize(),
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
            item { LastSeenSection(repo, role, person, zoneOf(repo, person)) }
        }
        item { Text(stringResource(Res.string.needs_title), style = MaterialTheme.typography.titleLarge) }
        if (needs.isEmpty()) {
            item { Text(stringResource(Res.string.no_needs), style = MaterialTheme.typography.bodyLarge) }
        }
        items(needs, key = { it.id }) { NeedCard(repo, role, person, it) }
        if (role == Role.VOLUNTEER) {
            item {
                OutlinedButton(onClick = onAddNeed, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.add_need))
                }
            }
        }
    }
}

@Composable
private fun NeedCard(repo: HelpRepository, role: Role, person: Person, need: Need) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(need.title, style = MaterialTheme.typography.titleLarge)
            if (need.type == NeedType.BIG) {
                val target = (need.targetAmountChf ?: 0).coerceAtLeast(1)
                LinearProgressIndicator(
                    progress = { (need.fundedAmountChf.toFloat() / target).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                )
                Text(
                    stringResource(Res.string.funded_of, need.fundedAmountChf, target),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text(
                    stringResource(need.status.label()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            NeedActions(repo, role, person, need)
        }
    }
}

@Composable
private fun NeedActions(repo: HelpRepository, role: Role, person: Person, need: Need) {
    val canGive = need.status == NeedStatus.OPEN || need.status == NeedStatus.FUNDING
    when (role) {
        Role.DONOR -> {
            if (need.type == NeedType.SMALL && need.status == NeedStatus.OPEN) {
                Button(onClick = { repo.claimNeed(need.id, ME_DONOR) }, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.action_bring))
                }
                OutlinedButton(onClick = { repo.donate(need.id, ME_DONOR, 20) }, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.action_pay))
                }
            } else if (need.type == NeedType.BIG && canGive) {
                Button(onClick = { repo.donate(need.id, ME_DONOR, 50) }, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.action_give))
                }
            } else if (need.type == NeedType.SMALL && need.status == NeedStatus.CLAIMED &&
                need.claimedByDonorId == ME_DONOR && !need.paid
            ) {
                Button(onClick = { repo.markFulfilled(need.id) }, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.action_handed_over))
                }
            }
        }
        Role.VOLUNTEER -> if (need.status == NeedStatus.FULFILLED) {
            Button(onClick = { repo.confirmReceived(need.id) }, modifier = BigButtonModifier) {
                Text(stringResource(Res.string.action_confirm, person.firstName))
            }
        }
        Role.NGO -> if (need.paid && need.status == NeedStatus.CLAIMED) {
            Button(onClick = { repo.markFulfilled(need.id) }, modifier = BigButtonModifier) {
                Text(stringResource(Res.string.action_delivered))
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
private fun LastSeenSection(repo: HelpRepository, role: Role, person: Person, zone: Zone?) {
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
                    },
                    modifier = BigButtonModifier,
                ) { Text(stringResource(Res.string.update_last_seen)) }
            }
        }
    }
}
