package ch.lauzhand.helpinghand.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.lauzhand.helpinghand.model.*
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
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.geojson.dsl.addFeature
import org.maplibre.spatialk.geojson.dsl.buildFeatureCollection

// Shared by the Android and web-JS targets (see app/shared/build.gradle.kts).

// Free OpenFreeMap styles, no API key. "bright" is the friendliest light one; "dark" for dark mode.
private const val STYLE_LIGHT = "https://tiles.openfreemap.org/styles/bright"
private const val STYLE_DARK = "https://tiles.openfreemap.org/styles/dark"

private val OVERVIEW_CENTER = Position(longitude = 6.632, latitude = 46.519)
private const val OVERVIEW_ZOOM = 12.6
private const val ZONE_ZOOM = 14.8

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
) {
    val onZone by rememberUpdatedState(onZoneClick)
    val zonesNow by rememberUpdatedState(zones)
    val scope = rememberCoroutineScope()
    var focusedZoneId by remember { mutableStateOf<String?>(null) }
    val onPerson by rememberUpdatedState(onPersonClick)

    // Only zones with open needs get a bubble (warm, tappable, with the count). Zones with nothing open
    // are dead ends, so they are not drawn at all.
    val openCounts = remember(zones, people, needs) {
        zones.associate { z ->
            z.id to people.filter { it.zoneId == z.id }.sumOf { p -> needs.count { it.personId == p.id && it.isOpen() } }
        }
    }
    val zoneData = remember(zones, openCounts) {
        buildFeatureCollection<Point, JsonObject?> {
            for (z in zones) {
                val open = openCounts[z.id] ?: 0
                if (open > 0) addFeature(
                    geometry = Point(z.position()),
                    properties = buildJsonObject { put("id", z.id); put("label", open.toString()) },
                )
            }
        }
    }
    // Exact last-seen markers: only for volunteers, and only recent ones.
    val seesExact = role == Role.VOLUNTEER
    val now = remember(people) { nowMillis() }
    val seenData = remember(people, seesExact, now) {
        buildFeatureCollection<Point, JsonObject?> {
            if (seesExact) {
                for (p in people) {
                    if (!p.lastSeenVisible(now)) continue
                    addFeature(
                        geometry = Point(Position(longitude = p.lastSeenLng!!, latitude = p.lastSeenLat!!)),
                        properties = buildJsonObject { put("id", p.id) },
                    )
                }
            }
        }
    }

    val zoneColor = MaterialTheme.colorScheme.tertiary          // warm, saturated: people are waiting
    val seenColor = MaterialTheme.colorScheme.secondary
    val outline = MaterialTheme.colorScheme.surface
    val labelColor = MaterialTheme.colorScheme.onTertiary
    val styleUrl = if (isSystemInDarkTheme()) STYLE_DARK else STYLE_LIGHT

    val state = rememberMapState(
        baseStyle = BaseStyle.Uri(styleUrl),
        initialCameraPosition = CameraPosition(center = OVERVIEW_CENTER, zoom = OVERVIEW_ZOOM),
    ) {
        val mapState = LocalMapState.current
        val zoneSource = rememberGeoJsonSource(GeoJsonData.Features(zoneData))
        val seenSource = rememberGeoJsonSource(GeoJsonData.Features(seenData))

        CircleLayer(
            id = "zones",
            source = zoneSource,
            radius = const(26.dp),
            color = const(zoneColor),
            opacity = const(0.92f),
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
                            update = if (zoomingOut) CameraUpdate(center = OVERVIEW_CENTER, zoom = OVERVIEW_ZOOM)
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
            textFont = const(listOf("Noto Sans Regular")),
            textSize = const(18.sp),
            textColor = const(labelColor),
        )
        CircleLayer(
            id = "last-seen",
            source = seenSource,
            radius = const(9.dp),
            color = const(seenColor),
            strokeColor = const(outline),
            strokeWidth = const(3.dp),
            hitPadding = 10.dp,
            onClick = { features ->
                features.firstOrNull()?.properties.id()?.let { onPerson(it) }
                ClickResult.Consume
            },
        )
    }
    MaplibreMap(state = state, modifier = modifier)
}

private fun Need.isOpen() = status == NeedStatus.OPEN || status == NeedStatus.FUNDING
