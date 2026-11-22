package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import ch.lauzhand.helpinghand.location.UserLocation
import ch.lauzhand.helpinghand.model.Cities
import ch.lauzhand.helpinghand.model.City
import ch.lauzhand.helpinghand.map.fractionX
import ch.lauzhand.helpinghand.map.fractionY
import ch.lauzhand.helpinghand.map.zoneBubbleCount
import ch.lauzhand.helpinghand.model.Role
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.lauzhand.helpinghand.data.HelpRepository
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Playful, soft map in the spirit of friend-finder apps: rounded land, white roads,
 * green patches, the lake as a band at the bottom and round bubbles per zone.
 * Zones are areas only (never exact spots). Tapping a bubble filters the people list.
 */
@Composable
fun ZoneMap(
    repo: HelpRepository,
    selectedZoneId: String?,
    onZoneClick: (String?) -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.DONOR,
    userLocation: UserLocation? = null,
    city: City = Cities.LAUSANNE,
    showBubbles: Boolean = true,
) {
    val land = MaterialTheme.colorScheme.primaryContainer
    val road = MaterialTheme.colorScheme.surface
    val park = MaterialTheme.app.success.copy(alpha = 0.18f)
    val lake = MaterialTheme.app.accent.copy(alpha = 0.28f)
    val lakeText = MaterialTheme.colorScheme.onTertiaryContainer

    BoxWithConstraints(modifier.clip(RoundedCornerShape(28.dp)).background(land)) {
        val w = maxWidth
        val h = maxHeight

        Canvas(Modifier.matchParentSize()) {
            val lakeTop = size.height * 0.84f
            // green patches
            drawCircle(park.copy(alpha = 0.7f), size.minDimension * 0.18f, Offset(size.width * 0.82f, size.height * 0.52f))
            drawCircle(park.copy(alpha = 0.7f), size.minDimension * 0.12f, Offset(size.width * 0.22f, size.height * 0.16f))
            drawCircle(park.copy(alpha = 0.5f), size.minDimension * 0.10f, Offset(size.width * 0.12f, size.height * 0.72f))
            // roads
            val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            fun roadPath(vararg p: Pair<Float, Float>) {
                val path = Path().apply {
                    moveTo(size.width * p[0].first, size.height * p[0].second)
                    for (i in 1 until p.size) lineTo(size.width * p[i].first, size.height * p[i].second)
                }
                drawPath(path, road, style = stroke)
            }
            roadPath(0.02f to 0.28f, 0.35f to 0.36f, 0.62f to 0.30f, 0.98f to 0.40f)
            roadPath(0.45f to 0.02f, 0.50f to 0.45f, 0.55f to 0.80f)
            roadPath(0.02f to 0.62f, 0.30f to 0.56f, 0.70f to 0.66f, 0.98f to 0.60f)
            // lake with a wavy shore
            val shore = Path().apply {
                moveTo(0f, lakeTop)
                val steps = 6
                val dx = size.width / steps
                for (i in 0 until steps) {
                    val x0 = dx * i
                    quadraticTo(x0 + dx / 2, lakeTop + if (i % 2 == 0) -14.dp.toPx() else 14.dp.toPx(), x0 + dx, lakeTop)
                }
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(shore, lake)
        }

        Text(
            stringResource(Res.string.map_lake),
            Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
            color = lakeText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )

        val bubble = 40.dp
        val slot = 76.dp
        repo.zones.filter { showBubbles && it.cityId == city.id }.forEach { z ->
            // No bubble where there is nothing to do for this role (see zoneBubbleCount).
            val open = zoneBubbleCount(role, z.id, repo.people, repo.needs) ?: return@forEach
            val selected = selectedZoneId == z.id
            Column(
                Modifier
                    .width(slot)
                    .offset(x = w * z.x - slot / 2, y = h * z.y - bubble / 2),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(bubble), contentAlignment = Alignment.Center) {
                    // selected: concentric ring in accent (30 %) that expands outwards and fades
                    if (selected) PulseRing(MaterialTheme.app.accent, bubble)
                    Surface(
                        onClick = { onZoneClick(if (selected) null else z.id) },
                        shape = CircleShape,
                        color = MaterialTheme.app.accent,          // solid accent bubble
                        contentColor = MaterialTheme.app.onNavy,   // white, bold number (large text on the accent blue)
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .size(bubble)
                            .border(3.dp, MaterialTheme.app.onNavy, CircleShape), // white ring
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(open.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    z.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
        }

        // The user's own position: a blue dot with a white ring and a slowly pulsing ring.
        // only when the position is actually inside this city's map
        userLocation?.takeIf { fractionX(it.lng, city) in 0f..1f && fractionY(it.lat, city) in 0f..1f }?.let { me ->
            val dot = 16.dp
            val box = 56.dp
            val fx = fractionX(me.lng, city)
            val fy = fractionY(me.lat, city)
            Box(
                Modifier.offset(x = w * fx - box / 2, y = h * fy - box / 2).size(box),
                contentAlignment = Alignment.Center,
            ) {
                PulseRing(MaterialTheme.app.accent, dot)
                Box(
                    Modifier.size(dot).clip(CircleShape).background(MaterialTheme.app.accent)
                        .border(3.dp, MaterialTheme.app.onNavy, CircleShape),
                )
            }
        }
    }
}


/** Concentric ring that expands from the bubble and fades out, repeating. */
@Composable
private fun PulseRing(color: Color, bubble: Dp) {
    val t by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
    )
    Canvas(Modifier.requiredSize(bubble + 64.dp)) {
        val start = bubble.toPx() / 2
        val end = size.minDimension / 2
        drawCircle(color.copy(alpha = 0.30f * (1f - t)), radius = start + (end - start) * t)
    }
}
