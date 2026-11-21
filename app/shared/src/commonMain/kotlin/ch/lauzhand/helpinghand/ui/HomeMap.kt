package ch.lauzhand.helpinghand.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.map.LausanneMap
import ch.lauzhand.helpinghand.map.isLiveMapSupported
import ch.lauzhand.helpinghand.model.Role

/**
 * The ONE place that decides which map Home shows:
 * the real map where it works (Android, web JS), otherwise the drawn ZoneMap.
 * Both filter the people list by zone; tapping a selected zone again clears the filter.
 */
@Composable
fun HomeMap(
    repo: HelpRepository,
    role: Role,
    zoneFilter: String?,
    onZoneFilter: (String?) -> Unit,
    onOpenPerson: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isLiveMapSupported) {
        LausanneMap(
            zones = repo.zones,
            people = repo.people,
            needs = repo.needs,
            role = role,
            onZoneClick = { onZoneFilter(if (it == zoneFilter) null else it) },
            onPersonClick = onOpenPerson,
            modifier = modifier,
        )
    } else {
        ZoneMap(repo, zoneFilter, onZoneFilter, modifier, role = role)
    }
}
