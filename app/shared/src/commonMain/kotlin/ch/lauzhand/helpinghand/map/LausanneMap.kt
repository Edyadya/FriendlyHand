package ch.lauzhand.helpinghand.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ch.lauzhand.helpinghand.model.Need
import ch.lauzhand.helpinghand.model.Person
import ch.lauzhand.helpinghand.model.Role
import ch.lauzhand.helpinghand.model.Zone

/**
 * A real map of Lausanne (MapLibre) on Android and web (Kotlin/JS).
 * Wasm and desktop show a simple placeholder, so use the drawn zone map there.
 *
 * - Everyone sees one translucent circle per zone with its number of open needs
 *   (never exact positions).
 * - VOLUNTEERs also see the exact last-seen markers (only recent ones, see lastSeenVisible).
 */
@Composable
expect fun LausanneMap(
    zones: List<Zone>,
    people: List<Person>,
    needs: List<Need>,
    role: Role,
    onZoneClick: (zoneId: String) -> Unit,
    onPersonClick: (personId: String) -> Unit,
    modifier: Modifier = Modifier,
)

/** True where [LausanneMap] draws a real map (Android, web JS). */
expect val isLiveMapSupported: Boolean

/**
 * Runs [content] (e.g. creating the Compose viewport) once the platform is ready.
 * Web JS: waits for Skiko, installs MapLibre, then runs [content]. Elsewhere: just runs [content].
 */
expect fun installMapSupport(content: () -> Unit)
