package ch.lauzhand.helpinghand.location

import androidx.compose.runtime.Composable

/** Where the user is. [isDemo] is true for the fixed demo position used where there is no real location. */
data class UserLocation(val lat: Double, val lng: Double, val accuracyMetres: Float, val isDemo: Boolean)

enum class LocationStatus {
    /** A real position is known. */
    LOCATED,
    /** No real location on this platform: the fixed demo position is used. */
    DEMO,
    /** The user said no (or location is off): the map keeps working, with a small hint. */
    DENIED,
    /** Allowed, but no position yet. */
    SEARCHING,
}

/** [askPermission] asks again (a no-op where there is nothing to ask). */
class LocationState(val location: UserLocation?, val status: LocationStatus, val askPermission: () -> Unit)

/**
 * DEMO LOCATION, NOT A REAL POSITION: a fixed spot in Lausanne (between the station and the lake) used on
 * Desktop and web, so the "you are here" dot can be shown without any location API.
 */
val DemoLocation = UserLocation(lat = 46.5105, lng = 6.6255, accuracyMetres = 60f, isDemo = true)

internal fun demoLocationState() = LocationState(DemoLocation, LocationStatus.DEMO) {}

/**
 * The user's location as Compose state.
 * Android: the real position, after asking for permission. Desktop and web: the fixed [DemoLocation].
 * (There is no iOS target in this project, so no CoreLocation implementation is needed yet.)
 */
@Composable
expect fun rememberUserLocation(): LocationState
