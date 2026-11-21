package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The donor's three main tabs. */
enum class DonorTab(val label: StringResource) {
    CONTRIBUTIONS(Res.string.tab_contributions),
    MAP(Res.string.tab_map),
    ACHIEVEMENTS(Res.string.tab_achievements),
}

/** The donor tabs, at the bottom of the screen. */
@Composable
fun DonorBottomBar(selected: DonorTab, onSelect: (DonorTab) -> Unit) {
    // AppShell already pads for system bars, so no extra insets here
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, windowInsets = WindowInsets(0)) {
        DonorTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { TabIcon(tab) },
                label = {
                    Text(
                        stringResource(tab.label),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                // selected: primary icon + label on a soft primary pill (12 %); unselected: muted
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.app.navUnselected,
                    unselectedTextColor = MaterialTheme.app.navUnselected,
                ),
            )
        }
    }
}

/** Small line icons drawn on a Canvas, so no icon library is needed. Uses the current content colour. */
@Composable
private fun TabIcon(tab: DonorTab) {
    val color = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val w = 2.dp.toPx()
        when (tab) {
            DonorTab.CONTRIBUTIONS -> drawList(color, w)
            DonorTab.MAP -> drawPin(color, w)
            DonorTab.ACHIEVEMENTS -> drawMedal(color, w)
        }
    }
}

// a list: three bullets with lines
private fun DrawScope.drawList(color: Color, w: Float) {
    for (i in 0..2) {
        val y = size.height * (0.25f + i * 0.25f)
        drawCircle(color, w * 0.9f, Offset(size.width * 0.15f, y))
        drawLine(color, Offset(size.width * 0.32f, y), Offset(size.width * 0.9f, y), w, StrokeCap.Round)
    }
}

// a map pin: rounded top, point at the bottom, hole in the middle
private fun DrawScope.drawPin(color: Color, w: Float) {
    val cx = size.width / 2
    val r = size.width * 0.3f
    val cy = size.height * 0.38f
    val pin = Path().apply {
        moveTo(cx, size.height * 0.92f)
        lineTo(cx - r * 0.93f, cy + r * 0.38f)
        arcTo(androidx.compose.ui.geometry.Rect(Offset(cx - r, cy - r), Size(2 * r, 2 * r)), 157.5f, 225f, false)
        close()
    }
    drawPath(pin, color, style = Stroke(w))
    drawCircle(color, r * 0.32f, Offset(cx, cy))
}

// a medal: ribbon and a round medal
private fun DrawScope.drawMedal(color: Color, w: Float) {
    val cx = size.width / 2
    drawLine(color, Offset(size.width * 0.3f, size.height * 0.08f), Offset(cx, size.height * 0.42f), w, StrokeCap.Round)
    drawLine(color, Offset(size.width * 0.7f, size.height * 0.08f), Offset(cx, size.height * 0.42f), w, StrokeCap.Round)
    drawCircle(color, size.width * 0.24f, Offset(cx, size.height * 0.66f), style = Stroke(w))
    drawCircle(color, size.width * 0.08f, Offset(cx, size.height * 0.66f))
}
