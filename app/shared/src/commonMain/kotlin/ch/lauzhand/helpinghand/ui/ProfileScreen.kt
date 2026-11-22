package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
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
import ch.lauzhand.helpinghand.location.rememberUserLocation
import ch.lauzhand.helpinghand.data.ME_DONOR
import ch.lauzhand.helpinghand.model.*
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

// One-tap amounts, plus the slider range for "Other amount"
private const val QUICK_PAY_CHF = 20
private const val QUICK_GIVE_CHF = 50
private const val MAX_SMALL_CHF = 100
private const val MAX_BIG_CHF = 1000

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

    Box(Modifier.fillMaxSize()) {
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
                // their photo, only if they consented to it
                if (person.photoConsent && person.photo != null) PersonAvatar(person, 96.dp)
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
        if (role == Role.VOLUNTEER) {
            item { LastSeenSection(repo, role, person, zoneOf(repo, person)) { notify(lastSeenSaved) } }
        }
        if (role == Role.VOLUNTEER) {
            item {
                PhotoSection(
                    consent = person.photoConsent,
                    photo = person.photo,
                    onConsentChange = { consent -> repo.setPhoto(person.id, if (consent) person.photo else null, consent) },
                    onPhotoChange = { repo.setPhoto(person.id, it, consent = true) },
                )
            }
        }
        item { Text(stringResource(Res.string.needs_title), style = MaterialTheme.typography.titleLarge) }
        if (needs.isEmpty()) {
            item { Text(stringResource(Res.string.no_needs), style = MaterialTheme.typography.bodyLarge) }
        }
        items(needs, key = { it.id }) { NeedCard(repo, role, person, it, notify) }
        if (role == Role.VOLUNTEER) {
            item {
                AppOutlinedButton(onClick = onAddNeed, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.add_need))
                }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(need.title, style = MaterialTheme.typography.titleLarge)
            if (need.type == NeedType.BIG) {
                val target = (need.targetAmountChf ?: 0).coerceAtLeast(1)
                val progress by animateFloatAsState((need.fundedAmountChf.toFloat() / target).coerceIn(0f, 1f))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = MaterialTheme.app.accent,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
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
    val msgHanded = stringResource(Res.string.feedback_handed_over, person.firstName)
    val msgConfirmed = stringResource(Res.string.feedback_confirmed, person.firstName)
    val msgDelivered = stringResource(Res.string.feedback_delivered)

    // slider dialog: null = closed, otherwise whether this is for a small need or a big goal
    var choosing by remember { mutableStateOf<NeedType?>(null) }
    val scope = rememberCoroutineScope()
    var showMoment by remember { mutableStateOf(false) }

    fun give(amount: Int, type: NeedType) {
        repo.donate(need.id, ME_DONOR, amount)
        scope.launch {
            notify(
                if (type == NeedType.SMALL) getString(Res.string.feedback_paid, amount, need.title)
                else getString(Res.string.feedback_given, amount, need.title),
            )
        }
    }

    choosing?.let { type ->
        val remaining = ((need.targetAmountChf ?: 0) - need.fundedAmountChf).coerceAtLeast(0)
        AmountDialog(
            initial = if (type == NeedType.SMALL) QUICK_PAY_CHF else QUICK_GIVE_CHF,
            max = if (type == NeedType.SMALL) MAX_SMALL_CHF else maxOf(remaining, QUICK_GIVE_CHF).coerceAtMost(MAX_BIG_CHF),
            onDismiss = { choosing = null },
            onConfirm = { amount -> choosing = null; give(amount, type) },
        )
    }
    if (showMoment) {
        ConfirmedMoment(repo.needs.count { it.status == NeedStatus.CONFIRMED }) { showMoment = false }
    }
    when (role) {
        Role.DONOR -> {
            if (need.type == NeedType.SMALL && need.status == NeedStatus.OPEN) {
                AppButton(onClick = { repo.claimNeed(need.id, ME_DONOR); notify(msgClaimed) }, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.action_bring), style = MaterialTheme.typography.titleMedium)
                }
                AppOutlinedButton(onClick = { give(QUICK_PAY_CHF, NeedType.SMALL) }, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.action_pay), style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = { choosing = NeedType.SMALL }, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.action_other_amount), style = MaterialTheme.typography.titleMedium)
                }
            } else if (need.type == NeedType.BIG && canGive) {
                AppButton(onClick = { give(QUICK_GIVE_CHF, NeedType.BIG) }, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.action_give), style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = { choosing = NeedType.BIG }, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.action_other_amount), style = MaterialTheme.typography.titleMedium)
                }
            } else if (need.type == NeedType.SMALL && need.status == NeedStatus.CLAIMED &&
                need.claimedByDonorId == ME_DONOR && !need.paid
            ) {
                AppButton(onClick = { repo.markFulfilled(need.id); notify(msgHanded) }, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.action_handed_over), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Role.VOLUNTEER -> {
            // a donor paid: the volunteer buys the item, brings it and marks it delivered
            if (need.paid && need.status == NeedStatus.CLAIMED) {
                AppButton(onClick = { repo.markFulfilled(need.id); notify(msgDelivered) }, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.action_delivered), style = MaterialTheme.typography.titleMedium)
                }
            }
            if (need.status == NeedStatus.FULFILLED) {
                AppButton(onClick = { repo.confirmReceived(need.id); notify(msgConfirmed); showMoment = true }, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.action_confirm, person.firstName), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

private fun zoneOf(repo: HelpRepository, person: Person): Zone? = repo.zones.firstOrNull { it.id == person.zoneId }

/**
 * Exact last-seen spot: ONLY for volunteers (donors never get this section, and the
 * server doesn't send the data to them either). Only shown while the sighting is recent.
 */
@Composable
private fun LastSeenSection(repo: HelpRepository, role: Role, person: Person, zone: Zone?, onSaved: () -> Unit) {
    val now = remember(person.lastSeenAtMillis) { nowMillis() }
    val visible = person.lastSeenVisible(now)
    var note by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }
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
                AppOutlinedButton(
                    onClick = { uriHandler.openUri("https://www.google.com/maps?q=${person.lastSeenLat},${person.lastSeenLng}") },
                    modifier = BigButtonModifier,
                ) { Text(stringResource(Res.string.open_in_maps)) }
            }
            if (role == Role.VOLUNTEER) {
                // The exact spot is the volunteer's own position right now (they are standing next to the person).
                val me = rememberUserLocation()
                val here = me.location
                OutlinedTextField(
                    note, { note = it }, Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.last_seen_note_field)) }, singleLine = true,
                )
                AppButton(
                    onClick = {
                        if (here != null) {
                            repo.updateLastSeen(person.id, here.lat, here.lng, note.trim().ifBlank { null })
                            note = ""
                            onSaved()
                        }
                    },
                    enabled = here != null,
                    modifier = BigButtonModifier,
                ) { Text(stringResource(Res.string.last_seen_here)) }
                AppOutlinedButton(onClick = { picking = true }, modifier = BigButtonModifier) {
                    Text(stringResource(Res.string.spot_pick_on_map))
                }
                if (picking) {
                    val city = zone?.let { Cities.byId(it.cityId) } ?: Cities.LAUSANNE
                    SpotPickerDialog(
                        repo, city, start = here,
                        onPick = { lat, lng ->
                            repo.updateLastSeen(person.id, lat, lng, note.trim().ifBlank { null })
                            note = ""
                            picking = false
                            onSaved()
                        },
                        onDismiss = { picking = false },
                    )
                }
                Text(
                    when {
                        here == null -> stringResource(Res.string.last_seen_no_location)
                        here.isDemo -> stringResource(Res.string.last_seen_demo_note)
                        else -> stringResource(Res.string.last_seen_exact_hint)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}
