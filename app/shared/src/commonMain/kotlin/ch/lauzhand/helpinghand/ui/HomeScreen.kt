package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
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

    @Composable
    fun Header() {
        Text(stringResource(Res.string.home_title), style = MaterialTheme.typography.headlineSmall)
        if (role == Role.VOLUNTEER) {
            Button(onClick = onNewPerson, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(Res.string.new_person))
            }
        }
        if (role == Role.DONOR) {
            OutlinedButton(onClick = onRewards, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(Res.string.my_rewards))
            }
        }
    }

    @Composable
    fun FilterRow() {
        val zone = repo.zones.firstOrNull { it.id == zoneFilter } ?: return
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(Res.string.zone_filter, zone.name),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { zoneFilter = null }) { Text(stringResource(Res.string.map_close_area)) }
        }
    }

    @Composable
    fun AreaPeople(modifier: Modifier) {
        if (zoneFilter == null) {
            Text(
                stringResource(Res.string.map_tap_bubble),
                modifier,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            PeopleList(repo, people, modifier, onOpenPerson)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 700.dp) {
            Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HomeMap(repo, role, zoneFilter, { zoneFilter = it }, onOpenPerson, Modifier.weight(1f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Header()
                    FilterRow()
                    AreaPeople(Modifier.weight(1f))
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // the map fills the screen until an area is picked, then makes room for its people
                val mapSize = if (zoneFilter == null) Modifier.weight(1f) else Modifier.height(260.dp)
                HomeMap(repo, role, zoneFilter, { zoneFilter = it }, onOpenPerson, Modifier.fillMaxWidth().then(mapSize))
                Header()
                FilterRow()
                AreaPeople(if (zoneFilter != null) Modifier.weight(1f) else Modifier)
            }
        }
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
