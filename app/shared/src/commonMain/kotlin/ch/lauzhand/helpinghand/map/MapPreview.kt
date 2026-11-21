package ch.lauzhand.helpinghand.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.FakeRepository
import ch.lauzhand.helpinghand.model.Role
import ch.lauzhand.helpinghand.ui.HelpTheme

/** Stand-alone preview of [LausanneMap] with demo data, to try the map without touching the app screens. */
@Composable
fun MapPreview() {
    val repo = remember { FakeRepository() }
    var role by remember { mutableStateOf(Role.DONOR) }
    var clicked by remember { mutableStateOf("-") }
    HelpTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Role.entries.forEach { r ->
                        FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r.name) })
                    }
                    Text("clicked: $clicked")
                }
                LausanneMap(
                    zones = repo.zones,
                    people = repo.people,
                    needs = repo.needs,
                    role = role,
                    onZoneClick = { clicked = "zone $it" },
                    onPersonClick = { clicked = "person $it" },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }
    }
}
