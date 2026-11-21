package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.model.Need
import ch.lauzhand.helpinghand.model.NeedStatus
import ch.lauzhand.helpinghand.model.Person
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** A need that still waits for help. */
val Need.isOpenForHelp: Boolean
    get() = status == NeedStatus.OPEN || status == NeedStatus.FUNDING

@Composable
fun HomeScreen(
    repo: HelpRepository,
    onOpenPerson: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.home_title), style = MaterialTheme.typography.headlineSmall)
        PeopleList(repo, repo.people, Modifier.weight(1f), onOpenPerson)
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
