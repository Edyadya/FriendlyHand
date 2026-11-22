package ch.lauzhand.helpinghand.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.lauzhand.helpinghand.location.UserLocation
import ch.lauzhand.helpinghand.model.*
import ch.lauzhand.helpinghand.ui.app
import ch.lauzhand.helpinghand.ui.isDark
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.map.LocalMapState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToString
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MapUiOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.overlay.AttributionDefaults
import org.maplibre.compose.overlay.ExpandingAttributionButton
import org.maplibre.compose.overlay.MapOverlayScope
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.geojson.dsl.addFeature
import org.maplibre.spatialk.geojson.dsl.buildFeatureCollection

// Shared by the Android and web-JS targets (see app/shared/build.gradle.kts).

// The light and dark base styles are defined once in MapStyles (commonMain).

private fun City.overview() = Position(longitude = lng, latitude = lat)
private const val ZONE_ZOOM = 14.8
private const val USER_ZOOM = 15.5

private fun Zone.position() = Position(longitude = centerLng, latitude = centerLat)

private fun JsonObject?.id(): String? = this?.get("id")?.jsonPrimitive?.contentOrNull

@Composable
actual fun LausanneMap(
    zones: List<Zone>,
    people: List<Person>,
    needs: List<Need>,
    role: Role,
    onZoneClick: (zoneId: String) -> Unit,
    onPersonClick: (personId: String) -> Unit,
    modifier: Modifier,
    userLocation: UserLocation?,
    centreOnUser: Int,
    city: City,
    onMapClick: ((lat: Double, lng: Double) -> Unit)?,
) {
    // The base map follows the APP's dark-mode switch (not only the system setting). A new style means a new map,
    // so it is rebuilt when the theme flips, starting from where the camera was.
    val dark = MaterialTheme.isDark
    val camera = remember { arrayOfNulls<CameraPosition>(1) }
    val focused = remember { mutableStateOf<String?>(null) } // the tapped bubble
    key(dark) {
        LausanneMapStyled(zones, people, needs, role, onZoneClick, onPersonClick, modifier, userLocation, centreOnUser, city, onMapClick, dark, camera, focused)
    }
}

@Composable
private fun LausanneMapStyled(
    zones: List<Zone>,
    people: List<Person>,
    needs: List<Need>,
    role: Role,
    onZoneClick: (zoneId: String) -> Unit,
    onPersonClick: (personId: String) -> Unit,
    modifier: Modifier,
    userLocation: UserLocation?,
    centreOnUser: Int,
    city: City,
    onMapClick: ((lat: Double, lng: Double) -> Unit)?,
    dark: Boolean,
    camera: Array<CameraPosition?>,
    focused: MutableState<String?>,
) {
    val onZone by rememberUpdatedState(onZoneClick)
    val zonesNow by rememberUpdatedState(zones)
    val cityNow by rememberUpdatedState(city)
    val scope = rememberCoroutineScope()
    var focusedZoneId by focused

    // Which zones get a bubble, and the number on it, depends on the role (see zoneBubbleCount).
    val zoneData = remember(zones, people, needs, role) {
        buildFeatureCollection<Point, JsonObject?> {
            for (z in zones) {
                val count = zoneBubbleCount(role, z.id, people, needs) ?: continue
                addFeature(
                    geometry = Point(z.position()),
                    properties = buildJsonObject { put("id", z.id); put("label", count.toString()) },
                )
            }
        }
    }

    // Concentric ring around the tapped bubble (accent at 30 %).
    val ringData = remember(focusedZoneId, zones) {
        buildFeatureCollection<Point, JsonObject?> {
            zones.firstOrNull { it.id == focusedZoneId }?.let { z ->
                addFeature(geometry = Point(z.position()), properties = buildJsonObject { put("id", z.id) })
            }
        }
    }

    // The user's own position: a blue dot with a white ring and a slow pulsing ring around it.
    val userData = remember(userLocation) {
        buildFeatureCollection<Point, JsonObject?> {
            userLocation?.let { addFeature(geometry = Point(Position(longitude = it.lng, latitude = it.lat)), properties = buildJsonObject { put("id", "me") }) }
        }
    }
    val pulseTransition = rememberInfiniteTransition(label = "me")
    val pulseRaw by pulseTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "pulse")
    // updated in 16 steps per cycle, so the map is not restyled on every frame
    val pulse by remember { derivedStateOf { (pulseRaw * 16).toInt() / 16f } }

    val zoneColor = MaterialTheme.app.accent                     // the user's own position stays accent blue
    // The area bubbles have a colour per role, both from the colour scheme: patrons accent blue, volunteers teal
    val bubbleColor = if (role == Role.VOLUNTEER) MaterialTheme.app.volunteer else MaterialTheme.app.bubble
    val outline = MaterialTheme.app.onNavy                       // white ring
    val labelColor = MaterialTheme.app.onNavy                    // white number (large and bold, so 3.8:1 is enough)
    val styleUrl = MapStyles.forTheme(dark)

    val state = rememberMapState(
        baseStyle = BaseStyle.Uri(styleUrl),
        initialCameraPosition = camera[0] ?: CameraPosition(center = city.overview(), zoom = city.zoom),
    ) {
        val mapState = LocalMapState.current
        val zoneSource = rememberGeoJsonSource(GeoJsonData.Features(zoneData))
        val ringSource = rememberGeoJsonSource(GeoJsonData.Features(ringData))
        val userSource = rememberGeoJsonSource(GeoJsonData.Features(userData))

        CircleLayer(
            id = "zone-selected-ring",
            source = ringSource,
            radius = const(44.dp),
            color = const(bubbleColor),
            opacity = const(0.30f),
        )
        CircleLayer(
            id = "zones",
            source = zoneSource,
            radius = const(26.dp),
            color = const(bubbleColor),
            opacity = const(1f),
            strokeColor = const(outline),
            strokeWidth = const(3.dp),
            hitPadding = 8.dp,
            onClick = { features ->
                features.firstOrNull()?.properties.id()?.let { id ->
                    // Tap a zone: fly in on it. Tap the same zone again: fly back out to all of Lausanne.
                    val zone = zonesNow.firstOrNull { it.id == id }
                    val zoomingOut = focusedZoneId == id
                    focusedZoneId = if (zoomingOut) null else id
                    if (zone != null) scope.launch {
                        mapState?.animateCamera(
                            update = if (zoomingOut) CameraUpdate(center = cityNow.overview(), zoom = cityNow.zoom)
                            else CameraUpdate(center = zone.position(), zoom = ZONE_ZOOM),
                            animation = CameraAnimation.Fly { duration = 1.seconds },
                        )
                    }
                    onZone(id)
                }
                ClickResult.Consume
            },
        )
        SymbolLayer(
            id = "zone-counts",
            source = zoneSource,
            textField = feature["label"].convertToString(),
            textFont = const(listOf("Noto Sans Bold")),
            textSize = const(18.sp),
            textColor = const(labelColor),
        )
        CircleLayer(
            id = "user-pulse",
            source = userSource,
            radius = const((20f + 22f * pulse).dp),
            color = const(zoneColor),
            opacity = const((if (dark) 0.5f else 0.32f) * (1f - pulse)),
        )
        CircleLayer(
            id = "user-dot",
            source = userSource,
            radius = const(if (dark) 11.dp else 8.dp), // bigger on the dark map, so it stays clearly visible
            color = const(zoneColor),
            strokeColor = const(outline),
            strokeWidth = const(3.dp),
        )
    }
    // remember where the camera is, for when the map is rebuilt in the other theme
    LaunchedEffect(Unit) { snapshotFlow { state.cameraPosition }.collect { camera[0] = it } }

    // Another city was chosen: fly there (not on the first frame, the map starts there already).
    var shownCity by remember { mutableStateOf(city.id) }
    LaunchedEffect(city.id) {
        if (shownCity != city.id) {
            shownCity = city.id
            focusedZoneId = null
            state.animateCamera(
                update = CameraUpdate(center = city.overview(), zoom = city.zoom),
                animation = CameraAnimation.Fly { duration = 1.5.seconds },
            )
        }
    }

    // Centre on me: fly to the user's position when the counter is bumped.
    LaunchedEffect(centreOnUser) {
        val me = userLocation
        if (centreOnUser > 0 && me != null) {
            state.animateCamera(
                update = CameraUpdate(center = Position(longitude = me.lng, latitude = me.lat), zoom = USER_ZOOM),
                animation = CameraAnimation.Fly { duration = 1.seconds },
            )
        }
    }
    // The map data licences (OpenStreetMap, OpenFreeMap) require a visible credit, so it has to stay.
    val creditOverlay: @Composable MapOverlayScope.() -> Unit = {
        // small, faint text: still readable, but it no longer competes with the map
        val expanded = AttributionDefaults.expandedStyle()
        ExpandingAttributionButton(
            Modifier.align(Alignment.BottomEnd).padding(4.dp),
            Alignment.BottomEnd,
            expandedStyle = expanded.copy(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                textStyle = expanded.textStyle.copy(fontSize = 9.sp),
            ),
        )
    }
    // While the style loads the map shows the theme's background, so there is no white flash in dark mode
    val loadColor = MaterialTheme.colorScheme.background
    val mapUi = remember(loadColor) { MapUiOptions(MapUiOptions.Standard) { this.loadColor = loadColor } }
    if (onMapClick == null) {
        MaplibreMap(state = state, modifier = modifier, uiOptions = mapUi, overlay = creditOverlay)
    } else {
        // Picking a spot: a tap on the map reports where it was
        val clicked by rememberUpdatedState(onMapClick)
        MaplibreMap(
            state = state,
            modifier = modifier,
            uiOptions = mapUi,
            overlay = creditOverlay,
            interactions = MapInteractions {
                callbacks {
                    click {
                        onEvent { event ->
                            event.position?.let { clicked(it.latitude, it.longitude) }
                            ClickResult.Consume
                        }
                    }
                }
            },
        )
    }
}

private fun Need.isOpen() = status == NeedStatus.OPEN || status == NeedStatus.FUNDING
