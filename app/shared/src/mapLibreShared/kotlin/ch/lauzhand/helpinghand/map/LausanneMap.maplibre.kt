package ch.lauzhand.helpinghand.map

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
import org.maplibre.compose.camera.CameraPosition
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

// Free OpenFreeMap style, no API key. "bright" is the friendliest of the free ones.
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/bright"

// Zone.x / Zone.y (0..1) are laid out on this box around Lausanne. Zones are areas, never exact spots.
private const val WEST = 6.595
private const val LON_SPAN = 0.070
private const val NORTH = 46.535
private const val LAT_SPAN = 0.032

private fun Zone.position() = Position(longitude = WEST + x * LON_SPAN, latitude = NORTH - y * LAT_SPAN)

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
    val onPerson by rememberUpdatedState(onPersonClick)

    // One circle per zone with its open-needs count.
    val zoneData = remember(zones, people, needs) {
        buildFeatureCollection<Point, JsonObject?> {
            for (z in zones) {
                val open = people.filter { it.zoneId == z.id }
                    .sumOf { p -> needs.count { it.personId == p.id && it.isOpen() } }
                addFeature(
                    geometry = Point(z.position()),
                    properties = buildJsonObject { put("id", z.id); put("label", open.toString()) },
                )
            }
        }
    }

    // Exact last-seen markers: only for volunteers / NGOs, and only recent ones.
    val seesExact = role == Role.VOLUNTEER || role == Role.NGO
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

    val zoneColor = MaterialTheme.colorScheme.primary
    val seenColor = MaterialTheme.colorScheme.secondary
    val outline = Color.White

    val state = rememberMapState(
        baseStyle = BaseStyle.Uri(STYLE_URL),
        initialCameraPosition = CameraPosition(center = Position(longitude = 6.632, latitude = 46.519), zoom = 12.6),
    ) {
        val zoneSource = rememberGeoJsonSource(GeoJsonData.Features(zoneData))
        val seenSource = rememberGeoJsonSource(GeoJsonData.Features(seenData))

        CircleLayer(
            id = "zones",
            source = zoneSource,
            radius = const(26.dp),
            color = const(zoneColor),
            opacity = const(0.45f),
            strokeColor = const(outline),
            strokeWidth = const(3.dp),
            hitPadding = 8.dp,
            onClick = { features ->
                features.firstOrNull()?.properties.id()?.let { onZone(it) }
                ClickResult.Consume
            },
        )
        SymbolLayer(
            id = "zone-counts",
            source = zoneSource,
            textField = feature["label"].convertToString(),
            textFont = const(listOf("Noto Sans Regular")),
            textSize = const(18.sp),
            textColor = const(Color.Black),
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
