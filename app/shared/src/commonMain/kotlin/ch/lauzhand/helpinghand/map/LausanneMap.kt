package ch.lauzhand.helpinghand.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ch.lauzhand.helpinghand.location.UserLocation
import ch.lauzhand.helpinghand.model.Cities
import ch.lauzhand.helpinghand.model.City
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
    /** The user's own position, drawn as a blue dot with a pulsing ring (null = not known). */
    userLocation: UserLocation? = null,
    /** Bump this counter to fly the camera to [userLocation] (the centre-on-me button). */
    centreOnUser: Int = 0,
    /** The city shown: where the camera starts and where it returns to. */
    city: City = Cities.LAUSANNE,
    /** When set, a tap on the empty map reports the exact latitude and longitude (used to pick a spot). */
    onMapClick: ((lat: Double, lng: Double) -> Unit)? = null,
)

/** True where [LausanneMap] draws a real map (Android, web JS). */
expect val isLiveMapSupported: Boolean

/**
 * Runs [content] (e.g. creating the Compose viewport) once the platform is ready.
 * Web JS: waits for Skiko, installs MapLibre, then runs [content]. Elsewhere: just runs [content].
 */
expect fun installMapSupport(content: () -> Unit)
