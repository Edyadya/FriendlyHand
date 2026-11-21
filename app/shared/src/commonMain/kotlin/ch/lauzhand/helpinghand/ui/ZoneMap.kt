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
import androidx.compose.runtime.Composable
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
) {
    val land = MaterialTheme.colorScheme.surfaceVariant
    val road = MaterialTheme.colorScheme.surface
    val park = MaterialTheme.colorScheme.primaryContainer
    val lake = MaterialTheme.colorScheme.tertiaryContainer
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
        repo.zones.forEach { z ->
            val open = repo.people.filter { it.zoneId == z.id }
                .sumOf { p -> repo.needs.count { it.personId == p.id && it.isOpenForHelp } }
            val selected = selectedZoneId == z.id
            Column(
                Modifier
                    .width(slot)
                    .offset(x = w * z.x - slot / 2, y = h * z.y - bubble / 2),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Nothing open here = dead end: grey, small, no number, not tappable.
                val hasPeople = open > 0
                val size = if (hasPeople) bubble else 22.dp
                Surface(
                    onClick = { onZoneClick(if (selected) null else z.id) },
                    enabled = hasPeople,
                    shape = CircleShape,
                    color = when {
                        !hasPeople -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                        selected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.tertiary   // warm, saturated
                    },
                    contentColor = when {
                        selected -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onTertiary
                    },
                    shadowElevation = if (hasPeople) 6.dp else 0.dp,
                    modifier = Modifier
                        .size(size)
                        .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
                ) {
                    if (hasPeople) Box(contentAlignment = Alignment.Center) {
                        Text(open.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    z.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasPeople) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
        }
    }
}
