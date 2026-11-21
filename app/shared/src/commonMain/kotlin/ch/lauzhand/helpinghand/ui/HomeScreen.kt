package ch.lauzhand.helpinghand.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.map.isLiveMapSupported
import ch.lauzhand.helpinghand.model.Need
import ch.lauzhand.helpinghand.model.NeedStatus
import ch.lauzhand.helpinghand.model.Person
import ch.lauzhand.helpinghand.model.Role
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** A need that still waits for help. */
val Need.isOpenForHelp: Boolean
    get() = status == NeedStatus.OPEN || status == NeedStatus.FUNDING

@Composable
fun HomeScreen(
    repo: HelpRepository,
    role: Role,
    onOpenPerson: (String) -> Unit,
    onNewPerson: () -> Unit,
    onRewards: () -> Unit,
) {
    // Zones are always there once data has arrived, so none means the repository is still loading.
    if (repo.zones.isEmpty()) {
        LoadingScreen()
        return
    }
    // People are only listed for the area whose bubble was tapped; nobody is listed before that.
    var zoneFilter by remember { mutableStateOf<String?>(null) }
    val people = repo.people.filter { it.zoneId == zoneFilter }

    // The area sheet keeps showing its last area while it slides away.
    val lastZone = remember { object { var id: String? = null } }
    if (zoneFilter != null) lastZone.id = zoneFilter

    // Zenly-style: the map fills the page; title, actions and the area's people float on top of it.
    Box(Modifier.fillMaxSize()) {
        HomeMap(repo, role, zoneFilter, { zoneFilter = it }, onOpenPerson, Modifier.fillMaxSize().padding(8.dp))

        // The live map draws its scale bar at the top-left, so the floating row starts just below it.
        Row(
            Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = if (isLiveMapSupported) 48.dp else 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(Modifier.weight(1f)) {
                FloatingPill {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                        Text(stringResource(Res.string.home_title), style = MaterialTheme.typography.titleMedium)
                        if (zoneFilter == null) {
                            Text(
                                stringResource(Res.string.map_tap_bubble),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (role == Role.DONOR) {
                FloatingPill(onClick = onRewards) {
                    Text(
                        stringResource(Res.string.my_rewards),
                        Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (role == Role.VOLUNTEER) {
                ExtendedFloatingActionButton(
                    onClick = onNewPerson,
                    shape = RoundedCornerShape(28.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) { Text(stringResource(Res.string.new_person), style = MaterialTheme.typography.titleMedium) }
            }
            AnimatedVisibility(
                visible = zoneFilter != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                val zoneId = lastZone.id ?: return@AnimatedVisibility
                AreaSheet(repo, zoneId, onClose = { zoneFilter = null }, onOpenPerson = onOpenPerson)
            }
        }
    }
}

/** Rounded, slightly see-through card floating over the map. */
@Composable
private fun FloatingPill(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, shadowElevation = 6.dp) { content() }
    } else {
        Surface(modifier = modifier, shape = shape, color = color, shadowElevation = 6.dp) { content() }
    }
}

/** Bottom sheet for the tapped area: who is there, as round bubbles. Tap one to open their profile. */
@Composable
private fun AreaSheet(repo: HelpRepository, zoneId: String, onClose: () -> Unit, onOpenPerson: (String) -> Unit) {
    val zone = repo.zones.firstOrNull { it.id == zoneId }
    val people = repo.people.filter { it.zoneId == zoneId }
    Surface(
        Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
    ) {
        Column(Modifier.padding(top = 10.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant),
            )
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(zone?.name.orEmpty(), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        if (people.size == 1) stringResource(Res.string.map_people_here_one)
                        else stringResource(Res.string.map_people_here_many, people.size),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onClose) { Text(stringResource(Res.string.map_close_area)) }
            }
            if (people.isEmpty()) {
                Text(
                    stringResource(Res.string.no_people),
                    Modifier.padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(people, key = { _, p -> p.id }) { i, p ->
                        PersonBubble(repo, p, i) { onOpenPerson(p.id) }
                    }
                }
            }
        }
    }
}

/** A person as a round initial "bubble" with their name and how many needs are open. */
@Composable
private fun PersonBubble(repo: HelpRepository, person: Person, index: Int, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (bg, fg) = when (index % 3) {
        0 -> colors.primaryContainer to colors.onPrimaryContainer
        1 -> colors.secondaryContainer to colors.onSecondaryContainer
        else -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
    val open = repo.needs.count { it.personId == person.id && it.isOpenForHelp }
    Column(
        Modifier.width(132.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(bg)
                .border(3.dp, colors.surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(person.firstName.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, color = fg)
        }
        Text(
            person.age?.let { stringResource(Res.string.person_age, person.firstName, it) } ?: person.firstName,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
        Text(
            when (open) {
                0 -> stringResource(Res.string.no_open_needs)
                1 -> stringResource(Res.string.open_needs_one)
                else -> stringResource(Res.string.open_needs_many, open)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.primary,
            maxLines = 2,
        )
    }
}

@Composable
fun PeopleList(
    repo: HelpRepository,
    people: List<Person>,
    modifier: Modifier = Modifier,
    onOpenPerson: (String) -> Unit,
) {
    if (people.isEmpty()) {
        Text(stringResource(Res.string.no_people), modifier, style = MaterialTheme.typography.bodyLarge)
        return
    }
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(people, key = { it.id }) { p -> PersonCard(repo, p) { onOpenPerson(p.id) } }
    }
}

@Composable
private fun PersonCard(repo: HelpRepository, person: Person, onClick: () -> Unit) {
    val zoneName = repo.zones.firstOrNull { it.id == person.zoneId }?.name.orEmpty()
    val open = repo.needs.count { it.personId == person.id && it.isOpenForHelp }
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val name = person.age?.let { stringResource(Res.string.person_age, person.firstName, it) }
                ?: person.firstName
            Text(name, style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(Res.string.zone_time, zoneName, person.usualTime),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                when (open) {
                    0 -> stringResource(Res.string.no_open_needs)
                    1 -> stringResource(Res.string.open_needs_one)
                    else -> stringResource(Res.string.open_needs_many, open)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
